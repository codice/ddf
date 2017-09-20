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
package org.codice.ddf.libs.klv.data.set;

import org.codice.ddf.libs.klv.KlvContext;
import org.codice.ddf.libs.klv.KlvDataElement;
import org.codice.ddf.libs.klv.KlvDecoder;
import org.codice.ddf.libs.klv.KlvDecodingException;
import org.codice.ddf.libs.klv.data.Klv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents a KLV element whose value is a chain of KLV data elements. */
public class KlvLocalSet extends KlvDataElement<KlvContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(KlvLocalSet.class);

  private final KlvContext localSetKlvContext;

  /**
   * Constructs a {@code KlvLocalSet} whose value is a chain of KLV data elements.
   *
   * @param key the data element's key
   * @param name a name describing the data element's value
   * @param localSetKlvContext the {@link KlvContext} describing the local KLV set
   * @throws IllegalArgumentException if any of the arguments are null
   */
  public KlvLocalSet(final byte[] key, final String name, final KlvContext localSetKlvContext) {
    super(key, name);
    this.localSetKlvContext = localSetKlvContext;
  }

  @Override
  protected void decodeValue(final Klv klv) {
    try {
      value = new KlvDecoder(localSetKlvContext).decode(klv.getValue());
    } catch (KlvDecodingException e) {
      LOGGER.debug("Couldn't decode the KLV local set named {}", name, e);
    }
  }

  @Override
  protected KlvDataElement copy() {
    // Shallow copying the local KlvContext is okay because when the local set is decoded,
    // the KLVDataElements inside it will be copied into a new KlvContext by the KlvDecoder.
    return new KlvLocalSet(keyBytes, name, localSetKlvContext);
  }
}
