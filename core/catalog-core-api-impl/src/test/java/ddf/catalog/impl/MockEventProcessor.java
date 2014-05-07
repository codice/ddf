/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.InvalidSubscriptionException;
import ddf.catalog.event.Subscription;
import ddf.catalog.event.SubscriptionExistsException;
import ddf.catalog.event.SubscriptionNotFoundException;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;

public class MockEventProcessor implements EventProcessor, PostIngestPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockEventProcessor.class);

    private boolean wasPosted = false;

    private boolean wasSent = false;

    private Metacard lastEvent;

    public Metacard getLastEvent() {
        return lastEvent;
    }

    public boolean wasEventPosted() {
        return wasPosted;
    }

    public boolean wasEventSent() {
        return wasSent;
    }

    @Override
    public String createSubscription(Subscription subscription) throws InvalidSubscriptionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSubscription(Subscription subscription, String subscriptionId)
        throws InvalidSubscriptionException, SubscriptionExistsException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteSubscription(String subscriptionId) throws SubscriptionNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public void notifyCreated(Metacard newMetacard) {
        LOGGER.trace("ENTERING: notifyCreated");

        wasSent = true;
        wasPosted = true;
        lastEvent = newMetacard;
        LOGGER.trace("EXITING: notifyCreated");

    }

    @Override
    public void notifyDeleted(Metacard oldMetacard) {
        LOGGER.trace("ENTERING: notifyDeleted");

        wasSent = true;
        wasPosted = true;
        lastEvent = oldMetacard;
        LOGGER.trace("EXITING: notifyDeleted");

    }

    @Override
    public void notifyUpdated(Metacard newMetacard, Metacard oldMetacard) {
        LOGGER.trace("ENTERING: notifyUpdated");

        wasSent = true;
        wasPosted = true;
        lastEvent = newMetacard;
        LOGGER.trace("EXITING: notifyUpdated");

    }

    @Override
    public void updateSubscription(Subscription subscription, String subcriptionId)
        throws SubscriptionNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process (CreateResponse)");
        List<Metacard> createdMetacards = input.getCreatedMetacards();
        wasSent = true;
        wasPosted = true;
        lastEvent = createdMetacards.get(createdMetacards.size() - 1);
        LOGGER.trace("EXITING: process (CreateResponse)");

        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process (UpdateResponse)");
        List<Update> updates = input.getUpdatedMetacards();
        Update lastUpdate = updates.get(updates.size() - 1);
        wasSent = true;
        wasPosted = true;
        lastEvent = lastUpdate.getNewMetacard();
        LOGGER.trace("EXITING: process (UpdateResponse)");

        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        LOGGER.trace("ENTERING: process (DeleteResponse)");

        List<Metacard> deletedMetacards = input.getDeletedMetacards();
        wasSent = true;
        wasPosted = true;
        lastEvent = deletedMetacards.get(deletedMetacards.size() - 1);
        LOGGER.trace("EXITING: process (DeleteResponse)");

        return input;
    }

}
