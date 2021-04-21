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

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingReader;
import java.util.LinkedHashSet;
import org.apache.commons.lang.StringUtils;

public class XstreamPathConverter implements Converter {

  public static final String PATH_KEY = "PATHS";

  private static final char TAG_BEGIN = '[';
  private static final char TAG_END = ']';
  private static final char ATTR_TAG = '@';
  private static final char EVERYTHING_TAG = '*';
  private static final char PATH_SEPARATOR = '/';

  @Override
  public void marshal(
      Object o,
      HierarchicalStreamWriter hierarchicalStreamWriter,
      MarshallingContext marshallingContext) {
    // Does nothing as of now
  }

  /**
   * @param reader
   * @param context
   * @return {@link XstreamPathValueTracker}
   * @throws ConversionException
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
      throws ConversionException {

    XstreamPathValueTracker pathValueTracker = new XstreamPathValueTracker();
    pathValueTracker.buildPaths((LinkedHashSet<Path>) context.get(PATH_KEY));

    PathTracker tracker = new PathTracker();
    PathTrackingReader pathReader = new PathTrackingReader(reader, tracker);

    readPath(pathReader, tracker, pathValueTracker, false);
    return pathValueTracker;
  }

  /**
   * Reads through the tree looking for a specific path and returns the value at that node
   *
   * <p>The reader is moved to the next node in the path
   *
   * <p>For example, if readPath(reader, "a", "b", "c") is called, then the value at /a/b/c is
   * returned and the reader is advanced to "<d>" {code} <a> <-- reader starts here <b> <c>value</c>
   * </b> </a> <d></d> {code}
   *
   * @param reader
   * @param tracker
   * @param pathValueTracker
   */
  protected void readPath(
      PathTrackingReader reader,
      PathTracker tracker,
      XstreamPathValueTracker pathValueTracker,
      boolean endElement) {

    pathValueTracker
        .getPaths()
        .forEach(path -> updatePath(reader, path, tracker.getPath(), pathValueTracker, endElement));

    while (reader.hasMoreChildren()) {
      reader.moveDown();

      readPath(reader, tracker, pathValueTracker, false);
      reader.moveUp();
      readPath(reader, tracker, pathValueTracker, true);
    }
  }

  protected void updatePath(
      PathTrackingReader reader,
      Path path,
      Path currentPath,
      XstreamPathValueTracker pathValueTracker,
      boolean endElement) {
    if (doBasicPathsMatch(path, currentPath)) {

      if (!endElement && path.toString().contains("@")) {
        String attributeName = StringUtils.substringAfterLast(path.toString(), "@");
        pathValueTracker.add(path, reader.getAttribute(attributeName));

      } else {
        pathValueTracker.add(path, reader.getValue());
      }
    }
  }

  /**
   * This method uses a streaming-like approach to compare 2 paths with a single iteration. The
   * comparison excludes count indexes in the path as well as the value of attributes in determining
   * equivalence. For the purposes of this method, "/a/b/c" matches "/a/b[2]/c/@attr". Also checks
   * that "a/b/c/*" will match "/a/b/c/d".
   *
   * @param pathObj1 The first path
   * @param pathObj2 The second path
   * @return If the paths match
   */
  protected boolean doBasicPathsMatch(final Path pathObj1, final Path pathObj2) {
    if (pathObj1.equals(pathObj2)) {
      return true;
    }

    String path1Str = pathObj1.toString();
    String path2Str = pathObj2.toString();
    char[] path1 = path1Str.toCharArray();
    char[] path2 = path2Str.toCharArray();
    int i, j;

    for (i = 0, j = 0; i < path1.length && j < path2.length; i++, j++) {
      i = countPastTag(path1, i);
      j = countPastTag(path2, j);
      if (i < path1.length && j < path2.length) {
        if ((path1[i] == ATTR_TAG && path2[j] == ATTR_TAG)
            || (path1[i] == EVERYTHING_TAG && path2Str.startsWith(path1Str.substring(0, i - 2)))) {
          return true;
        } else if (path1[i] != path2[j]) {
          return false;
        }
      } else {
        break;
      }
    }
    i = countPastTag(path1, i);
    j = countPastTag(path2, j);
    return (i == path1.length && j == path2.length) || endsMatch(path1, i, path2, j);
  }

  /**
   * Checks if 2 paths who have matched up to the provided indices, with 1 index being at or beyond
   * the end of the path, match by accounting for attribute value presence. This finishes the
   * comparison of 2 paths once one path has been traversed.
   *
   * @param path1 The first path
   * @param index1 The current index in path1
   * @param path2 The second path
   * @param index2 The current index in path2
   * @return If the path ends match
   */
  private boolean endsMatch(char[] path1, int index1, char[] path2, int index2) {
    if (index1 >= path1.length && index2 < path2.length - 1) {
      return path2[index2] == PATH_SEPARATOR
          && (path2[index2 + 1] == ATTR_TAG || path2[index2 + 1] == EVERYTHING_TAG);
    } else if (index2 >= path2.length && index1 < path1.length - 1) {
      return path1[index1] == PATH_SEPARATOR
          && (path1[index1 + 1] == ATTR_TAG || path1[index1 + 1] == EVERYTHING_TAG);
    }
    return false;
  }

  /**
   * If a count tag is present in the path at index, this method will continue beyond the tag in the
   * path and return the next index to be considered for matching purposes. This method returns the
   * initial index if the character at that index is not a count tag start.
   *
   * @param path The path
   * @param index The index to check for a tag
   * @return The index at which to resume comparison
   */
  private int countPastTag(char[] path, int index) {
    if (index < path.length && path[index] == TAG_BEGIN) {
      while (index < path.length && path[index] != TAG_END) {
        index++;
      }
      if (index < path.length) {
        index++;
      }
    }
    return index;
  }

  @Override
  public boolean canConvert(Class clazz) {
    return XstreamPathValueTracker.class.isAssignableFrom(clazz);
  }
}
