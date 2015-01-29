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
package ddf.catalog.resource.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceReader;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.tika.Tika;
import org.apache.tika.metadata.HttpHeaders;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A URLResourceReader retrieves a {@link Resource} from a local or remote file system using a
 * {@link URI}. The {@link URI} is used to specify the file location. A URLResourceReader supports
 * {@link URI}s with HTTP, HTTPS, and file schemes.
 */
public class URLResourceReader implements ResourceReader {
    private static final String URL_HTTP_SCHEME = "http";

    private static final String URL_HTTPS_SCHEME = "https";

    private static final String URL_FILE_SCHEME = "file";

    private static final Logger LOGGER = LoggerFactory.getLogger(URLResourceReader.class);

    private static final String VERSION = "1.0";

    private static final String SHORTNAME = "URLResourceReader";

    private static final String TITLE = "URL Resource Reader";

    private static final String DESCRIPTION = "Retrieves a file from a remote file system.";

    private static final String ORGANIZATION = "DDF";

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final int QUALIFIER_SET_SIZE = 3;

    private static final String BYTES_TO_SKIP = "BytesToSkip";

    private static Set<String> qualifierSet;

    static {
        qualifierSet = new HashSet<String>(QUALIFIER_SET_SIZE);
        qualifierSet.add(URL_HTTP_SCHEME);
        qualifierSet.add(URL_HTTPS_SCHEME);
        qualifierSet.add(URL_FILE_SCHEME);
    }

    /**
     * Mapper for file extensions-to-mime types (and vice versa)
     */
    private MimeTypeMapper mimeTypeMapper;

    /**
     * Default URLResourceReader constructor.
     */
    public URLResourceReader() {
    }

    public URLResourceReader(MimeTypeMapper mimeTypeMapper) {
        if (mimeTypeMapper == null) {
            LOGGER.debug("mimeTypeMapper is NULL");
        }
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
        this.mimeTypeMapper = mimeTypeMapper;
    }

    /**
     * Retrieves a {@link Resource} based on a {@link URI} and provided arguments. A connection is
     * made to the {@link URI} to obtain the {@link Resource}'s {@link InputStream} and build a
     * {@link ResourceResponse} from that. If the {@link URI}'s scheme is HTTP or HTTPS, the
     * {@link Resource}'s name gets set to the {@link URI} passed in, otherwise, if it is a file
     * scheme, the name is set to the actual file name.
     *
     * @param resourceURI A {@link URI} that defines what {@link Resource} to retrieve and how to do it.
     * @param properties  Any additional arguments that should be passed to the {@link ResourceReader}.
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}.
     */
    @Override
    public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
            throws IOException, ResourceNotFoundException {
        String bytesToSkip = null;

        if (resourceURI == null) {
            LOGGER.warn("Resource URI was null");
            throw new ResourceNotFoundException("Unable to find resource");
        }

        if (properties.containsKey(BYTES_TO_SKIP)) {
            bytesToSkip = properties.get(BYTES_TO_SKIP).toString();
            LOGGER.debug("bytesToSkip: {}", bytesToSkip);
        }

        if (resourceURI.getScheme().equals(URL_HTTP_SCHEME)
                || resourceURI.getScheme().equals(URL_HTTPS_SCHEME)) {
            LOGGER.debug("Resource URI is HTTP or HTTPS");
            String fileAddress = resourceURI.toURL().getFile();
            LOGGER.debug("resource name: {}", fileAddress);
            return retrieveHttpProduct(resourceURI, fileAddress, bytesToSkip, properties);
        } else if (resourceURI.getScheme().equals(URL_FILE_SCHEME)) {
            LOGGER.debug("Resource URI is a File");
            File filePathName = new File(resourceURI);
            String fileName = filePathName.getName();
            LOGGER.debug("resource name: {}", fileName);
            return retrieveFileProduct(resourceURI, fileName, bytesToSkip);
        } else {
            ResourceNotFoundException ce = new ResourceNotFoundException("Resource qualifier ( "
                    + resourceURI.getScheme() + " ) not valid. " + URLResourceReader.TITLE
                    + " requires a qualifier of " + URL_HTTP_SCHEME + " or " + URL_HTTPS_SCHEME
                    + " or " + URL_FILE_SCHEME);
            throw ce;
        }
    }

    private ResourceResponse retrieveFileProduct(URI resourceURI, String productName,
            String bytesToSkip) throws ResourceNotFoundException {
        URLConnection connection = null;
        try {
            LOGGER.debug("Opening connection to: {}", resourceURI.toString());
            connection = resourceURI.toURL().openConnection();

            productName = StringUtils
                    .defaultIfBlank(handleContentDispositionHeader(
                            connection.getHeaderField(HttpHeaders.CONTENT_DISPOSITION)),
                            productName);

            String mimeType = getMimeType(resourceURI, productName);

            InputStream is = connection.getInputStream();

            skipBytes(is, bytesToSkip);

            return new ResourceResponseImpl(new ResourceImpl(new BufferedInputStream(is), mimeType,
                    FilenameUtils.getName(productName)));
        } catch (MimeTypeResolutionException | IOException e) {
            LOGGER.error("Error retrieving resource", e);
            throw new ResourceNotFoundException("Unable to retrieve resource at: "
                    + resourceURI.toString(), e);
        }
    }

    private ResourceResponse retrieveHttpProduct(URI resourceURI, String productName,
            String bytesToSkip, Map<String, Serializable> properties)
            throws ResourceNotFoundException {

        try {
            LOGGER.debug("Opening connection to: {}", resourceURI.toString());

            WebClient client = getWebClient(resourceURI.toString());

            Object subjectObj = properties.get(SecurityConstants.SECURITY_SUBJECT);
            if (subjectObj != null) {
                Subject subject = (Subject) subjectObj;
                LOGGER.debug("Setting Subject on webclient: {}", subject);
                RestSecurity.setSubjectOnClient(subject, client);
            }

            Response response = client.get();

            MultivaluedMap<String, Object> headers = response.getHeaders();
            List<Object> cdHeaders = headers.get(HttpHeaders.CONTENT_DISPOSITION);
            if (cdHeaders != null && !cdHeaders.isEmpty()) {
                String contentHeader = (String) cdHeaders.get(0);
                productName = StringUtils
                        .defaultIfBlank(handleContentDispositionHeader(contentHeader), productName);
            }
            String mimeType = getMimeType(resourceURI, productName);

            Response clientResponse = client.get();

            InputStream is = null;
            Object entityObj = clientResponse.getEntity();
            if (entityObj instanceof InputStream) {
                is = (InputStream) entityObj;
                if (Response.Status.OK.getStatusCode() != clientResponse.getStatus()) {
                    String error = null;
                    try {
                        if (is != null) {
                            error = IOUtils.toString(is);
                        }
                    } catch (IOException ioe) {
                        LOGGER.debug("Could not convert error message to a string for output.",
                                ioe);
                    }
                    String errorMsg = "Received error code while retrieving resource (status "
                            + clientResponse.getStatus() + "): " + error;
                    LOGGER.warn(errorMsg);
                    throw new ResourceNotFoundException(errorMsg);
                }
            } else {
                throw new ResourceNotFoundException(
                        "Received null response while retrieving resource.");
            }

            skipBytes(is, bytesToSkip);

            return new ResourceResponseImpl(new ResourceImpl(new BufferedInputStream(is), mimeType,
                    FilenameUtils.getName(productName)));
        } catch (MimeTypeResolutionException | IOException | WebApplicationException e) {
            LOGGER.error("Error retrieving resource", e);
            throw new ResourceNotFoundException("Unable to retrieve resource at: "
                    + resourceURI.toString(), e);
        }
    }

    private String getMimeType(URI resourceURI, String productName)
            throws MimeTypeResolutionException, IOException {
        // Determine the mime type in a hierarchical fashion. The hierarchy is based on the
        // most accurate mime type resolution being used and lesser accurate approaches being
        // used
        // if a mime type is not resolved.

        // The approaches, in order, are:
        // 1. Try using the DDF MimeTypeMapper so that custom MimeTypeResolvers are used
        // 2. Try using Apache Tika directly on the URL

        String mimeType = null;
        if (mimeTypeMapper == null) {
            LOGGER.warn("mimeTypeMapper is NULL");
        } else {
            // Extract the file extension (if any) from the URL's filename
            String fileExtension = FilenameUtils.getExtension(productName);
            mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
        }

        // If MimeTypeMapper was null or did not yield a mime type, or if default
        // mime type was returned, try using Apache Tika to inspect the file for better
        // mime type resolution than just file extension mime type mapping
        if ((mimeType == null || mimeType.isEmpty() || mimeType.equals(DEFAULT_MIME_TYPE))
                && URL_FILE_SCHEME.equalsIgnoreCase(resourceURI.getScheme())) {
            // Use Apache Tika to detect mime type from URL
            Tika tika = new Tika();
            mimeType = tika.detect(resourceURI.toURL());
            LOGGER.debug("Tika determined mimeType for url = {}", mimeType);
        } else {
            LOGGER.debug("mimeType = {} set by MimeTypeMapper", mimeType);
        }

        // Legacy default is application/unknown but URLConnection returns content/unknown
        // as default when mime type does not map to a file extension. To maintain legacy
        // compatibility, change content/unknown to application/unknown

        // With switching to use MimeTypeMapper vs. URLConnection.getContentType() and
        // guessContentTypeFromName()
        // the underlying TikaMimeTypeResolver will always return at least
        // application/octet-stream as the default
        // mime type for an unknown file extension. Hence, application/unknown will probably
        // never be returned.
        if (mimeType == null || mimeType.equals("content/unknown")) {
            mimeType = "application/unknown";
        }

        LOGGER.debug("mimeType set to: {}", mimeType);
        return mimeType;
    }

    /* Check Connection headers for filename */
    private String handleContentDispositionHeader(String contentDispositionHeader) {
        if (StringUtils.isNotBlank(contentDispositionHeader)) {
            ContentDisposition contentDisposition = new ContentDisposition(
                    contentDispositionHeader);
            String filename = contentDisposition.getParameter("filename");
            if (StringUtils.isNotBlank(filename)) {
                LOGGER.debug("Found content disposition header, changing resource name to {}",
                        filename);
                return filename;
            }
        }
        return "";
    }

    private void skipBytes(InputStream is, String bytesToSkip) throws IOException {
        if (bytesToSkip != null) {
            LOGGER.debug("Skipping {} bytes", bytesToSkip);
            long bytesSkipped = is.skip(Long.valueOf(bytesToSkip));
            if (Long.valueOf(bytesToSkip) != bytesSkipped) {
                LOGGER.debug(
                        "Did not skip specified bytes while retrieving resource."
                                + " Bytes to skip: {} -- Skipped Bytes: {}", bytesToSkip,
                        bytesSkipped);
            }
        }
    }

    /* Added for Unit Testing */
    protected WebClient getWebClient(String uri) {
        return WebClient.create(uri);
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        LOGGER.debug("URLResourceReader getOptions doesn't support options, returning empty set.");
        return Collections.emptySet();
    }
}
