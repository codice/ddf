/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.transform.impl;

import com.google.common.io.Files;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.activation.MimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.codice.ddf.catalog.locator.TransformerLocator;
import org.codice.ddf.catalog.transform.MultiInputTransformer;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformImpl implements Transform {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformImpl.class);

  private TransformerLocator transformerLocator;

  private UuidGenerator uuidGenerator;

  public TransformImpl(TransformerLocator transformerLocator, UuidGenerator uuidGenerator) {
    this.transformerLocator = transformerLocator;
    this.uuidGenerator = uuidGenerator;
  }

  @Override
  public TransformResponse transform(
      MimeType mimeType,
      String parentId,
      Supplier<String> idSupplier,
      String fileName,
      File inputFile,
      String transformerId,
      Map<String, ? extends Serializable> transformerArguments)
      throws MetacardCreationException {
    try (InputStream transformerStream = Files.asByteSource(inputFile).openStream()) {
      TransformResponse transformResponse =
          transform(
              mimeType,
              parentId,
              idSupplier,
              transformerStream,
              transformerId,
              transformerArguments);
      transformResponse.getParentMetacard().ifPresent(metacard -> setTitle(fileName, metacard));
      transformResponse
          .getDerivedContentItems()
          .forEach(contentItem -> setTitle(contentItem.getFilename(), contentItem.getMetacard()));
      // TODO Should we set the title of derived metacards? We could do something like
      // 'fileName-INDEX'. The problem is derived metacards don't have their own filename.
      return transformResponse;
    } catch (IOException e) {
      throw new MetacardCreationException(
          String.format("Could not open the input file: %s", inputFile), e);
    }
  }

  @Override
  public TransformResponse transform(
      MimeType mimeType,
      String parentId,
      Supplier<String> idSupplier,
      InputStream message,
      String transformerId,
      Map<String, ? extends Serializable> transformerArguments)
      throws MetacardCreationException {

    List<MultiInputTransformer> listOfCandidates =
        StringUtils.isNotEmpty(transformerId)
            ? transformerLocator.findMultiInputTransformers(transformerId)
            : transformerLocator.findMultiInputTransformers(mimeType);

    LOGGER.trace(
        "List of matches for mimeType [{}] and transformerId [{}]: {}",
        mimeType,
        transformerId,
        listOfCandidates);

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {

      copy(message, fileBackedOutputStream);

      List<String> stackTraceList = new ArrayList<>();

      TransformResponse transformResponse = null;

      for (MultiInputTransformer transformer : listOfCandidates) {
        try (InputStream inputStreamMessageCopy =
            fileBackedOutputStream.asByteSource().openStream()) {
          transformResponse = transformer.transform(inputStreamMessageCopy, transformerArguments);
        } catch (CatalogTransformerException | IOException e) {
          List<String> stackTraces = Arrays.asList(ExceptionUtils.getRootCauseStackTrace(e));
          stackTraceList.add(
              String.format("Transformer [%s] could not create metacard.", transformer));
          stackTraceList.addAll(stackTraces);
          LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
        }
        if (isNotEmpty(transformResponse)) {
          break;
        }
      }

      if (isEmpty(transformResponse)) {
        throw new MetacardCreationException(
            String.format(
                "Could not create metacard with mimeType %s : %s",
                mimeType, StringUtils.join(stackTraceList, "\n")));
      }

      return setMetacardIds(parentId, idSupplier, transformResponse);
    } catch (IOException e) {
      throw new MetacardCreationException("Could not create metacard.", e);
    }
  }

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards,
      MimeType mimeType,
      String transformerId,
      Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException {
    if (CollectionUtils.isEmpty(metacards)) {
      throw new IllegalArgumentException("Metacards list is empty.");
    }

    List<MultiMetacardTransformer> multiMetacardTransformers =
        StringUtils.isNotEmpty(transformerId)
            ? transformerLocator.findMultiMetacardTransformers(transformerId)
            : transformerLocator.findMultiMetacardTransformers(mimeType);

    LOGGER.trace(
        "List of matches for mimeType [{}] and transformerId [{}]: {}",
        mimeType,
        transformerId,
        multiMetacardTransformers);

    if (CollectionUtils.isEmpty(multiMetacardTransformers)) {
      throw new IllegalArgumentException("Transformer " + transformerId + " not found");
    }

    List<String> stackTraceList = new ArrayList<>();
    List<BinaryContent> binaryContents = null;

    for (MultiMetacardTransformer transformer : multiMetacardTransformers) {
      binaryContents =
          attemptTransform(metacards, transformerArguments, stackTraceList, transformer);

      if (CollectionUtils.isNotEmpty(binaryContents)) {
        break;
      }
    }

    if (CollectionUtils.isEmpty(binaryContents)) {
      throw new CatalogTransformerException(
          String.format(
              "Could not transform metacards: transformerId=%s : %s",
              transformerId, StringUtils.join(stackTraceList, "\n")));
    }

    return binaryContents;
  }

  private List<BinaryContent> attemptTransform(
      List<Metacard> metacards,
      Map<String, Serializable> transformerArguments,
      List<String> stackTraceList,
      MultiMetacardTransformer transformer) {
    try {
      return transformer.transform(metacards, transformerArguments);
    } catch (CatalogTransformerException e) {
      List<String> stackTraces = Arrays.asList(ExceptionUtils.getRootCauseStackTrace(e));
      stackTraceList.add(
          String.format("Transformer [%s] could not transform metacard.", transformer));
      stackTraceList.addAll(stackTraces);
      LOGGER.debug("Transformer [{}] could not transform metacard.", transformer, e);
    }
    return null;
  }

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards,
      String transformerId,
      Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException {
    if (CollectionUtils.isEmpty(metacards)) {
      throw new IllegalArgumentException("Metacards list is empty.");
    }

    if (StringUtils.isEmpty(transformerId)) {
      throw new IllegalArgumentException("transformerId is empty or null.");
    }

    List<MultiMetacardTransformer> multiMetacardTransformers =
        transformerLocator.findMultiMetacardTransformers(transformerId);

    LOGGER.trace(
        "List of matches for transformerId [{}]: {}", transformerId, multiMetacardTransformers);

    if (CollectionUtils.isEmpty(multiMetacardTransformers)) {
      throw new IllegalArgumentException("Transformer " + transformerId + " not found");
    }

    List<String> stackTraceList = new ArrayList<>();
    List<BinaryContent> binaryContents = null;

    for (MultiMetacardTransformer transformer : multiMetacardTransformers) {
      binaryContents =
          attemptTransform(metacards, transformerArguments, stackTraceList, transformer);

      if (binaryContents != null) {
        break;
      }
    }

    if (CollectionUtils.isEmpty(binaryContents)) {
      throw new CatalogTransformerException(
          String.format(
              "Could not transform metacards: transformerId=%s : %s",
              transformerId, StringUtils.join(stackTraceList, "\n")));
    }

    return binaryContents;
  }

  @Override
  public boolean isMetacardTransformerIdValid(String transformerId) {
    return CollectionUtils.isNotEmpty(
        transformerLocator.findMultiMetacardTransformers(transformerId));
  }

  @Override
  public BinaryContent transform(
      SourceResponse response, String transformerId, Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException {
    return transformSourceResponse(
        response,
        transformerArguments,
        "transformerId",
        transformerLocator::findQueryResponseTransformers,
        transformerId);
  }

  @Override
  public BinaryContent transform(
      SourceResponse response, MimeType mimeType, Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException {
    return transformSourceResponse(
        response,
        transformerArguments,
        "mime-type",
        transformerLocator::findQueryResponseTransformers,
        mimeType);
  }

  private <T> BinaryContent transformSourceResponse(
      SourceResponse response,
      Map<String, Serializable> transformerArguments,
      String identString,
      Function<T, List<QueryResponseTransformer>> func,
      T id)
      throws CatalogTransformerException {
    if (response == null) {
      throw new IllegalArgumentException("SourceResponse is null.");
    }

    List<QueryResponseTransformer> transformers = func.apply(id);

    if (CollectionUtils.isEmpty(transformers)) {
      throw new IllegalArgumentException(
          "Transformers matching " + identString + " " + id + " not found");
    }

    List<String> stackTraceList = new LinkedList<>();

    BinaryContent binaryContent =
        transformSourceResponse(response, transformerArguments, transformers, stackTraceList::add);

    if (binaryContent == null) {
      throw new CatalogTransformerException(
          String.format(
              "Could not transform source response: %s=%s : %s",
              identString, id, StringUtils.join(stackTraceList, "\n")));
    }

    return binaryContent;
  }

  private String calculateId(Supplier<String> idSupplier) {
    if (idSupplier != null) {
      String id = idSupplier.get();
      return id != null ? id : uuidGenerator.generateUuid();
    }
    return uuidGenerator.generateUuid();
  }

  private String calculateId(String id) {
    return id != null ? id : uuidGenerator.generateUuid();
  }

  private BinaryContent transformSourceResponse(
      SourceResponse response,
      Map<String, Serializable> transformerArguments,
      List<QueryResponseTransformer> transformers,
      Consumer<String> stackTraceConsumer) {

    BinaryContent binaryContent = null;

    for (QueryResponseTransformer transformer : transformers) {
      binaryContent =
          attemptTransform(response, transformerArguments, stackTraceConsumer, transformer);

      if (binaryContent != null) {
        break;
      }
    }

    return binaryContent;
  }

  private BinaryContent attemptTransform(
      SourceResponse response,
      Map<String, Serializable> transformerArguments,
      Consumer<String> stackTraceConsumer,
      QueryResponseTransformer transformer) {
    try {
      return transformer.transform(response, transformerArguments);
    } catch (CatalogTransformerException e) {
      List<String> stackTraces = Arrays.asList(ExceptionUtils.getRootCauseStackTrace(e));
      stackTraceConsumer.accept(
          String.format("Transformer [%s] could not transform source response.", transformer));
      stackTraces.forEach(stackTraceConsumer);
      LOGGER.debug("Transformer [{}] could not transform source response.", transformer, e);
    }
    return null;
  }

  private void setMetacardId(Metacard metacard, String id) {
    metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
  }

  private void setMetacardId(Metacard metacard, Supplier<String> supplier) {
    setMetacardId(metacard, calculateId(supplier));
  }

  private TransformResponse setMetacardIds(
      String parentId, Supplier<String> idSupplier, TransformResponse transformResponse) {
    transformResponse
        .getParentMetacard()
        .ifPresent(metacard -> setMetacardId(metacard, calculateId(parentId)));
    transformResponse
        .getDerivedMetacards()
        .forEach(metacard -> setMetacardId(metacard, idSupplier));
    return transformResponse;
  }

  private void copy(InputStream message, TemporaryFileBackedOutputStream fileBackedOutputStream)
      throws MetacardCreationException {
    try {
      if (null != message) {
        IOUtils.copy(message, fileBackedOutputStream);
      } else {
        throw new MetacardCreationException(
            "Could not copy bytes of content message.  Message was NULL.");
      }
    } catch (IOException e) {
      throw new MetacardCreationException("Could not copy bytes of content message.", e);
    }
  }

  private void setTitle(String fileName, Metacard metacard) {
    if (metacard.getAttribute(Metacard.TITLE) == null) {
      metacard.setAttribute(new AttributeImpl(Metacard.TITLE, fileName));
    }
  }

  private boolean isEmpty(TransformResponse transformResponse) {
    if (transformResponse == null) {
      return true;
    }

    return !transformResponse.getParentMetacard().isPresent()
        && CollectionUtils.isEmpty(transformResponse.getDerivedMetacards())
        && CollectionUtils.isEmpty(transformResponse.getDerivedContentItems());
  }

  private boolean isNotEmpty(TransformResponse transformResponse) {
    return !isEmpty(transformResponse);
  }
}
