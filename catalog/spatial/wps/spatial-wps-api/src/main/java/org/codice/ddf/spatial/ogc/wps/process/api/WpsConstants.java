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
package org.codice.ddf.spatial.ogc.wps.process.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class WpsConstants {
  public static final String CAPABILITY_CONTENTS = "Contents";

  public static final String CAPABILITY_OPS_METADATA = "OperationsMetadata";

  public static final String CAPABILITY_SERVICE_ID = "ServiceIdentification";

  public static final String CAPABILITY_SERVICE_PROVIDER = "ServiceProvider";

  public static final List<String> GET_CAPABILITIES_SECTIONS =
      Collections.unmodifiableList(
          Arrays.asList(
              CAPABILITY_CONTENTS,
              CAPABILITY_OPS_METADATA,
              CAPABILITY_SERVICE_ID,
              CAPABILITY_SERVICE_PROVIDER));

  private WpsConstants() {}
}
