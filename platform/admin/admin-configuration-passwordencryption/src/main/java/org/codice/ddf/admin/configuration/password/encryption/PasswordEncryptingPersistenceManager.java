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
package org.codice.ddf.admin.configuration.password.encryption;

import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Function;

import org.apache.felix.cm.PersistenceManager;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import ddf.security.encryption.EncryptionService;

public class PasswordEncryptingPersistenceManager implements PersistenceManager {

    private PersistenceManager persistenceManager;

    private EncryptionService encryptionService;

    private ConfigurationAdmin configurationAdmin;

    public PasswordEncryptingPersistenceManager(PersistenceManager persistenceManager,
            EncryptionService encryptionService, ConfigurationAdmin configurationAdmin) {
        this.persistenceManager = persistenceManager;
        this.encryptionService = encryptionService;
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public boolean exists(String pid) {
        return persistenceManager.exists(pid);
    }

    @Override
    public Dictionary load(String pid) throws IOException {
        Dictionary dictionary = persistenceManager.load(pid);
        updatePasswords(pid, dictionary, encryptionService::decrypt);
        return dictionary;
    }

    @Override
    public Enumeration getDictionaries() throws IOException {
        List<Dictionary> dictionaryList = new ArrayList<>();
        Enumeration dictionaries = persistenceManager.getDictionaries();
        while (dictionaries.hasMoreElements()) {
            Dictionary dictionary = (Dictionary) dictionaries.nextElement();
            String pid = (String) ((dictionary.get(SERVICE_FACTORYPID) == null) ? dictionary.get(
                    SERVICE_PID) : dictionary.get(SERVICE_FACTORYPID));
            if (pid != null) {
                updatePasswords(pid, dictionary, encryptionService::decrypt);
            }
            dictionaryList.add(dictionary);
        }
        return Collections.enumeration(dictionaryList);
    }

    @Override
    public void store(String pid, Dictionary dictionary) throws IOException {
        Dictionary tempDictionary = new Hashtable();
        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            tempDictionary.put(key, dictionary.get(key));
        }
        updatePasswords(pid, tempDictionary, encryptionService::encrypt);
        persistenceManager.store(pid, tempDictionary);
    }

    private void updatePasswords(String pid, Dictionary dictionary,
            Function<String, String> passwordTransform) {
        String trimmedPid = pid;
        if (dictionary.get(SERVICE_FACTORYPID) != null) {
            trimmedPid = (String) dictionary.get(SERVICE_FACTORYPID);
        }
        ObjectClassDefinition objectClassDefinition = configurationAdmin.getObjectClassDefinition(
                trimmedPid);
        if (objectClassDefinition != null) {
            AttributeDefinition[] attributeDefinitions =
                    objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);
            for (AttributeDefinition metatypeAttribute : attributeDefinitions) {
                if (metatypeAttribute.getType() == AttributeDefinition.PASSWORD) {
                    Object beforePassword = dictionary.get(metatypeAttribute.getID());
                    if (beforePassword instanceof String) {
                        String afterPassword = passwordTransform.apply((String) beforePassword);
                        dictionary.put(metatypeAttribute.getID(), afterPassword);
                    }
                }
            }
        }
    }

    @Override
    public void delete(String pid) throws IOException {
        persistenceManager.delete(pid);
    }
}
