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
package ddf.catalog.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ddf.catalog.data.impl.BinaryContentImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.activation.MimetypesFileTypeMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryContentImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(BinaryContentImplTest.class);

  File content;

  MimeType mimeType;

  @Before
  public void setUp() {
    content = new File("src/test/resources/data/i4ce.png");
    MimetypesFileTypeMap mimeMapper = new MimetypesFileTypeMap();
    try {
      mimeType = new MimeType(mimeMapper.getContentType(content));
    } catch (MimeTypeParseException e) {
      LOGGER.error("Mime parser Failure", e);
      new Failure(null, e);
    }
  }

  @Test
  public void testBinaryContentImpl() {
    InputStream is;
    try {
      is = new FileInputStream(content);
      BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
      bci.setSize(content.length());
      // assertEquals(is, bci.getInputStream());
      assertEquals(mimeType, bci.getMimeType());
      assertEquals(content.length(), bci.getSize());
      assertNotNull(bci.toString());
    } catch (IOException e) {
      LOGGER.error("IO Failure", e);
      new Failure(null, e);
    }
  }

  @Test
  public void testBinaryContentImplNullInputStream() {
    InputStream is = null;
    BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
    bci.setSize(content.length());
    // assertEquals(is, bci.getInputStream());
    assertEquals(mimeType, bci.getMimeType());
    assertEquals(content.length(), bci.getSize());
  }

  @Test
  public void testBinaryContentImplNullMimeType() {
    InputStream is;
    try {
      is = new FileInputStream(content);
      BinaryContentImpl bci = new BinaryContentImpl(is, null);
      assertEquals(null, bci.getMimeType());
    } catch (IOException e) {
      LOGGER.error("IO Failure", e);
      new Failure(null, e);
    }
  }

  @Test
  public void testBinaryContentImplSizeNotSet() {
    InputStream is;
    try {
      is = new FileInputStream(content);
      BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
      assertEquals(-1, bci.getSize());
    } catch (IOException e) {
      LOGGER.error("IO Failure", e);
      new Failure(null, e);
    }
  }

  @Test
  public void testBinaryContentImplNegativeSize() {
    InputStream is;
    try {
      is = new FileInputStream(content);
      BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
      bci.setSize(-20L);
      assertEquals(-1, bci.getSize());
    } catch (IOException e) {
      LOGGER.error("IO Failure", e);
      new Failure(null, e);
    }
  }

  @Test
  public void testBinaryContentImplByteArrayNotNull() {
    InputStream is;
    try {
      is = new FileInputStream(content);
      BinaryContentImpl bci = new BinaryContentImpl(is, mimeType);
      byte[] contents = bci.getByteArray();
      assertNotNull(contents);
    } catch (IOException e) {
      LOGGER.error("IO Failure", e);
      new Failure(null, e);
    }
  }
}
