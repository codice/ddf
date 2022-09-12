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
package org.codice.ddf.libs.klv;

import static org.codice.ddf.libs.klv.data.Klv.KeyLength;
import static org.codice.ddf.libs.klv.data.Klv.LengthEncoding;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Represents the context used for decoding KLV-encoded data. A {@code KlvContext} specifies the key
 * length and length encoding type for the data elements (the fundamental units of KLV) in the data
 * as well as what data elements can be found within.
 *
 * <p>Basically, this class makes it possible to decode KLV-encoded data with an arbitrary
 * structure. The <strong>value</strong> portion of each KLV set can contain either a raw data value
 * (such as a number or string) or another chain of data elements (sometimes called a <i>local
 * set</i>). So, whatever is decoding it needs to know how to interpret the values of the data
 * elements it finds. This class allows you to provide that information so your KLV can be decoded.
 *
 * <p>In essence, a {@code KlvContext} represents the structure of the encoded KLV data you wish to
 * decode.
 */
public class KlvContext {
  private final KeyLength keyLength;

  private final LengthEncoding lengthEncoding;

  private final Map<String, KlvDataElement> nameToDataElementMap;

  private final Map<String, KlvDataElement> keyToDataElementMap;

  /**
   * Constructs a {@code KlvContext} containing the properties of a specific KLV-encoded data set.
   *
   * @param keyLength the key length of the data elements inside the KLV-encoded data
   * @param lengthEncoding the length encoding method of the data elements inside the KLV-encoded
   *     data
   * @throws IllegalArgumentException if any of the arguments are null
   */
  public KlvContext(final KeyLength keyLength, final LengthEncoding lengthEncoding) {
    this(keyLength, lengthEncoding, new HashSet<>());
  }

  /**
   * Constructs a {@code KlvContext} containing the properties of a specific KLV-encoded data set,
   * including the data elements that could possibly be found within the data.
   *
   * @param keyLength the key length of the data elements inside the KLV-encoded data
   * @param lengthEncoding the length encoding method of the date elements inside the KLV-encoded
   *     data
   * @param dataElements the data elements that could possibly be found inside the data
   * @throws IllegalArgumentException if any of the arguments are null
   */
  public KlvContext(
      final KeyLength keyLength,
      final LengthEncoding lengthEncoding,
      final Collection<? extends KlvDataElement> dataElements) {
    Preconditions.checkArgument(keyLength != null, "Key length cannot be null");
    Preconditions.checkArgument(lengthEncoding != null, "Length encoding cannot be null");
    Preconditions.checkArgument(
        dataElements != null, "The collection of KLVDataElements cannot be null");

    this.keyLength = keyLength;
    this.lengthEncoding = lengthEncoding;

    this.nameToDataElementMap = new HashMap<>();
    this.keyToDataElementMap = new HashMap<>();

    addDataElements(dataElements);
  }

  /**
   * Adds a {@link KlvDataElement} to the {@code KlvContext}.
   *
   * @param dataElement the {@code KlvDataElement} to add
   * @throws IllegalArgumentException if the argument is null
   */
  public void addDataElement(final KlvDataElement dataElement) {
    Preconditions.checkArgument(dataElement != null, "The data element cannot be null.");
    nameToDataElementMap.put(dataElement.getName(), dataElement);
    keyToDataElementMap.put(dataElement.getKeyAsString(), dataElement);
  }

  /**
   * Adds multiple {@link KlvDataElement}s to the {@code KlvContext}.
   *
   * @param dataElements the {@code KlvDataElement}s to add
   * @throws IllegalArgumentException if the argument is null
   */
  public void addDataElements(final Collection<? extends KlvDataElement> dataElements) {
    Preconditions.checkArgument(
        dataElements != null, "The collection of data elements cannot be null.");
    dataElements.forEach(
        dataElement -> {
          nameToDataElementMap.put(dataElement.getName(), dataElement);
          keyToDataElementMap.put(dataElement.getKeyAsString(), dataElement);
        });
  }

  public KeyLength getKeyLength() {
    return keyLength;
  }

  public LengthEncoding getLengthEncoding() {
    return lengthEncoding;
  }

  /**
   * Returns the {@link KlvDataElement}s inside this {@code KlvContext} as a {@link Map}, where the
   * keys are the data elements' names and the values are the data elements.
   *
   * @return a {@code Map} containing the {@code KlvDataElement}s inside this {@code KlvContext}
   */
  public Map<String, KlvDataElement> getDataElements() {
    return nameToDataElementMap;
  }

  /**
   * Determines whether this {@code KlvContext} contains a {@link KlvDataElement} with the given
   * name.
   *
   * @param name the data element's name
   * @return whether this {@code KlvContext} contains a data element with the name {@code name}
   */
  public boolean hasDataElement(final String name) {
    return nameToDataElementMap.containsKey(name);
  }

  /**
   * Returns the {@link KlvDataElement} in this {@code KlvContext} with the given name.
   *
   * @param name the data element's name
   * @return the {@code KlvDataElement} in this {@code KlvContext} with the name {@code name}, or
   *     null if this {@code KlvContext} does not have a {@code KlvDataElement} with the name {@code
   *     name}
   */
  public KlvDataElement getDataElementByName(final String name) {
    return nameToDataElementMap.get(name);
  }

  Map<String, KlvDataElement> getKeyToDataElementMap() {
    return keyToDataElementMap;
  }
}
