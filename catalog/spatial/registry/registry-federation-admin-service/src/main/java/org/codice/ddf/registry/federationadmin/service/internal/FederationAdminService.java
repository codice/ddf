/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.federationadmin.service.internal;

import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

/**
 * Service that provides the interface to get and add registry metacards <b> This code is
 * experimental. While this interface is functional and tested, it may change or be removed in a
 * future version of the library. </b>
 */
public interface FederationAdminService {

  /**
   * Add a registry metacard entry for the xml string provided. Converts the string to a registry
   * metacard using the RegistryTransformer.
   *
   * @param xml String representation of a registry package xml
   * @return Id of the created metacard
   * @throws FederationAdminException If the xml string is blank. If an exception is thrown from the
   *     registryTransformer. The CatalogFramework throws an exception trying to the metacard.
   */
  String addRegistryEntry(String xml) throws FederationAdminException;

  /**
   * Add a registry metacard entry for the xml string provided. Converts the string to a registry
   * metacard using the RegistryTransformer.
   *
   * @param xml String representation of a registry package xml
   * @param destinations Set of destinations to add
   * @return Id of the created metacard
   * @throws FederationAdminException If the xml string is blank. If an exception is thrown from the
   *     registryTransformer. The CatalogFramework throws an exception trying to add the metacard.
   */
  String addRegistryEntry(String xml, Set<String> destinations) throws FederationAdminException;

  /**
   * Add the given metacard to the registry catalog
   *
   * @param metacard Metacard to be stored
   * @return Id of the created metacard
   * @throws FederationAdminException If the metacard provided was null. The CatalogFramework.create
   *     call throws IngestException or SourceUnavailableException
   */
  String addRegistryEntry(Metacard metacard) throws FederationAdminException;

  /**
   * Add the given metacard to the registry catalog with the provided destinations
   *
   * @param metacard Metacard to be stored
   * @param destinations Set of destinations to add
   * @return Id of the created metacard
   * @throws FederationAdminException If the metacard provided was null. The CatalogFramework.create
   *     call throws IngestException or SourceUnavailableException
   */
  String addRegistryEntry(Metacard metacard, Set<String> destinations)
      throws FederationAdminException;

  /**
   * Add the given metacards to the registry catalog with the provided destinations
   *
   * @param metacards Metacards to be stored
   * @param destinations Set of destinations to add
   * @return List of Id's for the created metacards
   * @throws FederationAdminException If the metacard provided was null. The CatalogFramework.create
   *     call throws IngestException or SourceUnavailableException
   */
  List<String> addRegistryEntries(List<Metacard> metacards, Set<String> destinations)
      throws FederationAdminException;

  /**
   * * Deletes the registry entry from the registry catalog for each of the provided registry ids.
   *
   * @param registryIds The list of registry ids to be deleted.
   * @throws FederationAdminException If the list of ids provided is empty. If IngestException or
   *     Source UnavailableException was thrown by CatalogFramework.delete
   */
  void deleteRegistryEntriesByRegistryIds(List<String> registryIds) throws FederationAdminException;

  /**
   * Deletes the registry entry from the registry catalog for each of the provided registry ids. A
   * set of destinations is sent in the delete request
   *
   * @param registryIds The list of registry ids to be deleted.
   * @param destinations Set of destinations to add
   * @throws FederationAdminException If the list of ids provided is empty. If IngestException or
   *     Source UnavailableException was thrown by CatalogFramework.delete
   */
  void deleteRegistryEntriesByRegistryIds(List<String> registryIds, Set<String> destinations)
      throws FederationAdminException;

  /**
   * * Deletes the registry entry from the registry catalog for each of the provided metacard ids.
   *
   * @param metacardIds The list of metacard ids to be deleted.
   * @throws FederationAdminException If the list of ids provided is empty. If IngestException or
   *     Source UnavailableException was thrown by CatalogFramework.delete
   */
  void deleteRegistryEntriesByMetacardIds(List<String> metacardIds) throws FederationAdminException;

  /**
   * * Deletes the registry entry from the registry catalog for each of the provided metacard ids. A
   * set of destinations is sent in the delete request
   *
   * @param metacardIds The list of metacard ids to be deleted.
   * @param destinations The set of destinations to be added
   * @throws FederationAdminException If the list of ids provided is empty. If IngestException or
   *     Source UnavailableException was thrown by CatalogFramework.delete
   */
  void deleteRegistryEntriesByMetacardIds(List<String> metacardIds, Set<String> destinations)
      throws FederationAdminException;

  /**
   * * Write the provided metacard to the registry catalog. Updating the metacard currently stored.
   *
   * @param metacard Metacard with updates to be stored in the registry catalog
   * @throws FederationAdminException If the provided metacard doesn't have an id. If
   *     CatalogFramework.update call throws IngestException or SourceUnavailableException
   */
  void updateRegistryEntry(Metacard metacard) throws FederationAdminException;

  /**
   * Write the provided metacard to the registry catalog. Updating the metacard currently stored
   * with the provided destinations.
   *
   * @param metacard Metacard with updates to be stored in the registry catalog
   * @param destinations Set of destinations to add
   * @throws FederationAdminException If the provided metacard doesn't have an id. If
   *     CatalogFramework.update call throws IngestException or SourceUnavailableException
   */
  void updateRegistryEntry(Metacard metacard, Set<String> destinations)
      throws FederationAdminException;

  /**
   * Update a registry metacard for the xml string provided. The string will be converted to a
   * registry metacard using the RegistryTransformer.
   *
   * @param xml String representation of a registry package xml
   * @throws FederationAdminException If the provided metacard doesn't have an id. If an exception
   *     is thrown from the RegistryTransformer If CatalogFramework.update call throws
   *     IngestException or SourceUnavailableException
   */
  void updateRegistryEntry(String xml) throws FederationAdminException;

  /**
   * Update a registry metacard for the xml string provided. The string will be converted to a
   * registry metacard using the RegistryTransformer. The metacard will be updated with the provided
   * destinations
   *
   * @param xml String representation of a registry package xml
   * @param destinations Set of destinations to add
   * @throws FederationAdminException If the provided metacard doesn't have an id. If an exception
   *     is thrown from the RegistryTransformer If CatalogFramework.update call throws
   *     IngestException or SourceUnavailableException
   */
  void updateRegistryEntry(String xml, Set<String> destinations) throws FederationAdminException;

  /**
   * Get a list of registry metacards
   *
   * @return List<Metacard>
   * @throws FederationAdminException If any exception was thrown
   */
  List<Metacard> getRegistryMetacards() throws FederationAdminException;

  /**
   * Get a list of internal registry metacards.
   *
   * @return List<Metacard>
   * @throws FederationAdminException If any exception was thrown
   */
  List<Metacard> getInternalRegistryMetacards() throws FederationAdminException;

  /**
   * Gets a list of internal registry metacards with the matching registry-id
   *
   * @param registryId registry-id to match
   * @return List<Metacard>
   * @throws FederationAdminException
   */
  List<Metacard> getInternalRegistryMetacardsByRegistryId(String registryId)
      throws FederationAdminException;

  /**
   * Get a list of registry metacards
   *
   * @param destinations Set of destinations to query from
   * @return List<Metacard>
   * @throws FederationAdminException If any exception was thrown by the call to
   *     CatalogFramework.query()
   */
  List<Metacard> getRegistryMetacards(Set<String> destinations) throws FederationAdminException;

  /**
   * Get a list of local registry metacards
   *
   * @return List<Metacard>
   * @throws FederationAdminException If any exception was thrown by the call to
   *     CatalogFramework.query()
   */
  List<Metacard> getLocalRegistryMetacards() throws FederationAdminException;

  /**
   * Get a list of registry metacards with the provided registry ids. It won't run the query if the
   * list provided is empty.
   *
   * @param ids List of IDs to to get metacards for
   * @return List<Metacard>
   * @throws FederationAdminException If empty list of registry ids is provided If the list of id
   *     filters isn't created If any exception was thrown by the call to CatalogFramework.query()
   */
  List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids)
      throws FederationAdminException;

  /**
   * Get a list of registry metacards with the provided registry ids. It won't run the query if the
   * list provided is empty.
   *
   * @param ids List of IDs to to get metacards for
   * @param includeInternal boolean indicating whether the internal representation metacards should
   *     be included
   * @return List<Metacard>
   * @throws FederationAdminException If empty list of registry ids is provided If the list of id
   *     filters isn't created If any exception was thrown by the call to CatalogFramework.query()
   */
  List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids, boolean includeInternal)
      throws FederationAdminException;

  /**
   * Get a list of local registry metacards with the provided registry ids. It won't run the query
   * if the list provided is empty.
   *
   * @param ids List of IDs to to get metacards for
   * @return List<Metacard>
   * @throws FederationAdminException If empty list of registry ids is provided If the list of id
   *     filters isn't created If any exception was thrown by the call to CatalogFramework.query()
   */
  List<Metacard> getLocalRegistryMetacardsByRegistryIds(List<String> ids)
      throws FederationAdminException;

  /**
   * Get a list of local registry objects
   *
   * @return List<RegistryPackageType>
   * @throws FederationAdminException If an exception is thrown trying to unmarshal the xml
   */
  List<RegistryPackageType> getLocalRegistryObjects() throws FederationAdminException;

  /**
   * Get a list of all registry objects
   *
   * @return List<RegistryPackageType>
   * @throws FederationAdminException If an exception is thrown trying to unmarshal the xml
   */
  List<RegistryPackageType> getRegistryObjects() throws FederationAdminException;

  /**
   * Get a registry object by registry id
   *
   * @return List<RegistryPackageType>
   * @throws FederationAdminException If an exception is thrown trying to unmarshal the xml
   */
  RegistryPackageType getRegistryObjectByRegistryId(String registryId)
      throws FederationAdminException;

  /**
   * Get a registry object by registry id providing a list of source ids to be queried
   *
   * @return List<RegistryPackageType>
   * @throws FederationAdminException If an exception is thrown trying to unmarshal the xml
   */
  RegistryPackageType getRegistryObjectByRegistryId(String registryId, Set<String> sourceIds)
      throws FederationAdminException;

  /**
   * Returns the identity metacard for the local registry node.
   *
   * @return {@link java.util.Optional} containing the Identity {@link Metacard}
   * @throws FederationAdminException if more than one metacard was found
   */
  Optional<Metacard> getLocalRegistryIdentityMetacard() throws FederationAdminException;
}
