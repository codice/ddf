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

/**
 * LogoutState represents the current state of an in progress logout
 */
public class LogoutState {
    private String initialRelayState;

    private final Set<String> spDescriptors;

    private String originalIssuer;

    private String nameId;

    private String originalRequestId;

    private String currentRequestId;

    private boolean partialLogout = false;

    public LogoutState(Set<String> spDescriptors) {
        this.spDescriptors = Collections.synchronizedSet(spDescriptors);
    }

    public synchronized void removeSpDescriptor(String descriptor) {
        spDescriptors.remove(descriptor);
    }

    public synchronized Optional<String> getNextTarget() {
        Optional<String> item = spDescriptors.stream()
                .findFirst();

        if (item.isPresent()) {
            spDescriptors.remove(item.get());
        }
        return item;
    }

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

    public void setOriginalRequestId(String originalRequestId) {
        this.originalRequestId = originalRequestId;
    }

    public String getOriginalRequestId() {
        return originalRequestId;
    }

    public boolean isPartialLogout() {
        return partialLogout;
    }

    public void setPartialLogout(boolean partialLogout) {
        this.partialLogout = partialLogout;
    }

    public String getCurrentRequestId() {
        return currentRequestId;
    }

    public void setCurrentRequestId(String currentRequestId) {
        this.currentRequestId = currentRequestId;
    }
}
