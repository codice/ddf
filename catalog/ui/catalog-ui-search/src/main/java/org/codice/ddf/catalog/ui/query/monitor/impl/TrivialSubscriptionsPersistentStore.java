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

import static org.apache.commons.lang3.Validate.notBlank;

import java.util.Collections;
import java.util.Set;

import org.codice.ddf.catalog.ui.query.monitor.api.SubscriptionsPersistentStore;

public class TrivialSubscriptionsPersistentStore implements SubscriptionsPersistentStore {

    private final String emailAddress;

    public TrivialSubscriptionsPersistentStore(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public void addEmails(String id, Set<String> emails) {

    }

    @Override
    public void removeEmails(String id, Set<String> emails) {

    }

    @Override
    public Set<String> getEmails(String id) {
        return Collections.singleton(emailAddress);
    }

    @Override
    public Set<String> getSubscriptions(String email) {
        return null;
    }

    @Override
    public final void addEmail(String id, String email) {
        notBlank(id, "id must be non-blank");
        notBlank(email, "email must be non-blank");

        addEmails(id, Collections.singleton(email));
    }

    @Override
    public final void removeAllEmails(String id) {
        removeEmails(id, getEmails(id));
    }

    @Override
    public final void removeEmail(String id, String email) {
        notBlank(id, "id must be non-blank");
        notBlank(email, "email must be non-blank");

        removeEmails(id, Collections.singleton(email));
    }
}
