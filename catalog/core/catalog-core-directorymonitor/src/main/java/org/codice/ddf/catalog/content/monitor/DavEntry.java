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
 */
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

/**
 * A webdav implementation of the {@link org.apache.commons.io.monitor.FileEntry}
 * Uses https://github.com/lookfirst/sardine
 */
public class DavEntry implements Serializable {

    private static final long serialVersionUID = -2505664948818681153L;

    private static final DavEntry[] EMPTY_ENTRIES = new DavEntry[0];

    public static final String HTTP = "http";

    public static final String FORSLASH = "/";

    private final DavEntry parent;

    private DavEntry[] children;

    private File file;

    private String location;

    private boolean exists;

    private boolean directory;

    private long lastModified;

    private long length;

    private String eTag;

    /**
     * @param location must be fully qualified
     */
    DavEntry(final String location) {
        this(null, location);
    }

    private DavEntry(final DavEntry parent, final String location) {
        if (location == null) {
            throw new IllegalArgumentException("File is missing");
        }
        this.parent = parent;
        this.setLocation(getLocation(location, parent));
    }

    static DavEntry[] getEmptyEntries() {
        return EMPTY_ENTRIES;
    }

    boolean refresh(DavResource davResource) {

        // cache original values
        final boolean origExists = isExists();
        final long origLastModified = getLastModified();
        final boolean origDirectory = isDirectory();
        final long origLength = getLength();
        final String origEtag = getETag();

        // refresh the values
        setExists(davResource != null);
        setDirectory(isExists() && davResource.isDirectory());
        setLastModified(isExists() && davResource.getModified() != null ?
                davResource.getModified()
                        .getTime() :
                0);
        setLength(isExists() && !isDirectory() ? davResource.getContentLength() : 0);
        setETag(isExists() ? davResource.getEtag() : "0");

        if (file != null && !isDirectory() && origLastModified != getLastModified()) {
            FileUtils.deleteQuietly(file.getParentFile());
        }

        // Return if there are changes
        return isExists() != origExists //
                || getLastModified() != origLastModified //
                || isDirectory() != origDirectory //
                || getLength() != origLength //
                || !Objects.equals(getETag(), origEtag);
    }

    DavEntry newChildInstance(String location) {
        return new DavEntry(this, location);
    }

    // construct the fully qualified location from a fully qualified parent and
    // a child relative to the parent
    static String getLocation(String initialLocation, DavEntry parent) {
        String location = initialLocation;
        if (parent != null && !location.startsWith(HTTP)) {
            String parentLocation = parent.getLocation();
            if (parentLocation.endsWith(FORSLASH) && location.startsWith(FORSLASH)) {
                location = location.replaceFirst(FORSLASH, "");
            }
            if (!parentLocation.endsWith(FORSLASH) && !location.startsWith(FORSLASH)) {
                location = FORSLASH + location;
            }
            location = parentLocation + location;
        }
        try {
            // URL class performs structural decomposition of location for us
            // URI class performs character encoding, but ONLY via multipart constructors
            // Finally, we have a fully qualified and escaped location for future manipulation
            URL url = new URL(location);
            URI uri = new URI(url.getProtocol(),
                    url.getUserInfo(),
                    url.getHost(),
                    url.getPort(),
                    url.getPath(),
                    url.getQuery(),
                    url.getRef());
            location = uri.toASCIIString();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return location;
    }

    /**
     * Return the parent entry.
     *
     * @return the parent entry
     */
    public DavEntry getParent() {
        return parent;
    }

    /**
     * Return the level
     *
     * @return the level
     */
    int getLevel() {
        return getParent() == null ? 0 : getParent().getLevel() + 1;
    }

    /**
     * Return the directory's files.
     *
     * @return This directory's files or an empty
     * array if the file is not a directory or the
     * directory is empty
     */
    DavEntry[] getChildren() {
        return children != null ? children : getEmptyEntries();
    }

    /**
     * Set the directory's files.
     *
     * @param children This directory's files, may be null
     */
    void setChildren(final DavEntry... children) {
        this.children = children;
    }

    /**
     * Return a local cache of the file being monitored.
     * This file is invalidated if necessary when refresh() is called.
     *
     * @param sardine
     * @return the file being monitored
     */
    public File getFile(Sardine sardine) throws IOException {
        if (file == null || !file.exists()) {
            Path dav = Files.createTempDirectory("dav");
            File dest = new File(dav.toFile(),
                    URLDecoder.decode(FilenameUtils.getName(getLocation()), "UTF-8"));
            try (OutputStream os = new FileOutputStream(dest)) {
                IOUtils.copy(sardine.get(getLocation()), os);
                setFile(dest);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return file;
    }

    /**
     * Return the file location.
     *
     * @return the file location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the file location.
     *
     * @param location the file location
     */
    public void setLocation(final String location) {
        this.location = location;
    }

    /**
     * Return the last modified time from the last time it
     * was checked.
     *
     * @return the last modified time
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Return the last modified time from the last time it
     * was checked.
     *
     * @param lastModified The last modified time
     */
    public void setLastModified(final long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Return the length.
     *
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * Set the length.
     *
     * @param length the length
     */
    public void setLength(final long length) {
        this.length = length;
    }

    /**
     * Indicate whether the file existed the last time it
     * was checked.
     *
     * @return whether the file existed
     */
    public boolean isExists() {
        return exists;
    }

    /**
     * Set whether the file existed the last time it
     * was checked.
     *
     * @param exists whether the file exists or not
     */
    public void setExists(final boolean exists) {
        this.exists = exists;
    }

    /**
     * Indicate whether the file is a directory or not.
     *
     * @return whether the file is a directory or not
     */
    public boolean isDirectory() {
        return directory;
    }

    /**
     * Set whether the file is a directory or not.
     *
     * @param directory whether the file is a directory or not
     */
    public void setDirectory(final boolean directory) {
        this.directory = directory;
    }

    boolean remoteExists(Sardine sardine) {
        try {
            sardine.list(getLocation());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getETag() {
        return eTag;
    }

    void setFile(File file) {
        this.file = file;
    }

    void setETag(String eTag) {
        this.eTag = eTag;
    }
}
