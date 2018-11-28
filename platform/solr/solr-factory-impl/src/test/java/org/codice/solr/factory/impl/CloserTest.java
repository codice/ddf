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
package org.codice.solr.factory.impl;

import java.io.Closeable;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class CloserTest {
  private final Closeable closeable1 = Mockito.mock(Closeable.class);
  private final Closeable closeable2 = Mockito.mock(Closeable.class);
  private final Closeable closeable3 = Mockito.mock(Closeable.class);

  private final Closer closer = new Closer();

  @Test
  public void testWithCloseable() throws Exception {
    Assert.assertThat(closer.with(closeable1), Matchers.sameInstance(closeable1));

    closer.close();

    Mockito.verify(closeable1).close();
  }

  @Test
  public void testWithNull() throws Exception {
    closer.with(closeable1);

    Assert.assertThat(closer.with(null), Matchers.nullValue());

    closer.close();

    Mockito.verify(closeable1).close();
  }

  @Test
  public void testCloseWhenReturning() throws Exception {
    final Object result = new Object();

    closer.with(closeable1);
    closer.with(closeable2);
    closer.with(closeable3);

    Assert.assertThat(closer.returning(result), Matchers.sameInstance(result));

    Mockito.verify(closeable1, Mockito.never()).close();
    Mockito.verify(closeable2, Mockito.never()).close();
    Mockito.verify(closeable3, Mockito.never()).close();
  }

  @Test
  public void testCloseWhenNotReturning() throws Exception {
    closer.with(closeable1);
    closer.with(closeable2);
    closer.with(closeable3);

    closer.close();

    final InOrder inorder = Mockito.inOrder(closeable1, closeable2, closeable3);

    inorder.verify(closeable3).close();
    inorder.verify(closeable2).close();
    inorder.verify(closeable1).close();
  }

  @Test
  public void testCloseWhenEmpty() throws Exception {
    closer.close();
  }

  @Test
  public void testCloseWhenCloseableThrowsException() throws Exception {
    closer.with(closeable1);
    closer.with(closeable2);
    closer.with(closeable3);

    Mockito.doThrow(new IOException()).when(closeable2).close();

    closer.close();

    final InOrder inorder = Mockito.inOrder(closeable1, closeable2, closeable3);

    inorder.verify(closeable3).close();
    inorder.verify(closeable2).close();
    inorder.verify(closeable1).close();
  }
}
