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
package ddf.catalog.federation.layered.replication;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.util.impl.Requests;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestReplicatorPlugin implements PostIngestPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestReplicatorPlugin.class);

  private static final String RESPONSE = "RESPONSE: [{}]";

  /** A configurable property of parent's location. */
  private PropertyResolver parentAddress = null;

  private MetacardTransformer transformer = null;

  private WebClient client;

  public RestReplicatorPlugin(String endpointAddress) {
    setParentAddress(endpointAddress);
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {

    if (Requests.isLocal(input.getRequest()) && client != null && transformer != null) {

      for (Metacard m : input.getCreatedMetacards()) {

        String data = transform(m, client);

        Response r = client.post(data);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Posted the following GeoJSON: {}\n", data);
          LOGGER.debug(RESPONSE, ToStringBuilder.reflectionToString(r));
        }
      }
    }

    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {

    if (Requests.isLocal(input.getRequest()) && client != null && transformer != null) {

      WebClient updateClient = WebClient.fromClient(client);

      updateClient.type(MediaType.APPLICATION_JSON);

      List<Update> updates = input.getUpdatedMetacards();

      if (updates == null) {
        return input;
      }

      UpdateRequest request = input.getRequest();
      if (request != null && !Core.ID.equals(request.getAttributeName())) {
        throw new PluginExecutionException(
            new UnsupportedOperationException(
                "Cannot replicate records that are not updated by " + Core.ID));
      }

      for (int i = 0; i < updates.size(); i++) {

        Update update = updates.get(i);

        if (request != null
            && request.getUpdates() != null
            && request.getUpdates().get(i) != null
            && request.getUpdates().get(i).getKey() != null) {

          updateClient.path(request.getUpdates().get(i).getKey());

          Metacard newMetacard = update.getNewMetacard();

          String newData = transform(newMetacard, updateClient);

          Response r = updateClient.put(newData);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RESPONSE, ToStringBuilder.reflectionToString(r));
          }
        }
      }
    }

    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {

    if (input != null && Requests.isLocal(input.getRequest()) && client != null) {

      WebClient updateClient = WebClient.fromClient(client);

      updateClient.type(MediaType.APPLICATION_JSON);

      if (input.getDeletedMetacards() == null || input.getDeletedMetacards().isEmpty()) {
        return input;
      }

      for (int i = 0; i < input.getDeletedMetacards().size(); i++) {

        Metacard metacard = input.getDeletedMetacards().get(i);

        if (metacard != null && metacard.getId() != null) {

          updateClient.path(metacard.getId());

          Response r = updateClient.delete();
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RESPONSE, ToStringBuilder.reflectionToString(r));
          }
        }
      }
    }

    return input;
  }

  public String getParentAddress() {
    return parentAddress.getResolvedString();
  }

  public void setParentAddress(String endpointAddress) {

    if (endpointAddress == null) {

      this.parentAddress = new PropertyResolver(null);

      client = null;

    } else if (this.parentAddress == null
        || !endpointAddress.equals(this.parentAddress.getResolvedString())) {

      PropertyResolver previous = this.parentAddress;

      this.parentAddress = new PropertyResolver(endpointAddress);

      client = WebClient.create(this.parentAddress.getResolvedString(), true);

      LOGGER.debug(
          "Changed the parent address property from [{}] to [{}]", previous, this.parentAddress);
    }
  }

  public MetacardTransformer getTransformer() {
    return transformer;
  }

  public void setTransformer(MetacardTransformer transformer) {
    this.transformer = transformer;
    LOGGER.debug("Changed transformer to [{}]", this.transformer);
  }

  private String transform(Metacard m, WebClient client) throws PluginExecutionException {

    BinaryContent binaryContent;
    try {
      binaryContent = transformer.transform(m, new HashMap<>());
      client.type(getValidMimeType(binaryContent.getMimeTypeValue()));
      return new String(binaryContent.getByteArray(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.debug("Could not understand metacard.", e);
      throw new PluginExecutionException("Could not send metacard.");
    } catch (CatalogTransformerException e) {
      LOGGER.debug("Could not transform metacard.", e);
      throw new PluginExecutionException("Could not send metacard.");
    }
  }

  private String getValidMimeType(String mimeTypeValue) {
    if (mimeTypeValue == null) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    return mimeTypeValue;
  }
}
