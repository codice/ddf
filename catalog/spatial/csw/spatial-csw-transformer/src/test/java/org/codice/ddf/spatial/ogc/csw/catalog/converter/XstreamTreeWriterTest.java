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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import org.junit.Test;

public class XstreamTreeWriterTest {

  @Test
  public void testParentAndChildWithSameAttribute() {

    Path pathA = new Path("/A");
    Path pathB = new Path("/A/B");

    PathTracker pathTracker = mock(PathTracker.class);
    when(pathTracker.getPath()).thenReturn(pathA, pathA, pathB, pathB, pathB, pathA);

    XstreamPathValueTracker xstreamPathValueTracker = new XstreamPathValueTracker();
    xstreamPathValueTracker.add(new Path("/A/@x"), "1");
    xstreamPathValueTracker.add(new Path("/A/B/@x"), "1");

    PathTrackingWriter pathTrackingWriter = mock(PathTrackingWriter.class);

    XstreamTreeWriter xstreamTreeWriter =
        new XstreamTreeWriter(pathTrackingWriter, pathTracker, xstreamPathValueTracker);

    xstreamTreeWriter.startVisit("A");
    xstreamTreeWriter.startVisit("@x");
    xstreamTreeWriter.endVisit("@x");
    xstreamTreeWriter.startVisit("B");
    xstreamTreeWriter.startVisit("@x");
    xstreamTreeWriter.endVisit("@x");
    xstreamTreeWriter.endVisit("B");
    xstreamTreeWriter.endVisit("A");

    verify(pathTrackingWriter, times(2)).addAttribute("x", "1");
  }
}
