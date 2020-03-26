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
package ddf.catalog.resource.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceReader;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.SecurityConstants;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.tika.Tika;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A URLResourceReader retrieves a {@link ddf.catalog.resource.Resource} from a local or remote file
 * system using a {@link URI}. The {@link URI} is used to specify the file location. A
 * URLResourceReader supports {@link URI}s with HTTP, HTTPS, and file schemes.
 */
public class URLResourceReader implements ResourceReader {

  private static final String URL_HTTP_SCHEME = "http";

  private static final String URL_HTTPS_SCHEME = "https";

  private static final String URL_FILE_SCHEME = "file";

  private static final Logger LOGGER = LoggerFactory.getLogger(URLResourceReader.class);

  private static final String VERSION = "1.0";

  private static final String SHORTNAME = "URLResourceReader";

  private static final String TITLE = "URL ddf.catalog.resource.Resource Reader";

  private static final String DESCRIPTION = "Retrieves a file from a remote file system.";

  private static final String ORGANIZATION = "DDF";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  private static final String BYTES_TO_SKIP = "BytesToSkip";

  private static final String USERNAME = "username";

  @SuppressWarnings("squid:S2068" /* Password property key */)
  private static final String PASSWORD = "password";

  static final String ID_PROPERTY = "id";

  static final String OAUTH_DISCOVERY_URL = "oauthDiscoveryUrl";

  static final String OAUTH_CLIENT_ID = "oauthClientId";

  static final String OAUTH_CLIENT_SECRET = "oauthClientSecret";

  static final String OAUTH_FLOW = "oauthFlow";

  private static final Set<String> QUALIFIER_SET =
      ImmutableSet.of(URL_HTTP_SCHEME, URL_HTTPS_SCHEME, URL_FILE_SCHEME);

  private final ClientFactoryFactory clientFactoryFactory;

  /** Mapper for file extensions-to-mime types (and vice versa) */
  private MimeTypeMapper mimeTypeMapper;

  private Set<String> rootResourceDirectories = new HashSet<>();

  private boolean followRedirects = true;

  /** Default URLResourceReader constructor. */
  public URLResourceReader(ClientFactoryFactory clientFactoryFactory) {
    this.clientFactoryFactory = clientFactoryFactory;
  }

  public URLResourceReader(
      MimeTypeMapper mimeTypeMapper, ClientFactoryFactory clientFactoryFactory) {
    if (mimeTypeMapper == null) {
      LOGGER.debug("mimeTypeMapper is NULL");
    }
    this.mimeTypeMapper = mimeTypeMapper;
    this.clientFactoryFactory = clientFactoryFactory;

    LOGGER.debug(
        "Supported Schemes for {}: {}", URLResourceReader.class.getSimpleName(), QUALIFIER_SET);
  }

  public Set<String> getURLSupportedSchemes() {
    return QUALIFIER_SET;
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
    return QUALIFIER_SET;
  }

  public MimeTypeMapper getMimeTypeMapper() {
    return mimeTypeMapper;
  }

  public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
    this.mimeTypeMapper = mimeTypeMapper;
  }

  /**
   * Sets the directories that the {@link URLResourceReader} has permission to access when
   * attempting to download a resource linked by a file URL.
   *
   * @param rootResourceDirectoryPaths a set of absolute paths specifying which directories the
   *     {@link URLResourceReader} has permission to access when attempting to download resources
   *     linked by a file URL. A null or empty input clears all root resource directory paths from
   *     the {@link URLResourceReader} (this effectively blocks all resource downloads linked by
   *     file URLs).
   */
  public void setRootResourceDirectories(Set<String> rootResourceDirectoryPaths) {
    this.rootResourceDirectories.clear();
    if (rootResourceDirectoryPaths != null) {
      LOGGER.debug(
          "Attempting to set Root Resource Directories to {} for {}",
          rootResourceDirectoryPaths,
          URLResourceReader.class.getSimpleName());

      for (String rootResourceDirectoryPath : rootResourceDirectoryPaths) {
        String path;
        try {
          path = Paths.get(rootResourceDirectoryPath).toAbsolutePath().normalize().toString();
          this.rootResourceDirectories.add(path);
          LOGGER.debug(
              "Added [{}] to the list of Root Resource Directories for {}",
              path,
              URLResourceReader.class.getSimpleName());
        } catch (InvalidPathException e) {
          LOGGER.info("{} is an invalid path.", rootResourceDirectoryPath, e);
        }
      }
    }

    LOGGER.debug(
        "Root Resource Directories for {} are {}",
        URLResourceReader.class.getSimpleName(),
        this.rootResourceDirectories);
  }

  public Set<String> getRootResourceDirectories() {
    return this.rootResourceDirectories;
  }

  /**
   * Specifies whether the code should follow server issued redirection (HTTP Response codes between
   * 300 and 400)
   *
   * @param redirect true - follow redirections automatically false - do not follow server issued
   *     redirections
   */
  public void setFollowRedirects(Boolean redirect) {
    LOGGER.debug(
        "{}: Setting follow URL redirects (HTTP 300 codes) to {}",
        URLResourceReader.class.getName(),
        redirect);
    if (redirect != null) {
      this.followRedirects = redirect;
    }
  }

  /**
   * Gets the autoRedirect property
   *
   * @return true if the server issued redirections should be automatically followed
   */
  public Boolean getFollowRedirects() {
    return followRedirects;
  }

  /**
   * Retrieves a {@link ddf.catalog.resource.Resource} based on a {@link URI} and provided
   * arguments. A connection is made to the {@link URI} to obtain the {@link
   * ddf.catalog.resource.Resource}'s {@link InputStream} and build a {@link ResourceResponse} from
   * that. If the {@link URI}'s scheme is HTTP or HTTPS, the {@link ddf.catalog.resource.Resource}'s
   * name gets set to the {@link URI} passed in, otherwise, if it is a file scheme, the name is set
   * to the actual file name.
   *
   * @param resourceURI A {@link URI} that defines what {@link ddf.catalog.resource.Resource} to
   *     retrieve and how to do it.
   * @param properties Any additional arguments that should be passed to the {@link ResourceReader}.
   * @return A {@link ResourceResponse} containing the retrieved {@link
   *     ddf.catalog.resource.Resource}.
   */
  @Override
  public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
      throws IOException, ResourceNotFoundException {
    String bytesToSkip;
    if (resourceURI == null) {
      LOGGER.debug("Resource URI was null");
      throw new ResourceNotFoundException("Unable to find resource");
    }

    if (properties.containsKey(BYTES_TO_SKIP)) {
      bytesToSkip = properties.get(BYTES_TO_SKIP).toString();
      LOGGER.debug("bytesToSkip: {}", bytesToSkip);
    } else {
      bytesToSkip = "0";
    }

    switch (resourceURI.getScheme()) {
      case URL_HTTP_SCHEME:
      case URL_HTTPS_SCHEME:
        LOGGER.debug("Resource URI is HTTP or HTTPS");

        final Serializable qualifierSerializable = properties.get(ContentItem.QUALIFIER_KEYWORD);
        if (qualifierSerializable instanceof String) {
          final String qualifier = (String) qualifierSerializable;
          if (StringUtils.isNotBlank(qualifier)) {
            resourceURI =
                UriBuilder.fromUri(resourceURI)
                    .queryParam(ContentItem.QUALIFIER_KEYWORD, qualifier)
                    .build();
          }
        }

        String fileAddress = resourceURI.toURL().getFile();
        LOGGER.debug("resource name: {}", fileAddress);
        return retrieveHttpProduct(resourceURI, fileAddress, bytesToSkip, properties);
      case URL_FILE_SCHEME:
        LOGGER.debug("Resource URI is a File");
        File filePathName = new File(resourceURI);
        if (validateFilePath(filePathName)) {
          String fileName = filePathName.getName();
          LOGGER.debug("resource name: {}", fileName);
          return retrieveFileProduct(resourceURI, fileName, bytesToSkip);
        } else {
          throw new ResourceNotFoundException(
              "Error retrieving resource ["
                  + resourceURI.toString()
                  + "]. Invalid Resource URI of ["
                  + resourceURI.toString()
                  + "]. Resources must be in one of the following directories: "
                  + this.rootResourceDirectories.toString());
        }
      default:
        throw new ResourceNotFoundException(
            "Resource qualifier ( "
                + resourceURI.getScheme()
                + " ) not valid. "
                + URLResourceReader.TITLE
                + " requires a qualifier of "
                + URL_HTTP_SCHEME
                + " or "
                + URL_HTTPS_SCHEME
                + " or "
                + URL_FILE_SCHEME);
    }
  }

  private ResourceResponse retrieveFileProduct(
      URI resourceURI, String productName, String bytesToSkip) throws ResourceNotFoundException {
    URLConnection connection;
    try {
      LOGGER.debug("Opening connection to: {}", resourceURI);
      connection = resourceURI.toURL().openConnection();

      final String originalFileName = productName;
      productName =
          AccessController.doPrivileged(
              (PrivilegedAction<String>)
                  () ->
                      StringUtils.defaultIfBlank(
                          handleContentDispositionHeader(
                              connection.getHeaderField(HttpHeaders.CONTENT_DISPOSITION)),
                          originalFileName));

      String mimeType = getMimeType(resourceURI, productName);

      InputStream is = connection.getInputStream();

      skipBytes(is, bytesToSkip);

      return new ResourceResponseImpl(
          new ResourceImpl(
              new BufferedInputStream(is), mimeType, FilenameUtils.getName(productName)));
    } catch (MimeTypeResolutionException | IOException e) {
      LOGGER.info("Error retrieving resource", e);
      throw new ResourceNotFoundException(
          "Unable to retrieve resource at: " + resourceURI.toString(), e);
    }
  }

  private ResourceResponse retrieveHttpProduct(
      URI resourceURI, String productName, String bytesToSkip, Map<String, Serializable> properties)
      throws ResourceNotFoundException {

    try {
      LOGGER.debug("Opening connection to: {}", resourceURI);

      WebClient client = getWebClient(resourceURI.toString(), properties);

      Response response = client.get();

      MultivaluedMap<String, Object> headers = response.getHeaders();
      List<Object> cdHeaders = headers.get(HttpHeaders.CONTENT_DISPOSITION);
      if (cdHeaders != null && !cdHeaders.isEmpty()) {
        String contentHeader = (String) cdHeaders.get(0);
        productName =
            StringUtils.defaultIfBlank(handleContentDispositionHeader(contentHeader), productName);
      }
      String mimeType = getMimeType(resourceURI, productName);

      Response clientResponse = client.get();

      InputStream is;
      Object entityObj = clientResponse.getEntity();
      if (entityObj instanceof InputStream) {
        is = (InputStream) entityObj;
        if (Response.Status.OK.getStatusCode() != clientResponse.getStatus()
            && Response.Status.PARTIAL_CONTENT.getStatusCode() != clientResponse.getStatus()) {
          String error = getResponseErrorMessage(is);
          String errorMsg =
              "Received error code while retrieving resource (status "
                  + clientResponse.getStatus()
                  + "): "
                  + error;
          throw new ResourceNotFoundException(errorMsg);
        }
      } else {
        throw new ResourceNotFoundException("Received null response while retrieving resource.");
      }

      long responseBytesSkipped = 0L;
      if (headers.getFirst(HttpHeaders.CONTENT_RANGE) != null) {
        String contentRangeHeader = String.valueOf(headers.getFirst(HttpHeaders.CONTENT_RANGE));
        responseBytesSkipped =
            Long.parseLong(
                StringUtils.substringBetween(contentRangeHeader.toLowerCase(), "bytes ", "-"));
      }
      alignStream(is, Long.parseLong(bytesToSkip), responseBytesSkipped);

      return new ResourceResponseImpl(
          new ResourceImpl(
              new BufferedInputStream(is), mimeType, FilenameUtils.getName(productName)));
    } catch (MimeTypeResolutionException | IOException | WebApplicationException e) {
      LOGGER.info("Error retrieving resource", e);
      throw new ResourceNotFoundException(
          "Unable to retrieve resource at: " + resourceURI.toString(), e);
    }
  }

  private String getResponseErrorMessage(InputStream is) {
    String error = "";
    try {
      error = IOUtils.toString(is);
    } catch (IOException ioe) {
      LOGGER.debug("Could not convert error message to a string for output.", ioe);
    }
    return error;
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
      LOGGER.debug("mimeTypeMapper is NULL");
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
      final Tika tika = new Tika();
      try {
        mimeType =
            AccessController.doPrivileged(
                (PrivilegedExceptionAction<String>) () -> tika.detect(resourceURI.toURL()));
      } catch (PrivilegedActionException e) {
        throw new IOException("Error performing privileged action", e.getException());
      }
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
      ContentDisposition contentDisposition = new ContentDisposition(contentDispositionHeader);
      String filename = contentDisposition.getParameter("filename");
      if (StringUtils.isNotBlank(filename)) {
        LOGGER.debug("Found content disposition header, changing resource name to {}", filename);
        return filename;
      }
    }
    return "";
  }

  private void skipBytes(InputStream is, String bytesToSkip) throws IOException {
    if (bytesToSkip != null) {
      LOGGER.debug("Skipping {} bytes", bytesToSkip);
      long bytesSkipped = is.skip(Long.parseLong(bytesToSkip));
      if (Long.parseLong(bytesToSkip) != bytesSkipped) {
        LOGGER.debug(
            "Did not skip specified bytes while retrieving resource."
                + " Bytes to skip: {} -- Skipped Bytes: {}",
            bytesToSkip,
            bytesSkipped);
      }
    }
  }

  private void alignStream(InputStream in, long requestedBytesToSkip, long responseBytesSkipped)
      throws IOException {
    long misalignment = requestedBytesToSkip - responseBytesSkipped;

    if (misalignment == 0) {
      LOGGER.trace("Server responded with the correct byte range.");
      return;
    }

    try {
      if (requestedBytesToSkip > responseBytesSkipped) {
        LOGGER.debug(
            "Server returned incorrect byte range, skipping first [{}] bytes", misalignment);
        if (in.skip(misalignment) != misalignment) {
          throw new IOException(
              String.format("Input Stream could not be skipped %d bytes.", misalignment));
        }

      } else {
        throw new IOException("Server skipped more bytes than requested in the range header.");
      }
    } catch (IOException e) {
      throw new IOException(
          String.format(
              "Unable to align input stream with the requested byteOffset of %d",
              requestedBytesToSkip),
          e);
    }
  }

  private boolean validateFilePath(File resourceFilePath) throws IOException {
    String resourceCanonicalPath;
    try {
      resourceCanonicalPath =
          AccessController.doPrivileged(
              (PrivilegedExceptionAction<String>) () -> resourceFilePath.getCanonicalPath());
    } catch (PrivilegedActionException e) {
      LOGGER.debug("Unable to read path [{}]", resourceFilePath, e.getException());
      return false;
    }
    LOGGER.debug(
        "Converted resource path [{}] to its canonical path of [{}]",
        resourceFilePath,
        resourceCanonicalPath);
    if (this.rootResourceDirectories != null) {
      for (String rootResourceDirectory : this.rootResourceDirectories) {
        String rootResouceDirectoryCanonicalPath =
            new File(rootResourceDirectory).getCanonicalPath();
        LOGGER.debug(
            "Converted root resource directory [{}] to its canonical path of [{}]",
            rootResourceDirectory,
            rootResouceDirectoryCanonicalPath);
        LOGGER.debug(
            "Determining if resource path [{}] starts with configured root resource directory [{}].",
            resourceCanonicalPath,
            rootResouceDirectoryCanonicalPath);
        if (StringUtils.startsWith(resourceCanonicalPath, rootResouceDirectoryCanonicalPath)) {
          LOGGER.debug(
              "Resource path [{}] starts with configured root resource directory [{}]. Resource is in a valid location for download by the {}",
              resourceCanonicalPath,
              rootResouceDirectoryCanonicalPath,
              URLResourceReader.class.getSimpleName());
          return true;
        } else {
          LOGGER.debug(
              "Resource path [{}] does not start with configured root resource directory [{}].",
              resourceCanonicalPath,
              rootResouceDirectoryCanonicalPath);
        }
      }

      LOGGER.debug(
          "Unable to find a root resource directory in the {}'s configuration for resource path [{}]. Unable to download resource.",
          URLResourceReader.class.getSimpleName(),
          resourceCanonicalPath);
      return false;
    }

    return false;
  }

  protected WebClient getWebClient(String uri, Map<String, Serializable> properties) {
    SecureCxfClientFactory<WebClient> factory;
    if (properties.get(USERNAME) != null && properties.get(PASSWORD) != null) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              uri,
              WebClient.class,
              null,
              null,
              false,
              getFollowRedirects(),
              null,
              null,
              (String) properties.get(USERNAME),
              (String) properties.get(PASSWORD));
    } else if (properties.get(ID_PROPERTY) != null
        && properties.get(OAUTH_DISCOVERY_URL) != null
        && properties.get(OAUTH_CLIENT_ID) != null
        && properties.get(OAUTH_CLIENT_SECRET) != null
        && properties.get(OAUTH_FLOW) != null) {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              uri,
              WebClient.class,
              null,
              null,
              false,
              getFollowRedirects(),
              null,
              null,
              (String) properties.get(ID_PROPERTY),
              (String) properties.get(OAUTH_DISCOVERY_URL),
              (String) properties.get(OAUTH_CLIENT_ID),
              (String) properties.get(OAUTH_CLIENT_SECRET),
              (String) properties.get(OAUTH_FLOW));
    } else {
      factory =
          clientFactoryFactory.getSecureCxfClientFactory(
              uri, WebClient.class, null, null, false, getFollowRedirects());
    }
    Serializable subject = properties.get(SecurityConstants.SECURITY_SUBJECT);
    if (subject instanceof org.apache.shiro.subject.Subject) {
      return factory.getClientForSubject((org.apache.shiro.subject.Subject) subject);
    }
    return factory.getWebClient();
  }

  @Override
  public Set<String> getOptions(Metacard metacard) {
    LOGGER.debug("URLResourceReader getOptions doesn't support options, returning empty set.");
    return Collections.emptySet();
  }
}
