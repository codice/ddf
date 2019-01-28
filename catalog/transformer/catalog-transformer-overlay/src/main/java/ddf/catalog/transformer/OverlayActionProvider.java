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
package ddf.catalog.transformer;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayActionProvider implements ActionProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OverlayActionProvider.class);

  private static final String ID = "catalog.data.metacard.map.";

  private static final String DESCRIPTION =
      "Provides a metacard URL that transforms the metacard into a geographically aligned image (suitable for overlaying on a map) via the ";

  private static final String TITLE = "Export as ";

  private static final String OVERLAY_PREFIX = "overlay.";

  private static final Pattern OVERLAY_TRANSFORMER_ID_PATTERN =
      Pattern.compile("^" + Pattern.quote(OVERLAY_PREFIX) + "\\w+");

  private static final String UTF_8 = StandardCharsets.UTF_8.name();

  private final Predicate<Metacard> hasOverlay;

  private final String transformerId;

  public OverlayActionProvider(Predicate<Metacard> hasOverlay, String transformerId) {
    Validate.notNull(hasOverlay, "The overlay predicate cannot be null.");
    Validate.notNull(transformerId, "The transformer ID cannot be null.");
    Validate.isTrue(
        OVERLAY_TRANSFORMER_ID_PATTERN.matcher(transformerId).matches(),
        "The transformer ID must match the pattern " + OVERLAY_TRANSFORMER_ID_PATTERN.pattern());

    this.hasOverlay = hasOverlay;
    this.transformerId = transformerId;
  }

  @Override
  public <T> Action getAction(T subject) {
    if (canHandle(subject)) {
      final Metacard metacard = (Metacard) subject;

      try {
        final String sourceId = URLEncoder.encode(metacard.getSourceId(), UTF_8);
        final String metacardId = URLEncoder.encode(metacard.getId(), UTF_8);
        final String encodedTransformerId = URLEncoder.encode(transformerId, UTF_8);

        final URI uri =
            new URI(
                SystemBaseUrl.EXTERNAL.constructUrl(
                    "/catalog/sources/"
                        + sourceId
                        + "/"
                        + metacardId
                        + "?transform="
                        + encodedTransformerId,
                    true));

        final String overlayName = transformerId.substring(OVERLAY_PREFIX.length());

        return new ActionImpl(
            ID + transformerId,
            TITLE + overlayName + " overlay",
            DESCRIPTION + overlayName + " overlay transformer",
            uri.toURL());
      } catch (URISyntaxException | MalformedURLException | UnsupportedEncodingException e) {
        LOGGER.debug("Error constructing URL", e);
      }
    } else {
      LOGGER.debug("Cannot handle the input [{}]", subject);
    }

    return null;
  }

  @Override
  public String getId() {
    return ID + transformerId;
  }

  private boolean canHandleMetacard(Metacard metacard) {
    final String wkt = metacard.getLocation();
    try {
      final Geometry geometry = GeometryUtils.parseGeometry(wkt);
      return GeometryUtils.canHandleGeometry(geometry) && hasOverlay.test(metacard);
    } catch (CatalogTransformerException e) {
      LOGGER.debug("Cannot parse this metacard's geometry [wkt = {}]", wkt, e);
      return false;
    }
  }

  <T> boolean canHandle(T subject) {
    return subject instanceof Metacard && canHandleMetacard((Metacard) subject);
  }
}
