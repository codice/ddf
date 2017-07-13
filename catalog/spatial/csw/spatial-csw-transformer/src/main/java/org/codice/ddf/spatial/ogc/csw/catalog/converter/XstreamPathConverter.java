/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.util.LinkedHashSet;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingReader;

public class XstreamPathConverter implements Converter {

    public static final String PATH_KEY = "PATHS";

    private static final Pattern XPATH_INDEX = Pattern.compile("\\[[0-9]*\\]");

    private static final Pattern XPATH_ATTRIBUTE = Pattern.compile("/@.*$");

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter,
            MarshallingContext marshallingContext) {
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

        if (pathValueTracker != null) {

            PathTracker tracker = new PathTracker();
            PathTrackingReader pathReader = new PathTrackingReader(reader, tracker);

            readPath(pathReader, tracker, pathValueTracker);

        }
        return pathValueTracker;

    }

    /**
     * Reads through the tree looking for a specific path and returns the value at that node
     * <p/>
     * The reader is moved to the next node in the path
     * <p/>
     * For example, if readPath(reader, "a", "b", "c") is called, then the value at /a/b/c is
     * returned and the reader is advanced to "<d>"
     * {code}
     * <a>  <-- reader starts here
     * <b>
     * <c>value</c>
     * </b>
     * </a>
     * <d></d>
     * {code}
     *
     * @param reader
     * @param tracker
     * @param pathValueTracker
     */
    protected void readPath(PathTrackingReader reader, PathTracker tracker,
            XstreamPathValueTracker pathValueTracker) {

        pathValueTracker.getPaths()
                .forEach(path -> updatePath(reader, path, tracker.getPath(), pathValueTracker));

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            readPath(reader, tracker, pathValueTracker);
            reader.moveUp();
        }
    }

    protected void updatePath(PathTrackingReader reader, Path path, Path currentPath,
            XstreamPathValueTracker pathValueTracker) {
        if (doBasicPathsMatch(path, currentPath)) {

            if (path.toString()
                    .contains("@")) {
                String attributeName = StringUtils.substringAfterLast(path.toString(), "@");
                pathValueTracker.add(path, reader.getAttribute(attributeName));

            } else {
                pathValueTracker.add(path, reader.getValue());
            }

        }
    }

    protected boolean doBasicPathsMatch(final Path path1, final Path path2) {
        if (path1.equals(path2)) {
            return true;
        }
        // ignore count designators and attribute specifications whenc omparing paths
        // ie, /a/b[3]/c/@foo for our purposes is equivalent to /a/b/c

        String path1Replaced = normalizePath(path1);
        String path2Replaced = normalizePath(path2);

        return path1Replaced.equals(path2Replaced);

    }

    private String normalizePath(Path path) {
        return StringUtils.chomp(XPATH_ATTRIBUTE.matcher(XPATH_INDEX.matcher(path.toString())
                .replaceAll(""))
                .replaceAll(""), "/");
    }

    @Override
    public boolean canConvert(Class clazz) {
        return XstreamPathValueTracker.class.isAssignableFrom(clazz);
    }
}
