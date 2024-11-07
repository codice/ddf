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
package org.codice.ddf.preferences.impl;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import java.util.Collections;
import java.util.List;

public class PreferencesMetacardType extends MetacardTypeImpl {

  public static final String NAME = "ddf.preferences";

  public static final String TAG = "ddf-preferences";

  public static final String USER_ATTRIBUTE = "user";

  public static final List<AttributeDescriptor> DESCRIPTORS;

  static {
    DESCRIPTORS =
        Collections.singletonList(
            new AttributeDescriptorImpl(
                USER_ATTRIBUTE, true, true, false, false, BasicTypes.STRING_TYPE));
  }

  public PreferencesMetacardType() {
    super(NAME, Collections.singletonList(new CoreAttributes()));
    DESCRIPTORS.forEach(this::add);
  }
}
