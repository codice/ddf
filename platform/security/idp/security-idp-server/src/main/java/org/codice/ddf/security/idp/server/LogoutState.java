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
package org.codice.ddf.security.idp.server;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.opensaml.saml2.metadata.SPSSODescriptor;

/**
 * LogoutState represents the current state of an in progress logout
 */
public class LogoutState {
    private String initialRelayState;

    private final Set<SPSSODescriptor> spDescriptors;

    private String originalIssuer;

    private String nameId;

    public LogoutState(Set<SPSSODescriptor> spDescriptors) {
        this.spDescriptors = Collections.synchronizedSet(spDescriptors);
    }

    public synchronized void removeSpDescriptor(SPSSODescriptor descriptor) {
        spDescriptors.remove(descriptor);
    }

    public synchronized Optional<SPSSODescriptor> getNextTarget() {
        Optional<SPSSODescriptor> item =  spDescriptors.stream()
                .findFirst();

        if (item.isPresent()) {
            spDescriptors.remove(item.get());
        }
        return item;
    }

    /**
     * Get the remaining SP Descriptors still needing to be logged out
     *
     * @return An unmodifiable copy of the descriptors
     */
    /*public Set<SPSSODescriptor> getSpDescriptors() {
        return Collections.unmodifiableSet(new HashSet<>(spDescriptors));
    }*/
    public String getInitialRelayState() {
        return initialRelayState;
    }

    public void setInitialRelayState(String initialRelayState) {
        this.initialRelayState = initialRelayState;
    }

    public String getOriginalIssuer() {
        return originalIssuer;
    }

    public void setOriginalIssuer(String originalIssuer) {
        this.originalIssuer = originalIssuer;
    }

    public String getNameId() {
        return nameId;
    }

    public void setNameId(String nameId) {
        this.nameId = nameId;
    }
}
