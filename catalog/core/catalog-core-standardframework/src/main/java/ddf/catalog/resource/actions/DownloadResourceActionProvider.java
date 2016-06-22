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
package ddf.catalog.resource.actions;

import static ddf.catalog.resource.download.ResourceDownloadEndpoint.CONTEXT_PATH;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.catalog.actions.AbstractMetacardActionProvider;
import org.codice.ddf.configuration.SystemBaseUrl;

import ddf.action.Action;
import ddf.action.impl.ActionImpl;
import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;

/**
 * Action provider that creates {@link Action}s used to asynchronously download resources to
 * the product cache.
 */
public class DownloadResourceActionProvider extends AbstractMetacardActionProvider {

    private static final String TITLE = "Download resource to local cache";

    private static final String DESCRIPTION =
            "Downloads the resource from a federated source to the local product cache";

    private final ResourceCacheInterface resourceCache;

    private final ReliableResourceDownloadManager downloadManager;

    public DownloadResourceActionProvider(String actionProviderId,
            ResourceCacheInterface resourceCache, ReliableResourceDownloadManager downloadManager) {
        super(actionProviderId, TITLE, DESCRIPTION);
        this.resourceCache = resourceCache;
        this.downloadManager = downloadManager;
    }

    /**
     * Returns {@code true} only if caching is enabled and the {@link Metacard} provided is not
     * already being downloaded or cached.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleMetacard(Metacard metacard) {
        return downloadManager.isCacheEnabled() && !isCached(metacard);
    }

    @Override
    protected URL getMetacardActionUrl(String metacardSource, Metacard metacard) throws Exception {
        String encodedMetacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
        String encodedMetacardSource = URLEncoder.encode(metacardSource, CharEncoding.UTF_8);
        return getActionUrl(encodedMetacardSource, encodedMetacardId);
    }

    protected Action createMetacardAction(String actionProviderId, String title, String description,
            URL url) {
        return new ActionImpl(actionProviderId, title, description, url);
    }

    private boolean isCached(Metacard metacard) {
        String key = new CacheKey(metacard).generateKey();
        return resourceCache.isPending(key) || resourceCache.containsValid(key, metacard);
    }

    private URL getActionUrl(String metacardSource, String metacardId) throws Exception {
        return new URI(SystemBaseUrl.constructUrl(String.format("%s/%s/%s",
                CONTEXT_PATH,
                metacardSource,
                metacardId), true)).toURL();
    }
}
