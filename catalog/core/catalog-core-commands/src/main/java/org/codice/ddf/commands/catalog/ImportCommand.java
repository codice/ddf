package org.codice.ddf.commands.catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.catalog.transformer.zip.ZipValidator;
import org.codice.ddf.commands.util.CatalogCommandRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;

import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

@Service
@Command(scope = CatalogCommands.NAMESPACE, name = "import", description = "Imports Metacards and history into the current Catalog")
public class ImportCommand extends CatalogCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCommand.class);

    private static final int ID = 2;

    private static final int TYPE = 3;

    private static final int NAME = 4;

    private static final int DERIVED_NAME = 5;

    @Reference
    private StorageProvider storageProvider;

    @Argument(name = "Import File", description = "The file to import", index = 0, multiValued = false, required = true)
    String importFile;

    @Override
    protected Object executeWithSubject() throws Exception {
        ZipValidator zipValidator = initZipValidator();
        File file = initImportFile(importFile);
        InputTransformer transformer = getServiceByFilter(InputTransformer.class,
                String.format("(%s=%s)",
                        "id",
                        DEFAULT_TRANSFORMER_ID)).orElseThrow(() -> new CatalogCommandRuntimeException(
                "Could not get " + DEFAULT_TRANSFORMER_ID + " input transformer"));

        if (!zipValidator.validateZipFile(importFile)) {
            throw new CatalogCommandRuntimeException("Signature on zip file is not valid");
        }

        try (InputStream fis = new FileInputStream(file);
                ZipInputStream zipInputStream = new ZipInputStream(fis)) {
            ZipEntry entry = zipInputStream.getNextEntry();

            while (entry != null) {
                String filename = entry.getName();

                if (filename.startsWith("META-INF")) {
                    entry = zipInputStream.getNextEntry();
                    continue;
                }

                String[] pathParts = filename.split("/");
                String id = pathParts[ID];
                String type = pathParts[TYPE];

                switch (type) {
                case "metacard":
                    String metacardName = pathParts[NAME];
                    Metacard metacard = null;
                    try {
                        metacard = transformer.transform(zipInputStream, id);
                    } catch (IOException | CatalogTransformerException e) {
                        LOGGER.debug("Could not transform metacard: {}", id);
                    }
                    catalogProvider.create(new CreateRequestImpl(metacard));
                    break;
                case "content":
                    // TODO (RCZ) - make sure to fetch content already there and include it
                    String contentFilename = pathParts[NAME];
                    ContentItem contentItem = new ContentItemImpl(id,
                            new ZipEntryByteSource(zipInputStream),
                            null,
                            null);
                    CreateStorageRequestImpl createStorageRequest = new CreateStorageRequestImpl(
                            Collections.singletonList(contentItem),
                            id,
                            new HashMap<>());
                    storageProvider.create(createStorageRequest);
                    storageProvider.commit(createStorageRequest);
                    break;
                case "derived":
                    // TODO (RCZ) - make sure to fetch content already there and include it
                    break;
                default:
                    LOGGER.debug("Cannot interpret type of " + type);
                }

                entry = zipInputStream.getNextEntry();
            }
        }
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

    private ZipValidator initZipValidator() {
        ZipValidator zipValidator = new ZipValidator();
        zipValidator.setSignaturePropertiesPath(Paths.get(System.getProperty("ddf.home"),
                "/etc/ws-security/server/signature.properties")
                .toString());
        zipValidator.init();
        return zipValidator;
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
}
