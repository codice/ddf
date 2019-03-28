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
package org.codice.ddf.dominion.commons.pax.exam.options;

import java.util.HashMap;
import java.util.Map;
import org.codice.dominion.pax.exam.interpolate.PaxExamInterpolator;

/**
 * Provides an extension to PaxExam's KarafDistributionConfigurationFileContentOption which supports
 * adding new claims to the <code>etc/users.attributes</code> file.
 */
@SuppressWarnings("squid:MaximumInheritanceDepth" /* cannot control hierarchy for PaxExam */)
public class UsersAttributesFileClaimPutOption extends UsersAttributesFileContentOption {
  private final String id;

  private final Map<String, Object> claims = new HashMap<>();

  /**
   * Creates a new users.attributes content PaxExam option.
   *
   * @param interpolator the interpolator from which to retrieve Karaf directory locations
   * @param id the user id or external system (supports regex) to add claims for
   */
  public UsersAttributesFileClaimPutOption(PaxExamInterpolator interpolator, String id) {
    super(interpolator);
    this.id = id;
  }

  /**
   * Gets the user id for which to add claims.
   *
   * @return the user id for which to add claims
   */
  @SuppressWarnings("squid:S4144" /* user id can also be a matching external system */)
  public String getUserId() {
    return id;
  }

  /**
   * Gets the external system expression for which to add claims.
   *
   * @return the external system expression for which to add claims
   */
  @SuppressWarnings("squid:S4144" /* user id can also be a matching external system */)
  public String getSystem() {
    return id;
  }

  /**
   * Specifies a claim to be added to the corresponding user or external matching system.
   *
   * @param name the name of the claim
   * @param value the value for the claim
   * @return this for chaining
   */
  public UsersAttributesFileClaimPutOption addClaim(String name, Object value) {
    claims.put(name, value);
    return this;
  }

  @Override
  protected void update(Map<String, Map<String, Object>> claims) {
    final Map<String, Object> current = claims.get(id);

    if (current == null) {
      claims.put(id, this.claims);
    } else {
      current.putAll(this.claims);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{id=" + id + ", claims=" + claims + "}";
  }
}
