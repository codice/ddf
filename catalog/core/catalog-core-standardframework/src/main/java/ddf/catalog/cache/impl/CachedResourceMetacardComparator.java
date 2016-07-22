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
package ddf.catalog.cache.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Class used to compare two metacards to determine if the resource associated with it has changed
 * and needs to be re-retrieved.
 * <p/>
 * Note: The metacard resource size is not included in the comparison because it cannot
 * be relied upon, i.e., federated sources may not return it, may return an inaccurate value
 * or may return a value in a different unit (kilobytes instead of bytes for instance).
 * <p>
 * Since the {@code MetacardResourceSizePlugin} is used to address this issue by updating the
 * size of the metacard resource after a query is run based on the size of the product in the cache,
 * using the size would cause two problems. First, the resource would be downloaded twice
 * before the size in the cached metacard and the one retrieved become identical, which is not
 * acceptable in low bandwidth environments.
 * <p>
 * Secondly, once the resource has been retrieved twice and the size had become accurate,
 * comparing it becomes meaningless since the {@code MetacardResourceSizePlugin} will always make
 * the size of the freshly retrieved metacard the same as the one in the cache.
 */
class CachedResourceMetacardComparator {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CachedResourceMetacardComparator.class);

    private static final List<Function<Metacard, ?>> METACARD_METHODS =
            ImmutableList.of(Metacard::getModifiedDate,
                    Metacard::getThumbnail,
                    Metacard::getResourceURI,
                    Metacard::getLocation,
                    Metacard::getEffectiveDate,
                    Metacard::getCreatedDate,
                    Metacard::getExpirationDate,
                    Metacard::getContentTypeName);

    private static final Set<String> ATTRIBUTES_TO_IGNORE = ImmutableSet.of(Metacard.CHECKSUM,
            Metacard.MODIFIED,
            Metacard.RESOURCE_SIZE,
            Metacard.THUMBNAIL,
            Metacard.RESOURCE_URI,
            Metacard.GEOGRAPHY,
            Metacard.EFFECTIVE,
            Metacard.CREATED,
            Metacard.EXPIRATION,
            Metacard.CONTENT_TYPE);

    private CachedResourceMetacardComparator() {
    }

    /**
     * Indicates whether the resource associated with the provided metacard has changed by
     * comparing the state of the metacard when it was cached with the state of the metacard when
     * it was last retrieved. Since there is no single attribute that clearly indicates whether
     * the resource has changed or not, this class compares as many attributes as possible and if
     * it find a difference, it will return {@code false}.
     *
     * @param cachedMetacard  version of the metacard when the product was added to the cache
     * @param updatedMetacard current version of the metacard
     * @return {@code true} only if the attributes in the updated metacard do not indicate any
     * potential changes in the resource associated with it
     */
    public static boolean isSame(@Nullable Metacard cachedMetacard,
            @Nullable Metacard updatedMetacard) {

        if (cachedMetacard == null && updatedMetacard == null) {
            return true;
        }

        if (cachedMetacard == null || updatedMetacard == null) {
            return false;
        }

        if (!Objects.equals(cachedMetacard.getId(), updatedMetacard.getId())) {
            return false;
        }

        if (!Objects.equals(cachedMetacard.getAttribute(Metacard.CHECKSUM),
                updatedMetacard.getAttribute(Metacard.CHECKSUM))) {
            return false;
        }

        if (!allMetacardMethodsReturnMatchingAttributes(cachedMetacard, updatedMetacard)) {
            return false;
        }

        return allMetacardAttributesEqual(cachedMetacard, updatedMetacard);
    }

    private static boolean allMetacardMethodsReturnMatchingAttributes(Metacard cachedMetacard,
            Metacard updatedMetacard) {
        Optional<Function<Metacard, ?>> difference = METACARD_METHODS.stream()
                .filter(method -> !Objects.deepEquals(method.apply(cachedMetacard),
                        method.apply(updatedMetacard)))
                .findFirst();

        if (LOGGER.isDebugEnabled() && difference.isPresent()) {
            Function<Metacard, ?> metacardFunction = difference.get();
            LOGGER.debug("Metacard updated. Cached value: {}. Updated value: {}",
                    metacardFunction.apply(cachedMetacard),
                    metacardFunction.apply(updatedMetacard));
        }

        return !difference.isPresent();
    }

    private static boolean allMetacardAttributesEqual(Metacard cachedMetacard,
            Metacard updatedMetacard) {
        MetacardType cachedMetacardType = cachedMetacard.getMetacardType();
        MetacardType updatedMetacardType = updatedMetacard.getMetacardType();

        if (!Objects.equals(cachedMetacardType, updatedMetacardType)) {
            return false;
        }

        // We know cacheMetacardType and updateMetacardType are equal at this point, so if
        // cachedMetacardType is null then updatedMetacardType has to be null as well and no
        // attributes need to be compared so we can return true.
        if (cachedMetacardType == null) {
            return true;
        }

        Set<AttributeDescriptor> cachedDescriptors = cachedMetacardType.getAttributeDescriptors();

        // Since the Objects.equals() above does a deep compare, we know that the two descriptor
        // sets are equals. If cacheDescriptors is null, then updatedDescriptors has to be null
        // as well and no attributes need to be compared so we can return true.
        if (cachedDescriptors == null) {
            return true;
        }

        Optional<String> difference = cachedDescriptors.stream()
                .map(AttributeDescriptor::getName)
                .filter(attributeName -> !ATTRIBUTES_TO_IGNORE.contains(attributeName))
                .filter(attributeName -> !Objects.equals(cachedMetacard.getAttribute(attributeName),
                        updatedMetacard.getAttribute(attributeName)))
                .findFirst();

        if (LOGGER.isDebugEnabled() && difference.isPresent()) {
            String attributeName = difference.get();
            LOGGER.debug(
                    "Metacard updated. Attribute changed: {}, cached value: {}, updated value: {}",
                    attributeName,
                    cachedMetacard.getAttribute(attributeName),
                    updatedMetacard.getAttribute(attributeName));
        }

        return !difference.isPresent();
    }
}
