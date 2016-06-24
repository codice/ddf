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
package org.codice.ddf.catalog.ui.query.monitor.api;

import java.util.Set;

public interface SubscriptionsPersistentStore {

    public static final String SUBSCRIPTIONS_TYPE = "subscriptions";

    /**
     * Associate a set of email addresses with an ID. Adding an email that is already
     * assoicated with an ID is a no-op.
     *
     * @param id     must be non-null and non-blank
     * @param emails must be non-null and elements must be non-blank
     */
    void addEmails(String id, Set<String> emails);

    /**
     * Associate an email address with an ID. Adding an email that is already
     * assoicated with an ID is a no-op.
     *
     * @param id    must be non-null and non-blank
     * @param email must be non-null and non-blank
     */
    void addEmail(String id, String email);

    /**
     * Remove all emails that have been associated with an ID.
     *
     * @param id must be non-null and non-blank
     */
    void removeAllEmails(String id);

    /**
     * Remove a set of emails that have been associated with an ID. Removing an email that
     * is not associated with an ID is a no-op.
     *
     * @param id     must be non-null and non-blank
     * @param emails must be non-null and elements must be non-blank
     */
    void removeEmails(String id, Set<String> emails);

    /**
     * Remove an email that has been associated with an ID. Removing an email that is not
     * associated with an ID is a no-op.
     *
     * @param id    must be non-null and non-blank
     * @param email must be non-null
     */
    void removeEmail(String id, String email);

    /**
     * Get the set of emails that have been associated with an ID.
     *
     * @param id must be non-null and non-blank
     * @return a non-null set of email addresses
     */
    Set<String> getEmails(String id);

}
