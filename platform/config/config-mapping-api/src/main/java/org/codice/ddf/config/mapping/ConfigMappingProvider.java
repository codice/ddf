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
package org.codice.ddf.config.mapping;

import java.util.Map;
import org.codice.ddf.config.ConfigService;

/**
 * Provides access to mapped configuration properties for either a configuration mapping or for a
 * given instance of a configuration mapping.
 *
 * <p>The {@linkConfigMappingService} will apply priority on a per property-basis which would allow
 * additional registered providers to override some but not all properties. Comparison of providers
 * is based on ranking. A provider with a ranking of {@link Integer#MAX_VALUE} will have its mapped
 * properties override all others, whereas a service with a ranking of {@link Integer#MIN_VALUE} is
 * very likely to have its mapped properties overridden. When a tie in ranking exist, whichever
 * provider was first bound to the {@link ConfigMappingService} directly will be considered to have
 * a higher priority.
 */
public interface ConfigMappingProvider extends Comparable<ConfigMappingProvider> {
  /**
   * Gets a ranking priority for this provider (see class description for more details).
   *
   * <p><i>Note:</i> The provider's rank is not expected to change during the life of this provider
   * unless the provider is rebound with the {@link ConfigMappingService} which will re-compute
   * which config mappings are impacted by this change.
   *
   * @return this provider's ranking priority (defaults to 0)
   */
  public default int getRanking() {
    return 0;
  };

  /**
   * Indicates whether this provider is only capable of providing partial properties or if it can
   * provide all properties.
   *
   * <p>At least one provider capable of providing all properties and not just partial one is
   * required to be bound to the mapping service before a corresponding mapping can be made
   * available.
   *
   * @return <code>true</code> if this provider is only capable of providing parts of all the
   *     properties for this mapping; <code>false</code> if it is able to provide all of them
   */
  public boolean isPartial();

  /**
   * Checks if this provider can provide mapped dictionaries for a given configuration mapping.
   *
   * <p><i>Note:</i> A provider is expected not to change which configuration mappings it provides
   * for unless the provider is rebound with the {@link ConfigMappingService} which will re-compute
   * which config mappings are impacted by this change.
   *
   * @param mapping the config mapping to check if this provider can provide for
   * @return <code>true</code> if this provider can provide mapped dictionaries for the specified
   *     config mapping; <code>false</code> otherwise
   */
  public default boolean canProvideFor(ConfigMapping mapping) {
    return canProvideFor(mapping.getId());
  }

  /**
   * Checks if this provider can provide mapped dictionaries for a given configuration mapping.
   *
   * <p><i>Note:</i> A provider is expected not to change which configuration mappings it provides
   * for unless the provider is rebound with the {@link ConfigMappingService} which will re-compute
   * which config mappings are impacted by this change.
   *
   * @param id the id of the config mapping to check if this provider can provide for
   * @return <code>true</code> if this provider can provide mapped dictionaries for the specified
   *     config mapping; <code>false</code> otherwise
   */
  public boolean canProvideFor(ConfigMapping.Id id);

  /**
   * Provides the mapped dictionary for a given configuration mapping.
   *
   * @param id the unique config mapping id for the mapped properties to provide
   * @param config the configuration to use when computing the mapper properties
   * @return a map corresponding to all mapped properties and their current values after
   *     re-evaluating any internal rules defined
   * @throws ConfigMappingException if a failure occurred while resolving this config mapping
   */
  public Map<String, Object> provide(ConfigMapping.Id id, ConfigService config)
      throws ConfigMappingException;

  /**
   * {@inheritDoce}
   *
   * <p>The comparison must support the ranking priority described in the class description.
   *
   * @param provider the other provider to compare this provider with
   * @return <code>1</code> if this provider has a higher ranking priority than the one provided;
   *     <code>-1</code> if it has a lower ranking priority and <code>0</code> if they have equal
   *     priorities
   */
  @Override
  int compareTo(ConfigMappingProvider provider);
}
