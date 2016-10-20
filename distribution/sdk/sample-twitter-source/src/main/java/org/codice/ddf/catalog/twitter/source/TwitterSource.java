/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.catalog.twitter.source;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterSource implements FederatedSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterSource.class);

    TwitterFactory twitterFactory;

    String id;

    ResourceReader resourceReader;

    String consumerKey;

    String consumerSecret;

    public TwitterSource() {

    }

    public void init() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        Configuration configuration = configurationBuilder.setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setApplicationOnlyAuthEnabled(true)
                .build();

        twitterFactory = new TwitterFactory(configuration);
    }

    public void destroy() {
        twitterFactory = null;
    }

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return resourceReader.retrieveResource(uri, arguments);
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        return Collections.emptySet();
    }

    @Override
    public boolean isAvailable() {
        Twitter instance = twitterFactory.getInstance();
        try {
            instance.getOAuth2Token();
            return true;
        } catch (TwitterException e) {
            LOGGER.error("Unable to get OAuth2 token.", e);
            return false;
        }
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        if (isAvailable()) {
            callback.setAvailable();
            return true;
        } else {
            callback.setUnavailable();
            return false;
        }
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        Twitter instance = twitterFactory.getInstance();
        try {
            instance.getOAuth2Token();
        } catch (TwitterException e) {
            throw new UnsupportedQueryException("Unable to get OAuth2 token.", e);
        }
        TwitterFilterVisitor visitor = new TwitterFilterVisitor();
        request.getQuery()
                .accept(visitor, null);
        Query query = new Query();
        query.setCount(request.getQuery()
                .getPageSize());
        if (visitor.hasSpatial()) {
            GeoLocation geoLocation = new GeoLocation(visitor.getLatitude(),
                    visitor.getLongitude());
            query.setGeoCode(geoLocation, visitor.getRadius(), Query.Unit.km);
        }
        if (visitor.getContextualSearch() != null) {
            query.setQuery(visitor.getContextualSearch()
                    .getSearchPhrase());
        }
        if (visitor.getTemporalSearch() != null) {
            Calendar.Builder builder = new Calendar.Builder();
            builder.setInstant(visitor.getTemporalSearch()
                    .getStartDate());
            Calendar calendar = builder.build();
            query.setSince(calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-"
                    + calendar.get(Calendar.DAY_OF_MONTH));

            builder = new Calendar.Builder();
            builder.setInstant(visitor.getTemporalSearch()
                    .getEndDate());
            calendar = builder.build();
            query.setUntil(calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH) + "-"
                    + calendar.get(Calendar.DAY_OF_MONTH));
        }

        QueryResult queryResult;
        try {
            queryResult = instance.search()
                    .search(query);
        } catch (TwitterException e) {
            throw new UnsupportedQueryException(e);
        }
        List<Result> resultList = new ArrayList<>(queryResult.getCount());
        resultList.addAll(queryResult.getTweets()
                .stream()
                .map(status -> new ResultImpl(getMetacard(status)))
                .collect(Collectors.toList()));
        return new SourceResponseImpl(request, resultList);
    }

    private Metacard getMetacard(Status status) {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(id);
        metacard.setId(String.valueOf(status.getId()));
        metacard.setTitle(status.getText());
        metacard.setMetadata("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Resource>" +
                "<name>" + status.getText() + "</name>" +
                "</Resource>");
        metacard.setCreatedDate(status.getCreatedAt());
        metacard.setModifiedDate(status.getCreatedAt());
        metacard.setEffectiveDate(status.getCreatedAt());
        metacard.setPointOfContact(status.getUser()
                .getName());
        if (status.getURLEntities() != null && status.getURLEntities().length > 0) {
            try {
                metacard.setResourceURI(new URI(status.getURLEntities()[0].getExpandedURL()));
            } catch (URISyntaxException e) {
                LOGGER.error("Unable to set resource URI.", e);
            }
        } else if (status.getMediaEntities() != null && status.getMediaEntities().length > 0) {
            try {
                metacard.setResourceURI(new URI(status.getMediaEntities()[0].getExpandedURL()));
            } catch (URISyntaxException e) {
                LOGGER.error("Unable to set resource URI.", e);
            }
        } else if (status.getExtendedMediaEntities() != null
                && status.getExtendedMediaEntities().length > 0) {
            try {
                metacard.setResourceURI(
                        new URI(status.getExtendedMediaEntities()[0].getExpandedURL()));
            } catch (URISyntaxException e) {
                LOGGER.error("Unable to set resource URI.", e);
            }
        }
        GeoLocation geoLocation = status.getGeoLocation();
        if (geoLocation != null) {
            metacard.setLocation(
                    "POINT (" + geoLocation.getLongitude() + " " + geoLocation.getLatitude() + ")");
        }

        return metacard;
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return Collections.emptySet();
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return "Twitter Federated Source";
    }

    @Override
    public String getDescription() {
        return "Query using the Twitter API.";
    }

    @Override
    public String getOrganization() {
        return "Codice";
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setResourceReader(ResourceReader resourceReader) {
        this.resourceReader = resourceReader;
    }
}
