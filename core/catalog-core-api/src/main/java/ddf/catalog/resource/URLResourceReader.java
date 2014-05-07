/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.resource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tika.Tika;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.ResourceResponseImpl;
import ddf.mime.MimeTypeMapper;

/**
 * A URLResourceReader retrieves a {@link Resource} from a local or remote file
 * system using a {@link URI}. The {@link URI} is used to specify the file
 * location. A URLResourceReader supports {@link URI}s with HTTP, HTTPS, and
 * file schemes.
 * 
 * @deprecated - URLResourceReader has been moved to
 *             catalog-core-urlresourcereader, a separate bundle containing this
 *             ResourceReader implementation
 */
@Deprecated
public class URLResourceReader implements ResourceReader {
    private static final String URL_HTTP_SCHEME = "http";

    private static final String URL_HTTPS_SCHEME = "https";

    private static final String URL_FILE_SCHEME = "file";

    private static final XLogger logger = new XLogger(LoggerFactory.getLogger(URLResourceReader.class));

    private static final String VERSION = "1.0";

    private static final String SHORTNAME = "URLResourceReader";

    private static final String TITLE = "URL Resource Reader";

    private static final String DESCRIPTION = "Retrieves a file from a remote file system.";

    private static final String ORGANIZATION = "DDF";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static Set<String> qualifierSet;
    static {
        qualifierSet = new HashSet<String>(3);
        qualifierSet.add(URL_HTTP_SCHEME);
        qualifierSet.add(URL_HTTPS_SCHEME);
        qualifierSet.add(URL_FILE_SCHEME);
    }

    /** Mapper for file extensions-to-mime types (and vice versa) */
    private MimeTypeMapper mimeTypeMapper;

    /**
     * Default URLResourceReader constructor.
     */
    public URLResourceReader() {
        logger.debug("INSIDE: resource-impl.URLResourceReader default constructor");
    }

    public URLResourceReader(MimeTypeMapper mimeTypeMapper) {
        logger.debug("INSIDE: resource-impl.URLResourceReader constructor to set mimeTypeMapper");
        if (mimeTypeMapper == null)
            logger.debug("mimeTypeMapper is NULL");
        this.mimeTypeMapper = mimeTypeMapper;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getId() {
        return SHORTNAME;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }

    /**
     * Supported schemes are HTTP, HTTPS, and file
     * 
     * @return set of supported schemes
     */
    @Override
    public Set<String> getSupportedSchemes() {
        return qualifierSet;
    }

    public static Set<String> getURLSupportedSchemes() {
        return qualifierSet;
    }

    public MimeTypeMapper getMimeTypeMapper() {
        return mimeTypeMapper;
    }

    public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
        logger.debug("Setting mimeTypeMapper");
        this.mimeTypeMapper = mimeTypeMapper;
    }

    /**
     * Retrieves a {@link Resource} based on a {@link URI} and provided arguments. A connection is
     * made to the {@link URI} to obtain the {@link Resource}'s {@link InputStream} and build a
     * {@link ResourceResponse} from that. If the {@link URI}'s scheme is HTTP or HTTPS, the
     * {@link Resource}'s name gets set to the {@link URI} passed in, otherwise, if it is a file
     * scheme, the name is set to the actual file name.
     * 
     * @param resourceURI
     *            A {@link URI} that defines what {@link Resource} to retrieve and how to do it.
     * @param properties
     *            Any additional arguments that should be passed to the {@link ResourceReader}.
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}.
     */
    @Override
    public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
        throws IOException, ResourceNotFoundException {
        String methodName = "getResource";
        logger.entry(methodName);

        if (resourceURI == null) {
            logger.warn("Resource URI was null");
            throw new ResourceNotFoundException("Unable to find resource");
        }

        if (resourceURI.getScheme().equals(URL_HTTP_SCHEME)
                || resourceURI.getScheme().equals(URL_HTTPS_SCHEME)) {
            logger.debug("Resource URI is HTTP or HTTPS");
            URL url = resourceURI.toURL();
            logger.debug("resource name: " + url.getFile());
            return doRetrieveProduct(resourceURI, url.getFile());

        } else if (resourceURI.getScheme().equals(URL_FILE_SCHEME)) {
            logger.debug("Resource URI is a File");
            File filePathName = new File(resourceURI);
            String fileName = filePathName.getName();
            logger.debug("resource name: " + fileName);
            return doRetrieveProduct(resourceURI, fileName);
        } else {
            ResourceNotFoundException ce = new ResourceNotFoundException("Resource qualifier ( "
                    + resourceURI.getScheme() + " ) not valid. " + URLResourceReader.TITLE
                    + " requires a qualifier of " + URL_HTTP_SCHEME + " or " + URL_HTTPS_SCHEME
                    + " or " + URL_FILE_SCHEME);
            logger.throwing(XLogger.Level.DEBUG, ce);
            logger.exit(methodName);
            throw ce;
        }
    }

    private ResourceResponse doRetrieveProduct(URI resourceURI, String productName)
        throws IOException, ResourceNotFoundException {
        logger.trace("ENTERING: doRetrieveProduct");
        try {
            logger.debug("Creating URL for path: " + resourceURI.getPath());

            URL url = resourceURI.toURL();
            logger.debug("Opening connection to: " + resourceURI.toString());
            URLConnection conn = url.openConnection();

            // Determine the mime type in a hierarchical fashion. The hierarchy is based on the
            // most accurate mime type resolution being used and lesser accurate approaches being
            // used
            // if a mime type is not resolved.

            // The approaches, in order, are:
            // 1. Try using the DDF MimeTypeMapper so that custom MimeTypeResolvers are used
            // 2. Try using Apache Tika directly on the URL

            String mimeType = null;
            if (mimeTypeMapper == null) {
                logger.warn("mimeTypeMapper is NULL");
            } else {
                // Extract the file extension (if any) from the URL's file
                int index = url.getFile().lastIndexOf(".");

                // If there is a file extension, attempt to get mime type based on the file
                // extension,
                // using the MimeTypeMapper so that any custom MimeTypeResolvers are consulted
                if (index > -1) {
                    String fileExtension = url.getFile().substring(index + 1);

                    // Handle case where "." or ".." could be in the URL path and there is no
                    // valid file extension.
                    // Example:
                    // C:\workspaces\ddf_sprints\ddf\catalog\resource\./src/test/resources/data/JpegWithoutExtension
                    // When this occurs move on to alternate mime type resolution approaches.
                    if (!fileExtension.contains("\\") && !fileExtension.contains("/")) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("url.getFile() = " + url.getFile()
                                    + ",   fileExtension = " + fileExtension);
                        }
                        mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("fileExtension = "
                                    + fileExtension
                                    + " is not valid - proceeding with alternate mime type resolution");
                        }
                    }
                }
            }

            // If MimeTypeMapper was null or did not yield a mime type, or if default
            // mime type was returned, try using Apache Tika to inspect the file for better
            // mime type resolution than just file extension mime type mapping
            if (mimeType == null || mimeType.isEmpty() || mimeType.equals(DEFAULT_MIME_TYPE)) {
                // Use Apache Tika to detect mime type from URL
                Tika tika = new Tika();
                mimeType = tika.detect(url);
                logger.debug("Tika determined mimeType for url = " + mimeType);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("mimeType = " + mimeType + " set by MimeTypeMapper");
                }
            }

            // DDF-1400: Legacy default is application/unknown but URLConnection returns
            // content/unknown
            // as default when mime type does not map to a file extension. To maintain legacy
            // compatibility, change content/unknown to application/unknown

            // DDF-1525: With switching to use MimeTypeMapper vs. URLConnection.getContentType() and
            // guessContentTypeFromName()
            // the underlying TikaMimeTypeResolver will always return at least
            // application/octet-stream as the default
            // mime type for an unknown file extension. Hence, application/unknown will probably
            // never be returned.
            if (mimeType == null || mimeType.equals("content/unknown")) {
                mimeType = "application/unknown";
            }

            if (logger.isDebugEnabled()) {
                logger.debug("mimeType set to: " + mimeType);
            }
            InputStream is = conn.getInputStream();
            if (logger.isDebugEnabled()) {
                logger.debug("url file: " + url.getFile());
            }

            logger.trace("EXITING: doRetrieveProduct");

            return new ResourceResponseImpl(new ResourceImpl(new BufferedInputStream(is), mimeType,
                    productName));
        } catch (IOException e) {
            logger.error("IOException on retrieving resource", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error retrieving resource", e);
            throw new ResourceNotFoundException("Unable to retrieve resource at: "
                    + resourceURI.toString(), e);
        }
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        logger.trace("ENTERING/EXITING: getOptions");
        logger.debug("URLResourceReader getOptions doesn't support options, returning empty set.");
        return Collections.emptySet();
    }
}
