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
package ddf.sdk.plugin.predelivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.StopProcessingException;

public class DummyPreDeliveryPlugin implements PreDeliveryPlugin {
    private static Logger LOGGER = LoggerFactory.getLogger(DummyPreDeliveryPlugin.class);

    private static String ENTERING = "ENTERING {}";
    private static String EXITING = "EXITING {}";

    public DummyPreDeliveryPlugin() {
        LOGGER.trace("INSIDE: DummyPreDeliveryPlugin constructor");
    }

    @Override
    public Metacard processCreate(Metacard metacard) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processCreate";
        LOGGER.trace(ENTERING, methodName);

        Metacard newMetacard = metacard;

        LOGGER.trace(EXITING, methodName);

        return newMetacard;
    }

    @Override
    public Update processUpdateMiss(Update update) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processUpdateMiss";
        LOGGER.trace(ENTERING, methodName);

        Metacard newMetacard = update.getNewMetacard();
        Metacard oldMetacard = update.getOldMetacard();
        Update newUpdate = new UpdateImpl(newMetacard, oldMetacard);

        LOGGER.trace(EXITING, methodName);

        return newUpdate;
    }

    @Override
    public Update processUpdateHit(Update update) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processUpdateHit";
        LOGGER.trace(ENTERING, methodName);

        Metacard newMetacard = update.getNewMetacard();
        Metacard oldMetacard = update.getOldMetacard();
        Update newUpdate = new UpdateImpl(newMetacard, oldMetacard);

        LOGGER.trace(EXITING, methodName);

        return newUpdate;
    }

    @Override
    public Metacard processDelete(Metacard metacard) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processDelete";
        LOGGER.trace(ENTERING, methodName);

        Metacard newMetacard = metacard;

        LOGGER.trace(EXITING, methodName);

        return newMetacard;
    }

}
