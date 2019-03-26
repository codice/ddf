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
package ddf.catalog.plugin.groomer.metacard;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.groomer.AbstractMetacardGroomerPlugin;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.Map.Entry;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies general Create and Update grooming rules such as populating the {@link Core#ID}, {@link
 * Core#MODIFIED}, and {@link Core#CREATED} fields.
 */
public class StandardMetacardGroomerPlugin extends AbstractMetacardGroomerPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(StandardMetacardGroomerPlugin.class);

  private UuidGenerator uuidGenerator;

  public void setUuidGenerator(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  protected void applyCreatedOperationRules(
      CreateRequest createRequest, Metacard aMetacard, Date now) {
    LOGGER.debug("Applying standard rules on CreateRequest");
    if ((aMetacard.getResourceURI() != null && !isCatalogResourceUri(aMetacard.getResourceURI()))
        || !uuidGenerator.validateUuid(aMetacard.getId())) {
      aMetacard.setAttribute(new AttributeImpl(Core.ID, uuidGenerator.generateUuid()));
    }

    if (aMetacard.getAttribute(Core.CREATED).getValue() == null) {
      aMetacard.setAttribute(new AttributeImpl(Core.CREATED, now));
    }

    if (aMetacard.getAttribute(Core.MODIFIED).getValue() == null) {
      aMetacard.setAttribute(new AttributeImpl(Core.MODIFIED, now));
    }

    if (aMetacard.getEffectiveDate() == null) {
      aMetacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, now));
    }

    if (isDateAttributeEmpty(aMetacard, Core.METACARD_CREATED)) {
      aMetacard.setAttribute(new AttributeImpl(Core.METACARD_CREATED, now));
      logMetacardAttributeUpdate(aMetacard, Core.METACARD_CREATED, now);
    }

    aMetacard.setAttribute(new AttributeImpl(Core.METACARD_MODIFIED, now));
    logMetacardAttributeUpdate(aMetacard, Core.METACARD_MODIFIED, now);
  }

  private boolean isCatalogResourceUri(URI uri) {
    return uri != null && ContentItem.CONTENT_SCHEME.equals(uri.getScheme());
  }

  protected void applyUpdateOperationRules(
      UpdateRequest updateRequest,
      Entry<Serializable, Metacard> anUpdate,
      Metacard aMetacard,
      Date now) {

    if (UpdateRequest.UPDATE_BY_ID.equals(updateRequest.getAttributeName())
        && !anUpdate.getKey().toString().equals(aMetacard.getId())) {

      LOGGER.debug(
          "{} in metacard must match the Update {}, overwriting metacard {} [{}] with the update identifier [{}]",
          Core.ID,
          Core.ID,
          Core.ID,
          aMetacard.getId(),
          anUpdate.getKey());
      aMetacard.setAttribute(new AttributeImpl(Core.ID, anUpdate.getKey()));
    }

    if (aMetacard.getAttribute(Core.CREATED).getValue() == null) {
      LOGGER.debug(
          "{} date should match the original metacard. Changing date to current timestamp so it is at least not null.",
          Core.CREATED);
      aMetacard.setAttribute(new AttributeImpl(Core.CREATED, now));
    }

    if (isDateAttributeEmpty(aMetacard, Core.METACARD_CREATED)) {
      aMetacard.setAttribute(new AttributeImpl(Core.METACARD_CREATED, now));
      LOGGER.debug(
          "{} date should not be null on an update operation. Changing date to current timestamp so it is at least not null.",
          Core.METACARD_CREATED);
    }

    if (aMetacard.getAttribute(Core.MODIFIED).getValue() == null) {
      aMetacard.setAttribute(new AttributeImpl(Core.MODIFIED, now));
    }

    if (aMetacard.getEffectiveDate() == null) {
      aMetacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, now));
    }

    // upon an update operation, the metacard modified time should be updated
    aMetacard.setAttribute(new AttributeImpl(Core.METACARD_MODIFIED, now));
    logMetacardAttributeUpdate(aMetacard, Core.METACARD_MODIFIED, now);
  }

  private void logMetacardAttributeUpdate(Metacard metacard, String attribute, Object value) {
    LOGGER.debug(
        "Applying {} attribute with value {} to metacard [{}].",
        attribute,
        value,
        metacard.getId());
  }

  private boolean isDateAttributeEmpty(Metacard metacard, String attribute) {
    Attribute origAttribute = metacard.getAttribute(attribute);
    return (origAttribute == null || !(origAttribute.getValue() instanceof Date));
  }
}
