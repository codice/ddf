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
package org.codice.ddf.registry.federationadmin.internal;

import java.util.List;
import java.util.Map;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;

/**
 * This is the external facing interface for the FederationAdminService. This is what should be used
 * by the UI, for example, to interact with the FederationAdminService.
 */
public interface FederationAdminMBean {
  String OBJECT_NAME = "org.codice.ddf.registry:type=FederationAdminMBean";

  /**
   * Create a local entry in the registry catalog for the given object map. The map will be
   * converted to a {@link RegistryPackageType} using the {@link RegistryPackageWebConverter}. The
   * resulting {@link RegistryPackageType} will be passed to {@link
   * FederationAdminService#addLocalEntry} to create the entry.
   *
   * @param RegistryObjectMap A map of the registry object to create
   * @return Id of the created metacard.
   * @throws FederationAdminException Passes exception thrown by FederationAdminServiceImpl
   */
  String createLocalEntry(Map<String, Object> registryObjectMap) throws FederationAdminException;

  /**
   * Create a local entry in the registry catalog for the given string. The String provided is an
   * Base64 encoded representation of a registry package xml. The RegistryTransformer transforms the
   * Base64 decode stream into a registry metacard.
   *
   * @param base64EncodedXmlString Base64 encoded registry package xml.
   * @return Id of the created metacard
   * @throws FederationAdminException If Base64 decoding of the string fails If the
   *     RegistryTransformer can't convert the stream into a metacard Passes exceptions thrown by
   *     FederationAdminServiceImpl
   */
  String createLocalEntry(String base64EncodedXmlString) throws FederationAdminException;

  /**
   * Update a local entry in the registry catalog for the given object map. The map will be
   * converted to a {@link RegistryPackageType} using the {@link RegistryPackageWebConverter}. The
   * resulting {@link RegistryPackageType} will be passed to {@link
   * FederationAdminService#addLocalEntry} to update the entry.
   *
   * @param registryObjectMap A map of the registry object to update
   * @throws FederationAdminException Passes exception thrown byFederationAdminServiceImpl
   */
  void updateLocalEntry(Map<String, Object> registryObjectMap) throws FederationAdminException;

  /**
   * Deletes a local entry in the registry catalog for each of the ids provided.
   *
   * @param ids List of registry ids to be deleted
   * @throws FederationAdminException Passes exception thrown byFederationAdminServiceImpl
   */
  void deleteLocalEntry(List<String> ids) throws FederationAdminException;

  /**
   * Returns a Map of {@code RegistryPackageType} objects as converted using {@code
   * RegistryPackageWebConverter} List of node maps representing local registry entries can be found
   * in the returned map using the key 'nodes'. Additional information about the nodes/registry is
   * also included in the map.
   *
   * @return Map<String, Object>
   * @throws FederationAdminException Passes exception thrown byFederationAdminServiceImpl
   */
  Map<String, Object> getLocalNodes() throws FederationAdminException;

  /**
   * @param servicePID - The PID of the registry which will have the status checked
   * @return the status of a single registry as a boolean, true if available, false otherwise
   */
  boolean registryStatus(String servicePID);

  /** @return the list of registry metatypes */
  List<Service> allRegistryInfo();

  /**
   * Returns a Map of {@code RegistryPackageType} objects as converted using {@code
   * RegistryPackageWebConverter} List of node maps representing all registry entries can be found
   * in the returned map using the key 'nodes'. Additional information about the nodes/registry is
   * also included in the map.
   *
   * @return Map<String, Object>
   */
  Map<String, Object> allRegistryMetacards();

  /**
   * Returns a Map of {@code RegistryPackageType} object as converted using {@code
   * RegistryPackageWebConverter} A map of the registry node identified by the given registry id.
   * Additional information about the node is also included in the map.
   *
   * @param registryId The registry id of the node to retrieve.
   * @return Map<String, Object>
   */
  Map<String, Object> registryMetacard(String registryId);

  /**
   * Returns a Map of node maps representing all registry entries which can be found in the returned
   * map using the key 'nodes'. Additional information about the nodes/registry is also included in
   * the map.
   *
   * @return Map<String, Object>
   */
  Map<String, Object> allRegistryMetacardsSummary();

  /**
   * Regenerates registry sources for the given registry-ids
   *
   * @param ids registry-ids to regenerate
   */
  void regenerateRegistrySources(List<String> ids);

  /**
   * Sets the properties associated with the registry filter/policy
   *
   * @param properties
   */
  void nodeFilterProperties(Map<String, Object> properties);
}
