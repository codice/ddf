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
package org.codice.ddf.catalog.plugin.expiration;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pre-Ingest Plugin to overwrite Metacard expiration dates. */
public class ExpirationDatePlugin implements PreIngestPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpirationDatePlugin.class);

  private int offsetFromCreatedDate;

  private boolean overwriteIfBlank;

  private boolean overwriteIfExists;

  /** Modify metacard expiration dates (pre-ingest). */
  @Override
  public CreateRequest process(CreateRequest createRequest)
      throws PluginExecutionException, StopProcessingException {

    LOGGER.debug(
        "START Pre-Ingest Plugin: {}.process(CreateRequest createRequest)",
        this.getClass().getName());

    if (createRequest == null
        || createRequest.getMetacards() == null
        || createRequest.getMetacards().isEmpty()) {
      throw new PluginExecutionException("No metacards to validate or CreateRequest is null");
    }

    List<Metacard> metacards = createRequest.getMetacards();

    for (Metacard metacard : metacards) {
      updateExpirationDate(metacard);
    }

    LOGGER.debug(
        "END Pre-Ingest Plugin: {}.process(CreateRequest createRequest)",
        this.getClass().getName());
    return createRequest;
  }

  @Override
  public UpdateRequest process(UpdateRequest updateRequest)
      throws PluginExecutionException, StopProcessingException {
    return updateRequest;
  }

  @Override
  public DeleteRequest process(DeleteRequest deleteRequest)
      throws PluginExecutionException, StopProcessingException {
    return deleteRequest;
  }

  public void setOffsetFromCreatedDate(int days) {
    LOGGER.debug("Setting offset from created date to: {}", days);
    this.offsetFromCreatedDate = days;
  }

  public void setOverwriteIfBlank(boolean overwriteIfBlank) {
    LOGGER.debug("Setting overwrite if blank to: {}", overwriteIfBlank);
    this.overwriteIfBlank = overwriteIfBlank;
  }

  public void setOverwriteIfExists(boolean overwriteIfExists) {
    LOGGER.debug("Setting overwrite if exists to: {}", overwriteIfExists);
    this.overwriteIfExists = overwriteIfExists;
  }

  /**
   * Updates a metacard's expiration date.
   *
   * @param metacard the metacard to update.
   */
  private void updateExpirationDate(Metacard metacard) {

    Date currentExpirationDate = metacard.getExpirationDate();

    if (currentExpirationDate == null && !overwriteIfBlank) {
      // Don't overwrite empty expiration date if configuration disallows
      LOGGER.debug(
          "The Expiration Date Pre-Ingest Plugin is not configured to overwrite 'empty' expiration dates. Not overwriting null expiration date for metacard ID [{}]. ",
          metacard.getId());
      return;

    } else if (currentExpirationDate != null && !overwriteIfExists) {
      // Don't overwrite existing expiration date if configuration disallows
      LOGGER.debug(
          "The Expiration Date Pre-Ingest Plugin is not configured to overwrite 'existing' expiration dates. Not overwriting the existing expiration date of {} for metacard ID [{}]. ",
          currentExpirationDate,
          metacard.getId());
      return;
    }

    Date metacardCreatedDate = getMetacardCreatedDate(metacard);
    Date newExpirationDate = calculateNewExpirationDate(metacardCreatedDate);

    LOGGER.debug(
        "Metacard ID [{}] has an expiration date of {}. Calculating new expiration date by adding {} day(s) to the created date of {}. The new expiration date is {}.",
        metacard.getId(),
        currentExpirationDate,
        this.offsetFromCreatedDate,
        metacardCreatedDate,
        newExpirationDate);

    Attribute expirationDate = new AttributeImpl(Core.EXPIRATION, newExpirationDate);
    metacard.setAttribute(expirationDate);
  }

  /**
   * Calculates a new expiration date by adding offsetFromCreatedDate day(s) to the metacard's
   * modified date.
   *
   * @param offsetableDate the date that the metacard expiration date will be calculated against.
   * @return the new expiration date.
   */
  private Date calculateNewExpirationDate(Date offsetableDate) {
    Instant modified = offsetableDate.toInstant();
    Instant newExpiration = modified.plus(offsetFromCreatedDate, ChronoUnit.DAYS);
    return Date.from(newExpiration);
  }

  private Date getMetacardCreatedDate(Metacard metacard) {

    // initialize creation to now in case metacard groomer plugin failed to set creation date
    // or parsing fails
    Date createdDate = new Date();

    Attribute metacardCreatedAttribute = metacard.getAttribute(Core.METACARD_CREATED);
    if (metacardCreatedAttribute != null && (metacardCreatedAttribute.getValue() instanceof Date)) {
      createdDate = (Date) metacardCreatedAttribute.getValue();
    }

    return createdDate;
  }
}
