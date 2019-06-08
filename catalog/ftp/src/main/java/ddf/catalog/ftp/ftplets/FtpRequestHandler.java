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
package ddf.catalog.ftp.ftplets;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.ftp.user.FtpUser;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ftpserver.ftplet.DataConnection;
import org.apache.ftpserver.ftplet.DataConnectionFactory;
import org.apache.ftpserver.ftplet.DefaultFtpReply;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for incoming FTP requests from the client. Supports the PUT, MPUT, DELE, MKD, RETR, RMD,
 * APPE, RNTO, STOU, and SITE operations. When PUTing a new file, it is not temporarily written to
 * the file system. Instead, the new file's data stream is ingested directly into the catalog. If
 * PUT submits a file that begins with a dot (eg. .foo), then the data is not immediately stored
 * into the catalog. Instead, the system waits for a RNTO command for the dot-file and then stores
 * the data in the catalog. Clients may PUT multiple dot-files and then execute multiple RNTO
 * commands. Any dot-files that are not renamed at the end of the ftp session will be discarded and
 * resources will be released. The CWD command always return a successful response. This is to
 * support clients that use a MKDIR/CWD/PUT command sequence.
 */
public class FtpRequestHandler extends DefaultFtplet {
  private static final Logger LOGGER = LoggerFactory.getLogger(FtpRequestHandler.class);

  private static final String SUBJECT = "subject";

  private static final String STOR_REQUEST = "STOR";

  private static final String STOU_REQUEST = "STOU";

  private static final String ATTR_TEMP_FILENAMES = "org.codice.ddf.temp-files";

  private static final String CWD = "CWD";

  private CatalogFramework catalogFramework;

  private MimeTypeMapper mimeTypeMapper;

  private UuidGenerator uuidGenerator;

  public FtpRequestHandler(
      CatalogFramework catalogFramework,
      MimeTypeMapper mimeTypeMapper,
      UuidGenerator uuidGenerator) {
    notNull(catalogFramework, "catalogFramework");
    notNull(mimeTypeMapper, "mimeTypeMapper");

    this.catalogFramework = catalogFramework;
    this.mimeTypeMapper = mimeTypeMapper;
    this.uuidGenerator = uuidGenerator;
  }

  @Override
  public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
    return FtpletResult.DEFAULT;
  }

  @SuppressWarnings("unchecked")
  @Override
  public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {

    removeAllTempFilesFromSession(session);

    return FtpletResult.DEFAULT;
  }

  @Override
  public FtpletResult onLogin(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.setAttribute(SUBJECT, ((FtpUser) session.getUser()).getSubject());
    return FtpletResult.SKIP;
  }

  /** Find the temp file map of {@link TemporaryFileBackedOutputStream} */
  @SuppressWarnings("unchecked")
  private Optional<Map<String, TemporaryFileBackedOutputStream>> findTempFileMap(
      FtpSession ftpSession) {
    return Optional.ofNullable(ftpSession.getAttribute(ATTR_TEMP_FILENAMES))
        .filter(Map.class::isInstance)
        .map(m -> (Map<String, TemporaryFileBackedOutputStream>) m);
  }

  /**
   * Adds the filename and {@link TemporaryFileBackedOutputStream} to the map. If the object in the
   * ftp session is not a map or is null, then a new map is added to the session. Returns the TFBOS
   * that passed in.
   */
  private TemporaryFileBackedOutputStream addTempFileToSession(
      FtpSession ftpSession,
      String filename,
      TemporaryFileBackedOutputStream temporaryFileBackedOutputStream) {

    Map<String, TemporaryFileBackedOutputStream> map =
        findTempFileMap(ftpSession)
            .orElseGet(
                () -> {
                  Map<String, TemporaryFileBackedOutputStream> m = new HashMap<>();
                  ftpSession.setAttribute(ATTR_TEMP_FILENAMES, m);
                  return m;
                });

    map.put(filename, temporaryFileBackedOutputStream);

    return temporaryFileBackedOutputStream;
  }

  /** Find the {@link TemporaryFileBackedOutputStream} for a filename. */
  private Optional<TemporaryFileBackedOutputStream> findTempFileInSession(
      FtpSession ftpSession, String filename) {
    return findTempFileMap(ftpSession)
        .filter(m -> m.containsKey(filename))
        .map(m -> m.get(filename));
  }

  /** Removes the entry and closes the {@link TemporaryFileBackedOutputStream}. */
  private void removeTempFileFromSession(FtpSession ftpSession, String filename) {
    findTempFileMap(ftpSession)
        .ifPresent(
            m -> {
              Optional.ofNullable(m.get(filename)).ifPresent(IOUtils::closeQuietly);
              m.remove(filename);
            });
  }

  /** Removes all temp file entries from the session. */
  private void removeAllTempFilesFromSession(FtpSession ftpSession) {
    findTempFileMap(ftpSession)
        .map(Map::keySet)
        .ifPresent(
            filenames ->
                filenames.forEach(filename -> removeTempFileFromSession(ftpSession, filename)));

    ftpSession.removeAttribute(ATTR_TEMP_FILENAMES);
  }

  private boolean isDotFile(String filename) {
    return filename.startsWith(".");
  }

  private FtpletResult store(FtpSession session, FtpRequest request, boolean isStoreUnique)
      throws FtpException, IOException {

    LOGGER.debug("Beginning FTP ingest of {}", request.getArgument());

    Subject shiroSubject = (Subject) session.getAttribute(SUBJECT);
    if (shiroSubject == null) {
      return FtpletResult.DISCONNECT;
    }

    FtpFile ftpFile = null;
    String fileName = request.getArgument();

    try {
      ftpFile = session.getFileSystemView().getFile(fileName);
    } catch (FtpException e) {
      LOGGER.debug("Failed to retrieve file from FTP session");
    }

    String requestTypeString = isStoreUnique ? STOU_REQUEST : STOR_REQUEST;

    if (ftpFile == null) {
      LOGGER.debug("Sending FTP status code 501 to client - syntax errors in request parameters");
      session.write(
          new DefaultFtpReply(
              FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, requestTypeString));

      throw new FtpException("File to be transferred from client did not exist");
    }

    DataConnectionFactory connFactory = session.getDataConnection();
    if (connFactory instanceof IODataConnectionFactory) {
      InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();
      if (address == null) {
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS, "PORT or PASV must be issued first"));
        LOGGER.debug(
            "Sending FTP status code 503 to client - PORT or PASV must be issued before STOR");
        throw new FtpException("FTP client address was null");
      }
    }

    if (!ftpFile.isWritable()) {
      session.write(
          new DefaultFtpReply(
              FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Insufficient permissions"));
      LOGGER.debug(
          "Sending FTP status code 550 to client - insufficient permissions to write file.");
      throw new FtpException("Insufficient permissions to write file");
    }

    session.write(
        new DefaultFtpReply(
            FtpReply.REPLY_150_FILE_STATUS_OKAY, requestTypeString + " " + fileName));
    LOGGER.debug("Replying to client with code 150 - file status okay");

    if (isDotFile(request.getArgument())) {
      DataConnection dataConnection;
      try {
        dataConnection = connFactory.openConnection();
      } catch (Exception e) {
        throw new IOException("Error getting the output stream from FTP session", e);
      }
      dataConnection.transferFromClient(
          session,
          addTempFileToSession(
              session, ftpFile.getAbsolutePath(), new TemporaryFileBackedOutputStream()));

      if (isStoreUnique) {
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_125_DATA_CONNECTION_ALREADY_OPEN,
                "Storing data with unique name: " + fileName));
      }

      session.write(
          new DefaultFtpReply(
              FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Closing data connection"));
      LOGGER.debug("Sending FTP status code 226 to client - closing data connection");
    } else {
      try (TemporaryFileBackedOutputStream outputStream = new TemporaryFileBackedOutputStream()) {
        DataConnection dataConnection = connFactory.openConnection();
        dataConnection.transferFromClient(session, outputStream);

        CreateStorageRequest createRequest = getCreateStorageRequest(fileName, outputStream);

        List<Metacard> storedMetacards = storeObject(shiroSubject, fileName, createRequest);

        if (isStoreUnique && !storedMetacards.isEmpty()) {
          String ids =
              storedMetacards.stream().map(Metacard::getId).collect(Collectors.joining(","));
          session.write(
              new DefaultFtpReply(
                  FtpReply.REPLY_125_DATA_CONNECTION_ALREADY_OPEN,
                  "Storing data with unique name: " + ids));
        }

        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Closing data connection"));
        LOGGER.debug("Sending FTP status code 226 to client - closing data connection");

      } catch (FtpException fe) {
        throw new FtpException("Failure to create metacard for file " + fileName, fe);
      } catch (Exception e) {
        throw new IOException("Error getting the output stream from FTP session", e);
      } finally {
        session.getDataConnection().closeDataConnection();
      }
    }

    return FtpletResult.SKIP;
  }

  private CreateStorageRequest getCreateStorageRequest(
      String fileName, TemporaryFileBackedOutputStream outputStream) throws IOException {
    String fileExtension = FilenameUtils.getExtension(fileName);
    String mimeType = getMimeType(fileExtension, outputStream);

    ContentItem newItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            outputStream.asByteSource(),
            mimeType,
            fileName,
            0L,
            null);

    return new CreateStorageRequestImpl(Collections.singletonList(newItem), null);
  }

  private List<Metacard> storeObject(
      Subject shiroSubject, String fileName, CreateStorageRequest createRequest) {
    return shiroSubject.execute(
        () -> {
          CreateResponse createResponse;
          try {
            createResponse = catalogFramework.create(createRequest);
          } catch (IngestException | SourceUnavailableException e) {
            throw new FtpException("Failed to ingest file: filename=" + fileName, e);
          }

          if (createResponse != null) {
            if (LOGGER.isDebugEnabled()) {
              createResponse
                  .getCreatedMetacards()
                  .forEach(
                      metacard ->
                          LOGGER.debug("Content item created with id = {}", metacard.getId()));
            }

            return createResponse.getCreatedMetacards();
          }

          return Collections.emptyList();
        });
  }

  /**
   * @param session The current {@link FtpSession}
   * @param request The current {@link FtpRequest}
   * @return {@link FtpletResult#SKIP} - signals successful ingest and to discontinue and further
   *     processing on the FTP request
   * @throws FtpException general exception for Ftplets
   * @throws IOException thrown when there is an error fetching data from client
   */
  @Override
  public FtpletResult onUploadStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return store(session, request, false);
  }

  String getMimeType(String fileExtension, TemporaryFileBackedOutputStream outputStream) {
    String mimeType = "";

    if (mimeTypeMapper != null) {
      try (InputStream inputStream = outputStream.asByteSource().openBufferedStream()) {
        if ("xml".equals(fileExtension)) {
          mimeType = mimeTypeMapper.guessMimeType(inputStream, fileExtension);
        } else {
          mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
        }

      } catch (MimeTypeResolutionException | IOException e) {
        LOGGER.info("Did not find the MimeTypeMapper service. Proceeding with empty mimetype");
      }
    } else {
      LOGGER.info("Did not find the MimeTypeMapper service. Proceeding with empty mimetype");
    }

    return mimeType;
  }

  @Override
  public FtpletResult onUploadEnd(FtpSession session, FtpRequest request) {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onDeleteStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(
        new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "DELE successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(
        new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "RETR successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onRmdirStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(
        new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "RMD successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onMkdirStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED, "MKD successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onAppendStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(
        new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "APPE successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return store(session, request, true);
  }

  @Override
  public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return onUploadEnd(session, request);
  }

  @Override
  public FtpletResult onRenameStart(FtpSession session, FtpRequest request)
      throws FtpException, IOException {

    FtpFile fromFtpFile = session.getRenameFrom();

    String toFilename = request.getArgument();

    if (isDotFile(fromFtpFile.getName())) {

      Optional<TemporaryFileBackedOutputStream> tfbosOpt =
          findTempFileInSession(session, fromFtpFile.getAbsolutePath());

      if (!tfbosOpt.isPresent()) {
        session.write(
            new DefaultFtpReply(
                FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                "file not found: " + fromFtpFile.getAbsolutePath()));
        return FtpletResult.SKIP;
      }

      try (TemporaryFileBackedOutputStream tfbos = tfbosOpt.get()) {

        Subject shiroSubject = (Subject) session.getAttribute(SUBJECT);
        if (shiroSubject == null) {
          return FtpletResult.DISCONNECT;
        }

        CreateStorageRequest createRequest = getCreateStorageRequest(toFilename, tfbos);

        storeObject(shiroSubject, toFilename, createRequest);

      } finally {
        removeTempFileFromSession(session, fromFtpFile.getAbsolutePath());
      }
    }

    session.write(
        new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, "RNTO successful"));
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    return FtpletResult.SKIP;
  }

  @Override
  public FtpletResult onSite(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    session.write(
        new DefaultFtpReply(FtpReply.REPLY_202_COMMAND_NOT_IMPLEMENTED, "SITE not implemented"));
    return FtpletResult.SKIP;
  }

  private void notNull(Object object, String name) {
    if (object == null) {
      throw new IllegalArgumentException(name + " cannot be null");
    }
  }

  @Override
  public FtpletResult beforeCommand(FtpSession session, FtpRequest request)
      throws FtpException, IOException {
    String command = request.getCommand().toUpperCase();

    if (command.equals(CWD)) {
      session.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, CWD));
      return FtpletResult.SKIP;
    }

    return super.beforeCommand(session, request);
  }
}
