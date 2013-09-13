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

import org.apache.log4j.Logger;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreDeliveryPlugin;
import ddf.catalog.plugin.StopProcessingException;

public class DummyPreDeliveryPlugin implements PreDeliveryPlugin {
    private static Logger logger = Logger.getLogger(DummyPreDeliveryPlugin.class);

    public DummyPreDeliveryPlugin() {
        logger.trace("INSIDE: DummyPreDeliveryPlugin constructor");
    }

    @Override
    public Metacard processCreate(Metacard metacard) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processCreate";
        logger.trace("ENTERING: " + methodName);

        Metacard newMetacard = metacard;

        logger.trace("EXITING: " + methodName);

        return newMetacard;
    }

    @Override
    public Update processUpdateMiss(Update update) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processUpdateMiss";
        logger.trace("ENTERING: " + methodName);

        Metacard newMetacard = update.getNewMetacard();
        Metacard oldMetacard = update.getOldMetacard();
        Update newUpdate = new UpdateImpl(newMetacard, oldMetacard);

        logger.trace("EXITING: " + methodName);

        return newUpdate;
    }

    @Override
    public Update processUpdateHit(Update update) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processUpdateHit";
        logger.trace("ENTERING: " + methodName);

        Metacard newMetacard = update.getNewMetacard();
        Metacard oldMetacard = update.getOldMetacard();
        Update newUpdate = new UpdateImpl(newMetacard, oldMetacard);

        logger.trace("EXITING: " + methodName);

        return newUpdate;
    }

    @Override
    public Metacard processDelete(Metacard metacard) throws PluginExecutionException,
        StopProcessingException {
        String methodName = "processDelete";
        logger.trace("ENTERING: " + methodName);

        Metacard newMetacard = metacard;

        logger.trace("EXITING: " + methodName);

        return newMetacard;
    }

}
