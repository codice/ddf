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
package org.codice.ddf.spatial.geocoding.extract;

import com.google.common.io.ByteSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryCreator;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;

public class GeoNamesFileExtractor implements GeoEntryExtractor {

  private static final int BUFFER_SIZE = 4096;

  private static final String[] SCHEMES = {"http", "https"};

  private static final UrlValidator URL_VALIDATOR =
      new UrlValidator(SCHEMES, UrlValidator.ALLOW_LOCAL_URLS);

  private GeoEntryCreator geoEntryCreator;

  private WebClient webClient;

  private long fileSize = -1;

  private String url;

  public void setGeoEntryCreator(final GeoEntryCreator geoEntryCreator) {
    this.geoEntryCreator = geoEntryCreator;
  }

  @Override
  public void setUrl(String url) {
    if (!url.endsWith("/")) {
      this.url = url + "/";
    } else {
      this.url = url;
    }
  }

  @Override
  public List<GeoEntry> getGeoEntries(
      final String resource, final ProgressCallback progressCallback)
      throws GeoEntryExtractionException, GeoNamesRemoteDownloadException {
    // It's probably safe to assume that there will be at least 1000 GeoNames entries in the
    // resource.
    final List<GeoEntry> geoEntryList = new ArrayList<>(1000);

    final ExtractionCallback extractionCallback =
        new ExtractionCallback() {
          @Override
          public void extracted(final GeoEntry newEntry) {
            geoEntryList.add(newEntry);
          }

          @Override
          public void updateProgress(final int progress) {
            if (progressCallback != null) {
              progressCallback.updateProgress(progress);
            }
          }
        };

    pushGeoEntriesToExtractionCallback(resource, extractionCallback);

    return geoEntryList;
  }

  @Override
  public void pushGeoEntriesToExtractionCallback(
      final String resource, final ExtractionCallback extractionCallback)
      throws GeoEntryExtractionException, GeoNamesRemoteDownloadException {
    if (extractionCallback == null) {
      throw new IllegalArgumentException("You must pass a non-null callback.");
    }

    InputStream fileInputStream = getInputStreamFromResource(resource, extractionCallback);

    try (InputStreamReader inputStreamReader =
            new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inputStreamReader)) {

      double bytesRead = 0.0;

      for (String line; (line = reader.readLine()) != null; ) {
        extractionCallback.extracted(extractGeoEntry(line, resource));
        bytesRead += line.getBytes(StandardCharsets.UTF_8).length;
        extractionCallback.updateProgress((int) (50 + (bytesRead / fileSize) * 50));
      }
      extractionCallback.updateProgress(100);

    } catch (IOException e) {
      throw new GeoEntryExtractionException("Unable to open stream for " + resource, e);
    } catch (IndexOutOfBoundsException | NumberFormatException e) {
      throw new GeoEntryExtractionException(
          resource + " does not follow the " + "expected GeoNames file format.", e);
    } catch (GeoEntryIndexingException e) {
      throw new GeoEntryExtractionException("Unable to extract GeoEntry from " + resource + ".", e);
    }
  }

  /**
   * Determines the appropriate InputStream to get from the given file resource. If there is no file
   * extension, the resource is downloaded from a url ( default :
   * http://download.geonames.org/export/dump/ ) otherwise it is treated as an absolute path for a
   * file.
   *
   * @param resource - a String representing the resource. Could be a data set from GeoNames
   *     (allCities, cities15000, AU, US etc) or an absolute path.
   * @param extractionCallback - the callback to receive updates about the progress, may be null if
   *     you don't want any updates
   * @return the InputStream for the given resource
   * @throws GeoEntryExtractionException if an error occurs during extraction, obtaining the
   *     InputStream from a local resource, or if the resource is not a .zip or .txt file
   * @throws GeoNamesRemoteDownloadException if an error occurs when attempting to get an
   *     InputStream from a URL
   */
  private InputStream getInputStreamFromResource(
      String resource, ProgressCallback extractionCallback)
      throws GeoEntryExtractionException, GeoNamesRemoteDownloadException {
    boolean resourceIsValidUrl = URL_VALIDATOR.isValid(resource);
    if (!resourceIsValidUrl && FilenameUtils.isExtension(resource, "zip")) {
      InputStream inputStream = getFileInputStream(resource);
      return unZipInputStream(resource, inputStream);

    } else if (!resourceIsValidUrl && FilenameUtils.isExtension(resource, "txt")) {
      return getFileInputStream(resource);

    } else if (StringUtils.isNotBlank(resource)) {

      if (!StringUtils.isAlphanumeric(resource) && !resourceIsValidUrl) {
        throw new GeoEntryExtractionException(
            String.format(
                "Unable to update the index. %s is not a supported file path.", resource));
      }

      // Support the "all" keyword
      if ("all".equalsIgnoreCase(resource)) {
        resource = "allCountries";
        // Support case insensitive "cities" keyword
      } else if (resource.matches("((?i)cities[0-9]+)")) {
        resource = resource.toLowerCase();
        // Support case insensitive country codes
      } else if (!"allCountries".equalsIgnoreCase(resource) && !resourceIsValidUrl) {
        resource = resource.toUpperCase();
      }

      Response response = createConnection(resource);
      InputStream urlInputStream = getUrlInputStreamFromWebClient();
      InputStream inputStream =
          getInputStreamFromUrl(resource, response, urlInputStream, extractionCallback);

      return unZipInputStream(resource, inputStream);
    }
    throw new GeoEntryExtractionException(
        "Unable to update the index.  "
            + resource
            + " is not a .zip or .txt, or an invalid country code was entered.");
  }

  /**
   * Download a GeoNames .zip file from a remote location
   *
   * @param resource - the name of the zip file to download ( ex. AD )
   * @param response - the response from the get request
   * @param inputStream - the InputStream from the web connection
   * @param progressCallback - the callback to receive updates about the progress, may be null if
   *     you don't want any updates
   * @throws GeoNamesRemoteDownloadException when the connection could not be established or the
   *     file could not be downloaded.
   */
  private InputStream getInputStreamFromUrl(
      String resource,
      Response response,
      InputStream inputStream,
      ProgressCallback progressCallback)
      throws GeoNamesRemoteDownloadException {
    int responseCode = 0;

    try (TemporaryFileBackedOutputStream fileOutputStream =
        new TemporaryFileBackedOutputStream(BUFFER_SIZE)) {

      responseCode = response.getStatus();

      int totalFileSize = response.getLength();

      if (inputStream == null) {
        throw new GeoNamesRemoteDownloadException(
            "Unable to get input stream from "
                + url
                + ".  Server responded with : "
                + responseCode);
      }

      double totalBytesRead = 0.0;
      int bytesRead = -1;
      byte[] buffer = new byte[BUFFER_SIZE];

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        fileOutputStream.write(buffer, 0, bytesRead);
        totalBytesRead += bytesRead;
        if (progressCallback != null) {
          progressCallback.updateProgress((int) ((totalBytesRead / totalFileSize) * 50));
        }
      }

      if (progressCallback != null) {
        progressCallback.updateProgress(50);
      }

      ByteSource byteSource = fileOutputStream.asByteSource();
      fileOutputStream.flush();
      inputStream.close();
      closeConnection();

      return byteSource.openBufferedStream();

    } catch (IOException e) {
      throw new GeoNamesRemoteDownloadException(
          "Unable to download "
              + resource
              + " from "
              + url
              + ".  Server responded with : "
              + responseCode,
          e);
    }
  }

  public Response createConnection(String resource) {
    if (URL_VALIDATOR.isValid(resource)) {
      webClient = WebClient.create(resource);
      webClient.path(resource);
    } else {
      webClient = WebClient.create(url + resource + ".zip");
      webClient.path(url + resource + ".zip");
    }
    return webClient.get();
  }

  public InputStream getUrlInputStreamFromWebClient() throws GeoNamesRemoteDownloadException {
    try {
      return webClient.get(InputStream.class);
    } catch (NotFoundException e) {
      throw new GeoNamesRemoteDownloadException(
          "Unable get Input Stream from " + webClient.getCurrentURI(), e);
    }
  }

  public void closeConnection() {
    webClient.close();
  }

  /**
   * Get the InputStream for the given file resource.
   *
   * @param resource - the absolute path of the file to open the InputStream for.
   * @return the InputStream for the file resource
   * @throws GeoEntryExtractionException when the file cannot be found.
   */
  private InputStream getFileInputStream(String resource) throws GeoEntryExtractionException {
    FileInputStream fileInputStream;

    try {
      URI fileUri;
      if (resource.startsWith("file://")) {
        fileUri = new URI(resource);
      } else {
        fileUri = new URI("file://" + resource);
      }
      File file = new File(fileUri);
      fileSize = file.getTotalSpace();
      fileInputStream = new FileInputStream(file);
    } catch (FileNotFoundException | URISyntaxException e) {
      throw new GeoEntryExtractionException(resource + " cannot be found", e);
    }
    return fileInputStream;
  }

  /**
   * Unzips a file and returns the output as a new InputStream
   *
   * @param resource - the name of the resource file to be unzipped
   * @param inputStream - the InputStream for the file to be unzipped
   * @return - the unzipped file as an InputStream
   * @throws GeoEntryExtractionException when the given file fails to be unzipped.
   */
  private InputStream unZipInputStream(String resource, InputStream inputStream)
      throws GeoEntryExtractionException {
    try (TemporaryFileBackedOutputStream bufferedOutputStream =
            new TemporaryFileBackedOutputStream(BUFFER_SIZE);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

      ZipEntry zipEntry;
      while ((zipEntry = zipInputStream.getNextEntry()) != null) {

        // GeoNames <filename>.zip files will contain <filename>.txt and readme.txt
        if (!"readme.txt".equals(zipEntry.getName())) {

          byte data[] = new byte[BUFFER_SIZE];
          int bytesRead;
          while ((bytesRead = zipInputStream.read(data, 0, BUFFER_SIZE)) != -1) {
            bufferedOutputStream.write(data, 0, bytesRead);
          }

          ByteSource zipByteSource = bufferedOutputStream.asByteSource();
          bufferedOutputStream.flush();
          fileSize = zipByteSource.size();
          return zipByteSource.openBufferedStream();
        }
      }

    } catch (IOException e) {
      throw new GeoEntryExtractionException("Unable to unzip " + resource, e);
    }

    throw new GeoEntryExtractionException("Unable to unzip " + resource);
  }

  private GeoEntry extractGeoEntry(final String line, final String resource) {
    return geoEntryCreator.createGeoEntry(line, resource);
  }
}
