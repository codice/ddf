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
package org.codice.ddf.platform.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.transform.ErrorListener;

/** Properties for transforming XML */
public class TransformerProperties {
  private Map<String, String> transformProperties;

  private ErrorListener errorListener;

  public TransformerProperties() {
    transformProperties = new HashMap<>();
  }

  public void addOutputProperty(String propertyType, String propertyValue) {
    transformProperties.put(propertyType, propertyValue);
  }

  public void setErrorListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  public Set<Entry<String, String>> getOutputProperties() {
    return transformProperties.entrySet();
  }

  public ErrorListener getErrorListener() {
    return errorListener;
  }
}
