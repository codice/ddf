/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.content.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.MetacardTypeRegistry;
import ddf.content.ContentFramework;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.data.impl.IncomingContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.Request;
import ddf.content.operation.Request.Directive;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.operation.impl.CreateResponseImpl;
import ddf.content.operation.impl.DeleteRequestImpl;
import ddf.content.operation.impl.DeleteResponseImpl;
import ddf.content.operation.impl.ReadRequestImpl;
import ddf.content.operation.impl.UpdateResponseImpl;
import ddf.content.plugin.ContentPlugin;
import ddf.content.plugin.PluginExecutionException;
import ddf.content.plugin.PostCreateStoragePlugin;
import ddf.content.plugin.PostUpdateStoragePlugin;
import ddf.content.plugin.PreCreateStoragePlugin;
import ddf.content.plugin.PreUpdateStoragePlugin;
import ddf.content.storage.StorageException;
import ddf.content.storage.StorageProvider;
import ddf.security.SecurityConstants;

/**
 * ContentFrameworkImpl is the core class of the DDF Content Framework. It is used for create,
 * update, delete, and content retrieval operations for content stored in the DDF Content
 * Repository.
 */
public class ContentFrameworkImpl implements ContentFramework {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentFrameworkImpl.class);

    /**
     * The {@link List} of content plugins to execute on the ingest response after content has been
     * created, updated, or deleted in the content repository.
     */
    protected List<ContentPlugin> contentPlugins;

    /**
     * The {@link List} of storage plugins to execute on the ingest request before content has been
     * persisted to the filesystem.
     */
    protected List<PreCreateStoragePlugin> preCreateStoragePlugins;

    /**
     * The {@link List} of storage plugins to execute on the ingest response after content has been
     * persisted to the filesystem.
     */
    protected List<PostCreateStoragePlugin> postCreateStoragePlugins;

    /**
     * The {@link List} of storage plugins to execute on the update request before content has been
     * updated on the filesystem.
     */
    protected List<PreUpdateStoragePlugin> preUpdateStoragePlugins;

    /**
     * The {@link List} of storage plugins to execute on the update response after content has been
     * updated on the filesystem.
     */
    protected List<PostUpdateStoragePlugin> postUpdateStoragePlugins;

    protected MetacardTypeRegistry metacardTypeRegistry;

    private BundleContext context;

    private StorageProvider provider;

    private static final String STORAGE_PLUGIN_FAILED_TEXT =
            "Storage plugin processing failed. This is allowable. Skipping to next plugin.";

    /**
     * Instantiates a new ContentFrameworkImpl, usually invoked from blueprint.
     *
     * @param context                  The BundleContext that will be utilized by this instance.
     * @param provider                 The {@link StorageProvider} used for read, create, update, and delete operations.
     * @param contentPlugins           A list of {@link ContentPlugin}(s) that will be invoked after the ingest
     *                                 operation.
     * @param preCreateStoragePlugins  A list of {@link PreCreateStoragePlugin}(s) that will be invoked
     *                                 immediately before content is persisted to the filesystem.
     * @param postCreateStoragePlugins A list of {@link PostCreateStoragePlugin}(s) that will be invoked
     *                                 immediately after content has been persisted to the filesystem.
     * @param preUpdateStoragePlugins  A list of {@link PreUpdateStoragePlugin}(s) that will be invoked
     *                                 immediately before content is updated on the filesystem.
     * @param postUpdateStoragePlugins A list of {@link PostUpdateStoragePlugin}(s) that will be invoked
     *                                 immediately after content is updated on the filesystem.
     */
    public ContentFrameworkImpl(BundleContext context, StorageProvider provider,
            List<ContentPlugin> contentPlugins,
            List<PreCreateStoragePlugin> preCreateStoragePlugins,
            List<PostCreateStoragePlugin> postCreateStoragePlugins,
            List<PreUpdateStoragePlugin> preUpdateStoragePlugins,
            List<PostUpdateStoragePlugin> postUpdateStoragePlugins) {
        LOGGER.trace("ENTERING: ContentFrameworkImpl constructor");

        this.context = context;
        this.provider = provider;
        this.contentPlugins = contentPlugins;
        this.preCreateStoragePlugins = preCreateStoragePlugins;
        this.postCreateStoragePlugins = postCreateStoragePlugins;
        this.preUpdateStoragePlugins = preUpdateStoragePlugins;
        this.postUpdateStoragePlugins = postUpdateStoragePlugins;
        LOGGER.trace("EXITING: ContentFrameworkImpl constructor");
    }

    @Override
    public CreateResponse create(CreateRequest createRequest, Request.Directive directive)
            throws ContentFrameworkException {
        LOGGER.trace("ENTERING: create");

        LOGGER.debug("directive = " + directive);

        Subject subject = SecurityUtils.getSubject();

        if (subject instanceof ddf.security.Subject) {
            createRequest.getProperties()
                    .put(SecurityConstants.SECURITY_SUBJECT, (ddf.security.Subject) subject);
        }

        CreateResponse createResponse = null;

        // If directive includes processing and there are no ContentPlugins currently installed to
        // support processing, then throw an exception. (Do not want to do the STORE and get the
        // content repository out of sync with the Metadata Catalog.)
        if ((directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) && (
                this.contentPlugins.size() == 0)) {
            throw new ContentFrameworkException(
                    "Unable to perform " + directive + " because no ContentPlugins are installed.");
        }

        // Recreate content item so can add GUID to request
        ContentItem incomingContentItem = createRequest.getContentItem();
        String id = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
        LOGGER.debug("Created GUID: " + id);
        try {
            ContentItem contentItem = new IncomingContentItem(id,
                    incomingContentItem.getInputStream(),
                    incomingContentItem.getMimeTypeRawData(),
                    incomingContentItem.getFilename());
            contentItem.setUri(incomingContentItem.getUri());
            createRequest = new CreateRequestImpl(contentItem, createRequest.getProperties());
        } catch (IOException e1) {
            throw new ContentFrameworkException("Unable to add ID to IncomingContentItem", e1);
        }

        if (directive == Directive.STORE || directive == Directive.STORE_AND_PROCESS) {
            try {

                for (final PreCreateStoragePlugin plugin : preCreateStoragePlugins) {
                    try {
                        createRequest = plugin.process(createRequest);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(STORAGE_PLUGIN_FAILED_TEXT, e);
                    }
                }

                createResponse = provider.create(createRequest);

                for (final PostCreateStoragePlugin plugin : postCreateStoragePlugins) {
                    try {
                        createResponse = plugin.process(createResponse);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(STORAGE_PLUGIN_FAILED_TEXT, e);
                    }
                }

            } catch (StorageException e) {
                throw new ContentFrameworkException(e);
            } catch (Exception e) {
                LOGGER.warn("Content Provider error during create", e);
                throw new ContentFrameworkException(
                        "Unable to perform create because no content storage provider is installed or there is a problem with the content storage provider.",
                        e);
            }
        }

        if (directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) {
            if (directive == Directive.PROCESS) {
                // No content storage occurred to return a CreateResponse. So need to
                // instantiate a CreateResponse with the original CreateRequest's ContentItem
                // in it.
                if (createResponse == null) {
                    createResponse = new CreateResponseImpl(createRequest,
                            createRequest.getContentItem());
                }
            }

            LOGGER.debug("Number of ContentPlugins = " + contentPlugins.size());

            // Execute each ContentPlugin on the content item. If any plugin fails, then
            // assume the entire transaction fails, rolling back the storage of the content
            // item in the content repository (if applicable)
            try {
                for (final ContentPlugin plugin : contentPlugins) {
                    createResponse = plugin.process(createResponse);
                }
            } catch (PluginExecutionException e) {
                LOGGER.info("Content Plugin processing failed.", e);

                // If a STORE_AND_PROCESS directive was being done, will need to delete the
                // stored content item from the content repository (similar to a rollback)
                if (directive == Directive.STORE_AND_PROCESS) {
                    String contentId = createResponse.getCreatedContentItem()
                            .getId();
                    LOGGER.debug("Doing storage rollback - Deleting content item " + contentId);

                    ContentItem itemToDelete = new IncomingContentItem(contentId, null, null);
                    itemToDelete.setUri(incomingContentItem.getUri());
                    DeleteRequest deleteRequest = new DeleteRequestImpl(itemToDelete, null);
                    try {
                        this.provider.delete(deleteRequest);
                    } catch (Exception e2) {
                        LOGGER.warn(
                                "Unable to perform delete because no content storage provider is installed or there is a problem with the content storage provider.",
                                e2);
                    }

                    // Re-throw the exception (this will fail the Camel route that may have
                    // started this request)
                    throw new ContentFrameworkException(
                            "Content Plugin processing failed. Did not store item in content repository and did not create catalog entry.\n"
                                    + e.getMessage(),
                            e);
                } else {
                    // Re-throw the exception (this will fail the Camel route that may have
                    // started this request)
                    throw new ContentFrameworkException(
                            "Content Plugin processing failed. Did not create catalog entry.\n"
                                    + e.getMessage(),
                            e);
                }
            }
        }

        LOGGER.trace("EXITING: create");

        return createResponse;
    }

    @Override
    public ReadResponse read(ReadRequest readRequest) throws ContentFrameworkException {
        LOGGER.trace("ENTERING: read");

        ReadResponse response = null;

        try {
            response = this.provider.read(readRequest);
        } catch (StorageException e) {
            throw new ContentFrameworkException(e);
        } catch (Exception e) {
            LOGGER.warn("Content Provider error during read", e);
            throw new ContentFrameworkException(
                    "Unable to perform read because no content storage provider is installed or there is a problem with the content storage provider.",
                    e);
        }

        LOGGER.trace("EXITING: read");

        return response;
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest, Request.Directive directive)
            throws ContentFrameworkException {
        LOGGER.trace("ENTERING: update");

        Subject subject = SecurityUtils.getSubject();

        if (subject instanceof ddf.security.Subject) {
            updateRequest.getProperties()
                    .put(SecurityConstants.SECURITY_SUBJECT, (ddf.security.Subject) subject);
        }

        UpdateResponse updateResponse = null;

        // If directive includes processing and there are no ContentPlugins currently installed to
        // support processing, then throw an exception. (Do not want to do the STORE and get the
        // content repository out of sync with the Metadata Catalog.)
        if ((directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) && (
                this.contentPlugins.size() == 0)) {
            throw new ContentFrameworkException(
                    "Unable to perform " + directive + " because no ContentPlugins are installed.");
        }

        ContentItem itemToUpdate = updateRequest.getContentItem();

        if (directive == Directive.STORE || directive == Directive.STORE_AND_PROCESS) {
            // Verify content item exists in content repository before trying to update it
            try {
                ReadRequest readRequest = new ReadRequestImpl(updateRequest.getContentItem()
                        .getId(), null);
                this.provider.read(readRequest);
            } catch (StorageException e) {
                LOGGER.info("File does not exist, cannot update, doing a create: ", e);
                throw new ContentFrameworkException(
                        "File does not exist, cannot update, doing a create: ",
                        e);
            } catch (Exception e) {
                LOGGER.warn("Content Provider error during update", e);
                throw new ContentFrameworkException(
                        "Unable to perform update because no content storage provider is installed or there is a problem with the content storage provider.",
                        e);
            }

            LOGGER.info("Updating content repository for content item: " + itemToUpdate.getId());
            try {
                for (final PreUpdateStoragePlugin plugin : preUpdateStoragePlugins) {
                    try {
                        updateRequest = plugin.process(updateRequest);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(STORAGE_PLUGIN_FAILED_TEXT, e);
                    }
                }

                updateResponse = this.provider.update(updateRequest);

                for (final PostUpdateStoragePlugin plugin : postUpdateStoragePlugins) {
                    try {
                        updateResponse = plugin.process(updateResponse);
                    } catch (PluginExecutionException e) {
                        LOGGER.warn(STORAGE_PLUGIN_FAILED_TEXT, e);
                    }
                }

                try {
                    LOGGER.debug(
                            "updated item file length = " + updateResponse.getUpdatedContentItem()
                                    .getSize());
                } catch (IOException ioe) {
                }
            } catch (StorageException e) {
                throw new ContentFrameworkException(e);
            } catch (Exception e) {
                LOGGER.warn("Content Provider error during update", e);
                throw new ContentFrameworkException(
                        "Unable to perform update because no content storage provider is installed or there is a problem with the content storage provider.",
                        e);
            }
        }

        if (directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) {
            if (directive == Directive.PROCESS) {
                // No content update occurred to return an UpdateResponse. So need to
                // instantiate an UpdateResponse with the original UpdateRequest's ContentItem
                // in it.
                if (updateResponse == null) {
                    updateResponse = new UpdateResponseImpl(updateRequest,
                            updateRequest.getContentItem());
                }
            }

            // Execute each ContentPlugin on the content item. If any plugin fails, then
            // assume the entire transaction fails, rolling back the storage of the content
            // item in the content repository (if applicable)
            try {
                for (final ContentPlugin plugin : contentPlugins) {
                    updateResponse = plugin.process(updateResponse);
                }
            } catch (PluginExecutionException e) {

                LOGGER.info("Content Plugin processing failed.", e);

                // Re-throw the exception (this will fail the Camel route that may have
                // started this request)
                throw new ContentFrameworkException(
                        "Content Plugin processing failed. " + e.getMessage(), e);
            }
        }

        LOGGER.trace("EXITING: update");

        return updateResponse;
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest, Request.Directive directive)
            throws ContentFrameworkException {
        LOGGER.trace("ENTERING: delete");

        Subject subject = SecurityUtils.getSubject();

        if (subject instanceof ddf.security.Subject) {
            deleteRequest.getProperties()
                    .put(SecurityConstants.SECURITY_SUBJECT, (ddf.security.Subject) subject);
        }

        DeleteResponse deleteResponse = null;

        // If directive includes processing and there are no ContentPlugins currently installed to
        // support processing, then throw an exception. (Do not want to do the STORE and get the
        // content repository out of sync with the Metadata Catalog.)
        if ((directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) && (
                this.contentPlugins.size() == 0)) {
            throw new ContentFrameworkException(
                    "Unable to perform " + directive + " because no ContentPlugins are installed.");
        }

        if (directive == Directive.STORE || directive == Directive.STORE_AND_PROCESS) {
            try {
                deleteResponse = this.provider.delete(deleteRequest);
            } catch (StorageException e) {
                throw new ContentFrameworkException(e);
            } catch (Exception e) {
                LOGGER.warn("Content Provider error during delete", e);
                throw new ContentFrameworkException(
                        "Unable to perform delete because no content storage provider is installed or there is a problem with the content storage provider.",
                        e);
            }
        }

        if (directive == Directive.PROCESS || directive == Directive.STORE_AND_PROCESS) {
            if (directive == Directive.PROCESS) {
                // No content deletion occurred to return a DeleteResponse. So need to
                // instantiate a DeleteResponse with the original DeleteRequest's ContentItem
                // in it.
                if (deleteResponse == null) {
                    deleteResponse = new DeleteResponseImpl(deleteRequest,
                            deleteRequest.getContentItem(),
                            false);
                }
            }

            for (final ContentPlugin plugin : contentPlugins) {
                try {
                    deleteResponse = plugin.process(deleteResponse);
                } catch (PluginExecutionException e) {
                    LOGGER.info(
                            "Content Plugin processing failed. This is allowable. Skipping to next plugin.",
                            e);
                }
            }
        }

        LOGGER.trace("EXITING: delete");

        return deleteResponse;
    }

}
