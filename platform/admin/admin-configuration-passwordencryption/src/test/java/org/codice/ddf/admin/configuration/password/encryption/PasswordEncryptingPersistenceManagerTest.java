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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.codice.ddf.admin.core.api.ConfigurationAdmin;
import org.codice.ddf.admin.core.api.MetatypeAttribute;
import org.codice.ddf.admin.core.api.Service;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import ddf.security.encryption.EncryptionService;

public class PasswordEncryptingPersistenceManagerTest {

    private PasswordEncryptingPersistenceManager passwordEncryptingPersistenceManager;

    private EncryptionService encryptionService;

    private PersistenceManager persistenceManager;

    private ConfigurationAdmin configurationAdmin;

    @Before
    public void setup() throws IOException {
        persistenceManager = mock(PersistenceManager.class);
        configurationAdmin = mock(ConfigurationAdmin.class);
        encryptionService = mock(EncryptionService.class);
        passwordEncryptingPersistenceManager = new PasswordEncryptingPersistenceManager(
                persistenceManager,
                encryptionService,
                configurationAdmin);
        Hashtable encrypted = new Hashtable();
        encrypted.put("service.pid", "encryptedPid");
        encrypted.put("password", "encrypted");
        encrypted.put("notpassword", "foo");
        Hashtable encryptedMsf = new Hashtable();
        encryptedMsf.put("service.factoryPid", "encryptedPid");
        encryptedMsf.put("password", "encrypted");
        encryptedMsf.put("notpassword", "foo");
        Hashtable noPassword = new Hashtable();
        noPassword.put("service.factoryPid", "noPasswordPid.uuid");
        noPassword.put("random", "stuff");
        Hashtable nullPassword = new Hashtable();
        nullPassword.put("service.pid", "nullPasswordPid");
        nullPassword.put("notstring", new ArrayList<String>());
        Enumeration enumeration = Collections.enumeration(Arrays.asList(encrypted,
                noPassword,
                nullPassword));
        when(persistenceManager.load("encryptedPid")).thenReturn(encrypted);
        when(persistenceManager.load("encryptedPid.uuid")).thenReturn(encryptedMsf);
        when(persistenceManager.load("noPasswordPid")).thenReturn(noPassword);
        when(persistenceManager.load("nullPasswordPid")).thenReturn(nullPassword);
        when(persistenceManager.getDictionaries()).thenReturn(enumeration);
        when(encryptionService.encrypt("decrypted")).thenReturn("encrypted");
        when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");
        AttributeDefinition attributeDefinition = mock(AttributeDefinition.class);
        when(attributeDefinition.getID()).thenReturn("password");
        when(attributeDefinition.getType()).thenReturn(AttributeDefinition.PASSWORD);
        AttributeDefinition attributeDefinition1 = mock(AttributeDefinition.class);
        when(attributeDefinition1.getID()).thenReturn("notstring");
        when(attributeDefinition1.getType()).thenReturn(AttributeDefinition.PASSWORD);
        AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {attributeDefinition, };
        ObjectClassDefinition objectClassDefinition = mock(ObjectClassDefinition.class);
        when(objectClassDefinition.getID()).thenReturn("metatype");
        when(objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attributeDefinitions);
        when(configurationAdmin.getObjectClassDefinition(anyString())).thenReturn(objectClassDefinition);
    }

    @Test
    public void testLoad() throws IOException {
        Dictionary dictionary = passwordEncryptingPersistenceManager.load("encryptedPid");
        assertThat(dictionary.get("password"), is("decrypted"));
        assertThat(dictionary.get("notpassword"), is("foo"));
    }

    @Test
    public void testLoadNoDecrypt() throws IOException {
        Dictionary dictionary = passwordEncryptingPersistenceManager.load("noPasswordPid");
        assertThat(dictionary.get("random"), is("stuff"));
        verifyZeroInteractions(encryptionService);
    }

    @Test
    public void testLoadNoDecryptNotString() throws IOException {
        passwordEncryptingPersistenceManager.load("nullPasswordPid");
        verifyZeroInteractions(encryptionService);
    }

    @Test
    public void testStore() throws IOException {
        Hashtable decrypted = new Hashtable();
        decrypted.put("password", "decrypted");
        passwordEncryptingPersistenceManager.store("encryptedPid", decrypted);
        verify(encryptionService, times(1)).encrypt("decrypted");
    }

    @Test
    public void testStoreMsf() throws IOException {
        Hashtable decrypted = new Hashtable();
        decrypted.put("password", "decrypted");
        passwordEncryptingPersistenceManager.store("encryptedPid.uuid", decrypted);
        verify(encryptionService, times(1)).encrypt("decrypted");
    }

    @Test
    public void testStoreNullService() throws IOException {
        when(configurationAdmin.getObjectClassDefinition(anyString())).thenReturn(null);
        Hashtable decrypted = new Hashtable();
        decrypted.put("password", "decrypted");
        passwordEncryptingPersistenceManager.store("encryptedPid", decrypted);
        verifyZeroInteractions(encryptionService);
    }

    @Test
    public void testGetDictionaries() throws IOException {
        Enumeration dictionaries = passwordEncryptingPersistenceManager.getDictionaries();
        assertThat(((Dictionary) dictionaries.nextElement()).get("password"), is("decrypted"));
        assertThat(((Dictionary) dictionaries.nextElement()).get("random"), is("stuff"));
        verify(persistenceManager, times(1)).getDictionaries();
    }

    @Test
    public void testExists() {
        passwordEncryptingPersistenceManager.exists("encryptedPid");
        verify(persistenceManager, times(1)).exists("encryptedPid");
    }

    @Test
    public void testDelete() throws IOException {
        passwordEncryptingPersistenceManager.delete("encryptedPid");
        verify(persistenceManager, times(1)).delete("encryptedPid");
    }

    class ServiceImpl extends HashMap<String, Object> implements Service {

    }

    class MetatypeAttributeImpl extends HashMap<String, Object> implements MetatypeAttribute {

    }
}
