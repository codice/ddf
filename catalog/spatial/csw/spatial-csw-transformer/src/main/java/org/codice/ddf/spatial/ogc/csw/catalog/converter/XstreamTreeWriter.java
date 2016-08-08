/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import org.apache.commons.lang.StringUtils;

import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;

public class XstreamTreeWriter {
    private PathTrackingWriter writer = null;

    private PathTracker tracker = null;

    private XstreamPathValueTracker pathValues = null;

    XstreamTreeWriter(PathTrackingWriter writer, PathTracker tracker,
            XstreamPathValueTracker pathValues) {
        this.writer = writer;
        this.tracker = tracker;
        this.pathValues = pathValues;
    }

    void startVisit(final String node) {
        if (!isAttributeNode(node)) {
            writer.startNode(normalizeNode(node));
        }

        Path currentPath = tracker.getPath();

        for (Path path : pathValues.getPaths()) {
            String value = pathValues.getFirstValue(path);
            Path searchPath = path;

            if (value != null) {

                if (currentPath.isAncestor(searchPath) && searchPath.toString()
                        .endsWith(node)) {

                    if (isAttributePath(searchPath) & isAttributeNode(node)) {

                        writer.addAttribute(getAttributeNameFromPath(searchPath), value);
                    } else if (!isAttributeNode(node) && !isAttributePath(searchPath)) {

                        writer.setValue(value);
                    }
                }
            }
        }

    }

    private String normalizeNode(String node) {
        return node.replaceAll("\\[[0-9]+\\]", "");
    }

    void endVisit(final String node) {
        if (!isAttributeNode(node)) {
            writer.endNode();
        }
    }

    private boolean isAttributeNode(final String node) {
        return node.trim()
                .startsWith("@");
    }

    public boolean isAttributePath(final Path path) {
        return path.toString()
                .contains("@");
    }

    public String getAttributeNameFromPath(final Path path) {
        return StringUtils.substringAfterLast(path.toString(), "@");
    }

}
