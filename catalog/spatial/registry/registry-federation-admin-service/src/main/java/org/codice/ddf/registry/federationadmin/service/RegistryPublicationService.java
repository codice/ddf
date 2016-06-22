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
 **/
package org.codice.ddf.registry.federationadmin.service;

import ddf.catalog.data.Metacard;

public interface RegistryPublicationService {

    /**
     * Publishes a local registry to a remote registry.
     * <p>This will write the local metacard to the remote destination.
     * <p>The remote destination will be added to the local metacard's list of published locations.
     * The local metacard's last published time will also be updated. The updated metacard will be written to the local store.
     *
     * @param registryId the registry id of the registry to be published
     * @param destinationRegistryId the registry of the remote registry to be published to
     * @throws FederationAdminException if there were problems writing to the remote registry or updating the local metacard
     */
    void publish(String registryId, String destinationRegistryId) throws FederationAdminException;

    /**
     * Unpublishes a local registry from a remote registry.
     * <p> The local registry metacard will be deleted from the remote registry.
     * <p>The remote destination will be removed from the local metacard's list of published locations.
     * The local metacard's last published time will also be updated. The updated metacard will be written to the local store.
     * @param registryId the registry id of the registry to be unpublished
     * @param destinationRegistryId the registry of the remote registry to be unpublished from
     * @throws FederationAdminException if there were problems deleting from the remote registry or updating the local metacard
     */
    void unpublish(String registryId, String destinationRegistryId) throws FederationAdminException;

    /**
     * Writes the metacard to each of the remote registries in the metacard's list of published locations.
     * <p>The metacard's last published time will be updated and the updated metacard will be written to the local store.
     * @param metacard the metacard up write
     * @throws FederationAdminException if there are any problems updating the local or remote registries.
     */
    void update(Metacard metacard) throws FederationAdminException;
}
