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
package ddf.sdk.plugin.preingest; // TODO: Change package name.

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateRequestImpl;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;

/***************************************************************************************
 * Follow DDF Developer's Guide for examples
 ****************************************************************************************/

public class DummyPreIngestPlugin implements PreIngestPlugin {
    // TODO: Fill in all methods with appropriate logic
    private static Logger LOGGER = LoggerFactory.getLogger(DummyPreIngestPlugin.class.getName());

    private static String ENTERING = "ENTERING {}";
    private static String EXITING = "EXITING {}";

    public DummyPreIngestPlugin() {

    }

    public int getPriority() {
        // In this example we give this service the second highest priority
        return 2;
    }

    public CreateRequest process(CreateRequest input) throws PluginExecutionException {
        String methodName = "process(CreateRequest)";
        LOGGER.debug(ENTERING, methodName);

        CreateRequest newRequest = input;
        if (input != null) {

            List<Metacard> filteredCards = filterOutMetacards(input.getMetacards());
            newRequest = new CreateRequestImpl(filteredCards);
        }

        LOGGER.debug(EXITING, methodName);

        return newRequest;
    }

    public UpdateRequest process(UpdateRequest input) throws PluginExecutionException {
        String methodName = "process(UpdateRequest)";
        LOGGER.debug(ENTERING, methodName);

        UpdateRequest newRequest = input;

        if (newRequest != null) {
            List<Entry<Serializable, Metacard>> updates = newRequest.getUpdates();

            List<Metacard> updatedMetacards = new ArrayList<Metacard>();

            for (Entry<Serializable, Metacard> updateEntry : updates) {
                Metacard metacard = updateEntry.getValue();
                updatedMetacards.add(metacard);
            }

            // Loop to get all ids
            List<String> ids = new ArrayList<String>();
            int i = 0;
            for (Entry<Serializable, Metacard> updateEntry : updates) {

                if (i % 2 == 0) {
                    ids.add((String) updateEntry.getKey());
                }
                i++;

            }

            updatedMetacards = this.filterOutMetacards(updatedMetacards);
            LOGGER.debug("Returning new update request with id list size: {} and metacard list size: {}", ids.size(), updatedMetacards.size());
            newRequest = new UpdateRequestImpl((String[]) (ids.toArray(new String[ids.size()])),
                    updatedMetacards);
        }

        LOGGER.debug(EXITING, methodName);

        return newRequest;
    }

    public DeleteRequest process(DeleteRequest input) throws PluginExecutionException {
        String methodName = "process(DeleteRequest)";
        LOGGER.debug(ENTERING, methodName);

        DeleteRequest newRequest = input;

        // List<String> results = new ArrayList<String>();
        // if(idsToDelete != null)
        // {
        // int size = idsToDelete.size();
        //
        // //In this example, we demonstrate filtering out every other
        // //id in the list
        // for(int i = 0; i < size; i++) {
        // if(i % 2 == 0) {
        // results.add(idsToDelete.get(i));
        // }
        // }
        // }

        LOGGER.debug(EXITING, methodName);
        //
        // return results;
        return newRequest;
    }

    private List<Metacard> filterOutMetacards(List<Metacard> cards) {
        String methodName = "filterOutMetacards";
        LOGGER.debug(ENTERING, methodName);

        List<Metacard> results = new ArrayList<Metacard>();
        if (cards != null) {
            int size = cards.size();

            // In this example, we demonstrate filtering out every other
            // metacard in the list
            for (int i = 0; i < size; i++) {
                if (i % 2 == 0) {
                    results.add(cards.get(i));
                }
            }

            LOGGER.debug("Original size of Metacard list: {}", size);
            LOGGER.debug("Filtered size of Metacard list: {}", results.size());
        }

        LOGGER.debug(EXITING, methodName);

        return results;
    }

    private List<Metacard> filterOutIds(List<Metacard> cards) {
        String methodName = "filterOutMetacards";
        LOGGER.debug(ENTERING, methodName);

        List<Metacard> results = new ArrayList<Metacard>();
        if (cards != null) {
            int size = cards.size();

            // In this example, we demonstrate filtering out every other
            // metacard in the list
            for (int i = 0; i < size; i++) {
                if (i % 2 == 0) {
                    results.add(cards.get(i));
                }
            }

            LOGGER.debug("Original size of Metacard list: {}", size);
            LOGGER.debug("Filtered size of Metacard list: {}", results.size());
        }

        LOGGER.debug(EXITING, methodName);

        return results;
    }
}
