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
package ddf.service.kml.subscription;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.DeliveryMethod;

public class KmlUpdateDeliveryMethod implements DeliveryMethod {

    private ConcurrentLinkedQueue<Metacard> createdQueue;

    private ConcurrentLinkedQueue<Metacard> updatedQueue;

    private ConcurrentLinkedQueue<Metacard> deletedQueue;

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(KmlUpdateDeliveryMethod.class));

    public KmlUpdateDeliveryMethod() {
        createdQueue = new ConcurrentLinkedQueue<Metacard>();
        updatedQueue = new ConcurrentLinkedQueue<Metacard>();
        deletedQueue = new ConcurrentLinkedQueue<Metacard>();
    }

    @Override
    public void created(Metacard metacard) {
        String methodName = "created";
        logger.entry(methodName);
        createdQueue.add(metacard);
    }

    @Override
    public void deleted(Metacard metacard) {
        String methodName = "deleted";
        logger.entry(methodName);
        deletedQueue.add(metacard);
    }

    public Queue<Metacard> getCreated() {
        return createdQueue;
    }

    public Queue<Metacard> getUpdated() {
        return updatedQueue;
    }

    public Queue<Metacard> getDeleted() {
        return deletedQueue;
    }

    @Override
    public void updatedHit(Metacard newMetacard, Metacard oldMetacard) {
        String methodName = "updated";
        logger.entry(methodName);
        updatedQueue.add(newMetacard);
    }

    @Override
    public void updatedMiss(Metacard newMetacard, Metacard oldMetacard) {
        // TODO Auto-generated method stub

    }

}
