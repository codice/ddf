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
package org.codice.ddf.commands.catalog;

import com.google.common.io.ByteSource;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.common.audit.SecurityLogger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.codice.ddf.commands.util.DigitalSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports Metacards, History, and their content from a zip file into the catalog. <b> This code is
 * experimental. While this interface is functional and tested, it may change or be removed in a
 * future version of the library. </b>
 */
@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "import",
  description = "Imports Metacards and history into the current Catalog"
)
public class ImportCommand extends CatalogCommands {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportCommand.class);

  private static final int ID = 2;

  private static final int TYPE = 3;

  private static final int NAME = 4;

  private static final int DERIVED_NAME = 5;

  @Reference private List<AttributeInjector> attributeInjectors;

  @Reference private StorageProvider storageProvider;

  private DigitalSignature verifier = new DigitalSignature();

  @Argument(
    name = "Import File",
    description = "The file to import",
    index = 0,
    multiValued = false,
    required = true
  )
  String importFile;

  @Option(
    name = "--skip-signature-verification",
    required = false,
    multiValued = false,
    description =
        "Exports the data but does NOT sign the resulting zip file. "
            + "WARNING: This file will not be able to be verified on import for integrity and authenticity."
  )
  boolean unsafe = false;

  @Option(
    name = "--force",
    required = false,
    aliases = {"-f"},
    multiValued = false,
    description = "Do not prompt"
  )
  boolean force = false;

  @Option(
    name = "--signature",
    required = false,
    aliases = {"-s"},
    multiValued = false,
    description =
        "Provide an absolute path for the digital signature to verify the integrity of the exported data. Required unless you use --skip-signature-verification."
  )
  String signatureFile;

  @Override
  protected final Object executeWithSubject() throws Exception {
    int metacards = 0;
    int content = 0;
    int derivedContent = 0;
    File file = initImportFile(importFile);
    InputTransformer transformer =
        getServiceByFilter(
                InputTransformer.class, String.format("(%s=%s)", "id", DEFAULT_TRANSFORMER_ID))
            .orElseThrow(
                () ->
                    new CatalogCommandRuntimeException(
                        "Could not get " + DEFAULT_TRANSFORMER_ID + " input transformer"));

    if (unsafe) {
      if (!force) {
        String input =
            session.readLine(
                "This will import data with no check to see if data is modified/corrupt. Do you wish to continue? (y/N) ",
                null);
        if (!input.matches("^[yY][eE]?[sS]?$")) {
          console.println("ABORTED IMPORT.");
          return null;
        }
      }
      SecurityLogger.audit(
          "Skipping validation check of imported data. There are no "
              + "guarantees of integrity or authenticity of the imported data."
              + "File being imported: {}",
          importFile);
    } else {
      if (StringUtils.isBlank(signatureFile)) {
        String message = "A signature file must be provided with import data";
        console.println(message);
        throw new CatalogCommandRuntimeException(message);
      }

      String alias =
          AccessController.doPrivileged(
              (PrivilegedAction<String>)
                  () -> System.getProperty("org.codice.ddf.system.hostname"));

      try (FileInputStream fileIs = new FileInputStream(file);
          FileInputStream sigFileIs = new FileInputStream(signatureFile)) {
        if (!verifier.verifyDigitalSignature(fileIs, sigFileIs, alias)) {
          throw new CatalogCommandRuntimeException("The provided data could not be verified");
        }
      }
    }
    SecurityLogger.audit("Called catalog:import command on the file: {}", importFile);
    console.println("Importing file");
    Instant start = Instant.now();
    try (InputStream fis = new FileInputStream(file);
        ZipInputStream zipInputStream = new ZipInputStream(fis)) {
      ZipEntry entry = zipInputStream.getNextEntry();

      while (entry != null) {
        String filename = entry.getName();

        if (filename.startsWith("META-INF")) {
          entry = zipInputStream.getNextEntry();
          continue;
        }

        String[] pathParts = filename.split("\\" + File.separator);
        if (pathParts.length < 5) {
          console.println("Entry is not valid! " + filename);
          entry = zipInputStream.getNextEntry();
          continue;
        }
        String id = pathParts[ID];
        String type = pathParts[TYPE];

        switch (type) {
          case "metacard":
            {
              String metacardName = pathParts[NAME];
              Metacard metacard = null;
              try {
                metacard =
                    transformer.transform(
                        new UncloseableBufferedInputStreamWrapper(zipInputStream), id);
              } catch (IOException | CatalogTransformerException e) {
                LOGGER.debug("Could not transform metacard: {}", id);
                entry = zipInputStream.getNextEntry();
                continue;
              }
              metacard = applyInjectors(metacard, attributeInjectors);
              catalogProvider.create(new CreateRequestImpl(metacard));
              metacards++;
              break;
            }
          case "content":
            {
              content++;
              String contentFilename = pathParts[NAME];
              ContentItem contentItem =
                  new ContentItemImpl(
                      id,
                      new ZipEntryByteSource(
                          new UncloseableBufferedInputStreamWrapper(zipInputStream)),
                      null,
                      contentFilename,
                      entry.getSize(),
                      null);
              CreateStorageRequestImpl createStorageRequest =
                  new CreateStorageRequestImpl(
                      Collections.singletonList(contentItem), id, new HashMap<>());
              storageProvider.create(createStorageRequest);
              storageProvider.commit(createStorageRequest);
              break;
            }
          case "derived":
            {
              derivedContent++;
              String qualifier = pathParts[NAME];
              String derivedContentName = pathParts[DERIVED_NAME];
              ContentItem contentItem =
                  new ContentItemImpl(
                      id,
                      qualifier,
                      new ZipEntryByteSource(
                          new UncloseableBufferedInputStreamWrapper(zipInputStream)),
                      null,
                      derivedContentName,
                      entry.getSize(),
                      null);
              CreateStorageRequestImpl createStorageRequest =
                  new CreateStorageRequestImpl(
                      Collections.singletonList(contentItem), id, new HashMap<>());
              storageProvider.create(createStorageRequest);
              storageProvider.commit(createStorageRequest);
              break;
            }
          default:
            {
              LOGGER.debug("Cannot interpret type of {}", type);
            }
        }

        entry = zipInputStream.getNextEntry();
      }
    } catch (Exception e) {
      printErrorMessage(
          String.format(
              "Exception while importing metacards (%s)%nFor more information set the log level to INFO (log:set INFO org.codice.ddf.commands.catalog) ",
              e.getMessage()));
      LOGGER.info("Exception while importing metacards", e);
      throw e;
    }
    console.println("File imported successfully. Imported in: " + getFormattedDuration(start));
    console.println("Number of metacards imported: " + metacards);
    console.println("Number of content imported: " + content);
    console.println("Number of derived content imported: " + derivedContent);
    return null;
  }

  private File initImportFile(String importFile) {
    File file = new File(importFile);

    if (!file.exists()) {
      throw new CatalogCommandRuntimeException("File does not exist: " + importFile);
    }

    if (!FilenameUtils.isExtension(importFile, "zip")) {
      throw new CatalogCommandRuntimeException("File must be a zip file: " + importFile);
    }

    return file;
  }

  private Metacard applyInjectors(Metacard original, List<AttributeInjector> injectors) {
    Metacard metacard = original;
    for (AttributeInjector injector : injectors) {
      metacard = injector.injectAttributes(metacard);
    }
    return metacard;
  }

  private static class ZipEntryByteSource extends ByteSource {
    private InputStream input;

    private ZipEntryByteSource(InputStream input) {
      this.input = input;
    }

    @Override
    public InputStream openStream() throws IOException {
      return input;
    }
  }

  /**
   * Identical to BufferedInputStream with the exception that it does not close the underlying
   * resource stream when the close() method is called. The buffer is still emptied when closed.
   * <br>
   * This is useful for cases when the inputstream being consumed belongs to something that should
   * not be closed.
   */
  private static class UncloseableBufferedInputStreamWrapper extends BufferedInputStream {
    private static final AtomicReferenceFieldUpdater<BufferedInputStream, byte[]> BUF_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(BufferedInputStream.class, byte[].class, "buf");

    public UncloseableBufferedInputStreamWrapper(InputStream in) {
      super(in);
    }

    @Override
    public void close() throws IOException {
      byte[] buffer;
      while ((buffer = buf) != null) {
        if (BUF_UPDATER.compareAndSet(this, buffer, null)) {
          in = null;
          // Purposely do not close `in`
          return;
        }
        // Else retry in case a new buf was CASed in fill()
      }
    }
  }
}
