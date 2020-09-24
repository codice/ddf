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
package org.codice.ddf.checksum;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.codice.ddf.checksum.impl.Sha256ChecksumProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Sha256ChecksumProviderTest {

  private ChecksumProvider checksumProvider;

  @Before
  public void intialize() {
    checksumProvider = new Sha256ChecksumProvider();
  }

  @Test
  public void testCalculateChecksumString() throws IOException, NoSuchAlgorithmException {
    String testString = "Hello World";

    InputStream stringInputStream = getInputStreamFromObject(testString);
    String checksumValue = checksumProvider.calculateChecksum(stringInputStream);

    // compare returned checksum to previous checksum
    // as they should be the same if the checksum is calculated
    // correctly
    assertThat(
        checksumValue, is("cade3dbf41da60f7154ef628446ff163700b6a9860cd951de8e4321b2189de6d"));
  }

  @Test
  public void testCalculateChecksumLargeString() throws IOException, NoSuchAlgorithmException {

    final char[] chars = new char[1024 * 10000];

    Arrays.fill(chars, 'a');
    String checksumValue = checksumProvider.calculateChecksum(getInputStreamFromObject(chars));

    assertThat(
        checksumValue, is("a7efde5c5b4772b468c63aa9d379531a2b21bf8e4fe0c0f551a626e1dd5aea3f"));
  }

  @Test
  public void testCalculateChecksumObject() throws IOException, NoSuchAlgorithmException {

    SerializableTestObject obj = new SerializableTestObject();
    obj.setName("Test Name");
    obj.setDescription("Test Description");

    InputStream stringInputStream = getInputStreamFromObject(obj);
    String checksumValue = checksumProvider.calculateChecksum(stringInputStream);

    // compare returned checksum to previous checksum
    // as they should be the same if the checksum is calculated
    // correctly
    Assert.assertThat(
        checksumValue, is("7b42b8b57b09e1f451a6cf6b63b35724520f46f7f927dbae7f6ab209128800ff"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCalculateChecksumWithNullInputStream()
      throws IOException, NoSuchAlgorithmException {

    checksumProvider.calculateChecksum(null);
  }

  @Test
  public void testGetChecksumAlgorithm() {

    String expectedAlgorithm = "SHA-256";
    String checksumAlgorithm = checksumProvider.getChecksumAlgorithm();

    // we want to make sure that the checksum algorithm
    // returned does not change in the event there exists
    // checks against the 'Adler32'
    assertThat(expectedAlgorithm, is(checksumAlgorithm));
  }

  private InputStream getInputStreamFromObject(Object obj) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objOutputStream.writeObject(obj);
    objOutputStream.flush();
    objOutputStream.close();

    InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    return inputStream;
  }
}
