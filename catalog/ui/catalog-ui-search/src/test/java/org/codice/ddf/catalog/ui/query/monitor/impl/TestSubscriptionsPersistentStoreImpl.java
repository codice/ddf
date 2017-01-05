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
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Before;
import org.junit.Test;

public class TestSubscriptionsPersistentStoreImpl {

    public static final String EMAIL1 = "test@test.com";

    public static final String EMAIL2 = "foo@foo.com";

    public static final String EMAIL3 = "a@a.com";

    public static final String EMAIL4 = "b@b.com";

    private static final String IDSTRING = "idstring";

    private SubscriptionsPersistentStoreImpl store;

    @Before
    public void setup() {

        PersistentStore persistentStore = new MemoryPersistentStore();

        store = new SubscriptionsPersistentStoreImpl(persistentStore);

    }

    /**
     * Remove an email from an empty store
     */
    @Test
    public void testRemoveEmailEmpty() {
        store.removeEmail(IDSTRING, EMAIL1);
        assertThat(store.getEmails(IDSTRING), is(Collections.emptySet()));
    }

    @Test
    public void testRemoveEmail() {
        store.addEmail(IDSTRING, EMAIL1);
        store.removeEmail(IDSTRING, EMAIL1);
        assertThat(store.getEmails(IDSTRING), is(Collections.emptySet()));
    }

    @Test
    public void testRemoveAllEmails() {
        store.addEmail(IDSTRING, EMAIL1);
        store.addEmail(IDSTRING, EMAIL2);
        store.removeAllEmails(IDSTRING);
        assertThat(store.getEmails(IDSTRING), is(Collections.emptySet()));
    }

    @Test
    public void testAddEmail() {

        store.addEmail(IDSTRING, EMAIL1);

        Set<String> results = store.getEmails(IDSTRING);

        assertThat(results, is(Collections.singleton(EMAIL1)));
    }

    @Test
    public void testAddEmails() {

        store.addEmails(IDSTRING, new HashSet<>(Arrays.asList(EMAIL1, EMAIL2)));

        Set<String> results = store.getEmails(IDSTRING);

        assertThat(results, is(new HashSet<>(Arrays.asList(EMAIL1, EMAIL2))));
    }

    @Test
    public void testAddEmailsMultiple() {

        store.addEmails(IDSTRING, new HashSet<>(Arrays.asList(EMAIL1, EMAIL2)));
        store.addEmails(IDSTRING, new HashSet<>(Arrays.asList(EMAIL3, EMAIL4)));

        Set<String> results = store.getEmails(IDSTRING);

        assertThat(results, is(new HashSet<>(Arrays.asList(EMAIL1, EMAIL2, EMAIL3, EMAIL4))));

    }

    /**
     * Add a single email and then multiple emails
     */
    @Test
    public void testAddEmailsMultiple2() {

        store.addEmail(IDSTRING, EMAIL1);
        store.addEmails(IDSTRING, new HashSet<>(Arrays.asList(EMAIL3, EMAIL4)));

        Set<String> results = store.getEmails(IDSTRING);

        assertThat(results, is(new HashSet<>(Arrays.asList(EMAIL1, EMAIL3, EMAIL4))));

    }

    @Test
    public void testGetEmailsEmpty() {
        assertThat(store.getEmails(IDSTRING), is(Collections.emptySet()));
    }

    private static class MemoryPersistentStore implements PersistentStore {

        private Map<String, Map<String, Object>> map = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public void add(String type, Map<String, Object> properties) throws PersistenceException {
            String id = ((String) properties.get("id_txt")).replace("_txt", "");
            Map<String, Object> copy = new HashMap<>(properties);
            copy.computeIfPresent("email_txt", (k, v) -> {
                if (String.class.isInstance(v)) {
                    return v;
                }
                if (Set.class.isInstance(v)) {
                    Set<String> emails = (Set<String>) v;
                    if (emails.size() == 1) {
                        return emails.toArray()[0];
                    } else {
                        return v;
                    }
                }
                throw new RuntimeException();
            });
            map.put(id, copy);
        }

        @Override
        public List<Map<String, Object>> get(String type) throws PersistenceException {
            throw new RuntimeException("not implemented");
        }

        @Override
        public List<Map<String, Object>> get(String type, String ecql) throws PersistenceException {
            String id = ecql.split("=")[1].replaceAll("'", "");
            if (map.containsKey(id)) {
                return Collections.singletonList(map.get(id));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public int delete(String type, String ecql) throws PersistenceException {
            throw new RuntimeException("not implemented");
        }
    }

}
