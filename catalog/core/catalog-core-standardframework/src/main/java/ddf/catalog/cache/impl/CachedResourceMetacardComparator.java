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
package ddf.catalog.cache.impl;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to compare two metacards to determine if the resource associated with it has changed
 * and needs to be re-retrieved.
 *
 * <p>Note: The metacard resource size is not included in the comparison because it cannot be relied
 * upon, i.e., federated sources may not return it, may return an inaccurate value or may return a
 * value in a different unit (kilobytes instead of bytes for instance).
 *
 * <p>Since the {@code MetacardResourceSizePlugin} is used to address this issue by updating the
 * size of the metacard resource after a query is run based on the size of the product in the cache,
 * using the size would cause two problems. First, the resource would be downloaded twice before the
 * size in the cached metacard and the one retrieved become identical, which is not acceptable in
 * low bandwidth environments.
 *
 * <p>Secondly, once the resource has been retrieved twice and the size had become accurate,
 * comparing it becomes meaningless since the {@code MetacardResourceSizePlugin} will always make
 * the size of the freshly retrieved metacard the same as the one in the cache.
 */
class CachedResourceMetacardComparator {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(CachedResourceMetacardComparator.class);

  private static final List<Function<Metacard, ?>> METACARD_METHODS =
      ImmutableList.of(Metacard::getModifiedDate, Metacard::getCreatedDate);

  private CachedResourceMetacardComparator() {}

  /**
   * Indicates whether the resource associated with the provided metacard has changed by comparing
   * the state of the metacard when it was cached with the state of the metacard when it was last
   * retrieved. Since there is no single attribute that clearly indicates whether the resource has
   * changed or not, this class compares as many attributes as possible and if it find a difference,
   * it will return {@code false}.
   *
   * @param cachedMetacard version of the metacard when the product was added to the cache
   * @param updatedMetacard current version of the metacard
   * @return {@code true} only if the attributes in the updated metacard do not indicate any
   *     potential changes in the resource associated with it
   */
  public static boolean isSame(
      @Nullable Metacard cachedMetacard, @Nullable Metacard updatedMetacard) {

    if (cachedMetacard == null && updatedMetacard == null) {
      return true;
    }

    if (cachedMetacard == null || updatedMetacard == null) {
      return false;
    }

    if (!Objects.equals(cachedMetacard.getId(), updatedMetacard.getId())) {
      return false;
    }

    if (!Objects.equals(
        cachedMetacard.getAttribute(Metacard.CHECKSUM),
        updatedMetacard.getAttribute(Metacard.CHECKSUM))) {
      return false;
    }

    return allMetacardMethodsReturnMatchingAttributes(cachedMetacard, updatedMetacard);
  }

  private static boolean allMetacardMethodsReturnMatchingAttributes(
      Metacard cachedMetacard, Metacard updatedMetacard) {
    Optional<Function<Metacard, ?>> difference =
        METACARD_METHODS
            .stream()
            .filter(
                method ->
                    !Objects.deepEquals(
                        method.apply(cachedMetacard), method.apply(updatedMetacard)))
            .findFirst();

    if (LOGGER.isDebugEnabled() && difference.isPresent()) {
      Function<Metacard, ?> metacardFunction = difference.get();
      LOGGER.debug(
          "Metacard updated. Cached value: {}. Updated value: {}",
          metacardFunction.apply(cachedMetacard),
          metacardFunction.apply(updatedMetacard));
    }

    return !difference.isPresent();
  }
}
