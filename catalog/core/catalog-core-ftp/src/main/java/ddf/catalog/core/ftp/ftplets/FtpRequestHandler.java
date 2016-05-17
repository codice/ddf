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
package ddf.catalog.core.ftp.ftplets;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.core.ftp.user.FtpUser;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

/**
 * Handler for incoming FTP requests from the client. Supports the PUT and MPUT operations - all other operations respond with 550 request not taken response.
 * When PUTing a new file, it is not temporarily written to the file system. Instead, the new file's data stream is ingested directly into the catalog.
 */
public class FtpRequestHandler extends DefaultFtplet {
    private static final Logger LOGGER = LoggerFactory.getLogger(FtpRequestHandler.class);

    private static final String UNSUPPORTED_METHOD_RESPONSE =
            "Unsupported method, only PUT and MPUT are supported.";

    private static final String SUBJECT = "subject";

    private static final String STOR_REQUEST = "STOR";

    private CatalogFramework catalogFramework;

    private MimeTypeMapper mimeTypeMapper;

    public FtpRequestHandler(CatalogFramework catalogFramework, MimeTypeMapper mimeTypeMapper) {
        notNull(catalogFramework, "catalogFramework");
        notNull(mimeTypeMapper, "mimeTypeMapper");

        this.catalogFramework = catalogFramework;
        this.mimeTypeMapper = mimeTypeMapper;
    }

    @Override
    public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
        return FtpletResult.DEFAULT;
    }

    @Override
    public FtpletResult onLogin(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.setAttribute(SUBJECT, ((FtpUser) session.getUser()).getSubject());
        return FtpletResult.SKIP;
    }

    /**
     * @param session The current {@link FtpSession}
     * @param request The current {@link FtpRequest}
     * @return {@link FtpletResult#SKIP} - signals successful ingest and to discontinue and further processing on the FTP request
     * @throws FtpException general exception for Ftplets
     * @throws IOException  thrown when there is an error fetching data from client
     */
    @Override
    public FtpletResult onUploadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {

        LOGGER.debug("Beginning FTP ingest of {}", request.getArgument());

        Subject shiroSubject = (Subject) session.getAttribute(SUBJECT);
        if (shiroSubject != null) {
            FtpFile ftpFile = null;
            String fileName = request.getArgument();

            try {
                ftpFile = session.getFileSystemView()
                        .getFile(fileName);
            } catch (FtpException e) {
                LOGGER.error("Failed to retrieve file from FTP session");
            }

            if (ftpFile == null) {
                LOGGER.error(
                        "Sending FTP status code 501 to client - syntax errors in request parameters");
                session.write(new DefaultFtpReply(FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                        STOR_REQUEST));

                throw new FtpException("File to be transferred from client did not exist");
            }

            DataConnectionFactory connFactory = session.getDataConnection();
            if (connFactory instanceof IODataConnectionFactory) {
                InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();
                if (address == null) {
                    session.write(new DefaultFtpReply(FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                            "PORT or PASV must be issued first"));
                    LOGGER.error(
                            "Sending FTP status code 503 to client - PORT or PASV must be issued before STOR");
                    throw new FtpException("FTP client address was null");
                }
            }

            if (!ftpFile.isWritable()) {
                session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                        "Insufficient permissions"));
                LOGGER.error(
                        "Sending FTP status code 550 to client - insufficient permissions to write file.");
                throw new FtpException("Insufficient permissions to write file");
            }

            session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY,
                    STOR_REQUEST + " " + fileName));
            LOGGER.debug("Replying to client with code 150 - file status okay");

            try (FileBackedOutputStream outputStream = new FileBackedOutputStream(1000000);
                    final AutoCloseable ac = outputStream::reset) {
                DataConnection dataConnection = connFactory.openConnection();
                dataConnection.transferFromClient(session, outputStream);

                session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                        "Closing data connection"));
                LOGGER.debug("Sending FTP status code 226 to client - closing data connection");

                String fileExtension = FilenameUtils.getExtension(ftpFile.getAbsolutePath());
                String mimeType = getMimeType(fileExtension, outputStream);

                ContentItem newItem = new ContentItemImpl(outputStream.asByteSource(),
                        mimeType,
                        fileName,
                        null);

                CreateStorageRequest createRequest =
                        new CreateStorageRequestImpl(Collections.singletonList(newItem), null);
                createRequest.getProperties()
                        .put(SecurityConstants.SECURITY_SUBJECT, shiroSubject);

                CreateResponse createResponse = catalogFramework.create(createRequest);
                if (createResponse != null) {
                    List<Metacard> createdMetacards = createResponse.getCreatedMetacards();

                    for (Metacard metacard : createdMetacards) {
                        LOGGER.info("Content item created with id = {}", metacard.getId());
                    }
                } else {
                    throw new FtpException();
                }
            } catch (SourceUnavailableException | IngestException e) {
                LOGGER.error("Failure to ingest file {}", fileName, e);
                throw new FtpException("Failure to ingest file " + fileName);
            } catch (FtpException fe) {
                LOGGER.error("Failure to create metacard for file {}", fileName);
                throw new FtpException("Failure to create metacard for file " + fileName);
            } catch (Exception e) {
                LOGGER.error("Error getting the output data stream from the FTP session");
                throw new IOException("Error getting the output stream from FTP session");
            } finally {
                session.getDataConnection()
                        .closeDataConnection();
            }
        }
        return FtpletResult.SKIP;
    }

    protected String getMimeType(String fileExtension, FileBackedOutputStream outputStream) {
        String mimeType = "";

        if (mimeTypeMapper != null) {
            try (InputStream inputStream = outputStream.asByteSource()
                    .openBufferedStream()) {
                if (fileExtension.equals("xml")) {
                    mimeType = mimeTypeMapper.guessMimeType(inputStream, fileExtension);
                } else {
                    mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                }

            } catch (MimeTypeResolutionException | IOException e) {
                LOGGER.warn(
                        "Did not find the MimeTypeMapper service. Proceeding with empty mimetype");
            }
        } else {
            LOGGER.warn("Did not find the MimeTypeMapper service. Proceeding with empty mimetype");
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
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onDeleteEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onDownloadStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onDownloadEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onRmdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onRmdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onMkdirStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onMkdirEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onAppendStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onAppendEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onUploadUniqueStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onUploadUniqueEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onRenameStart(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onRenameEnd(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    @Override
    public FtpletResult onSite(FtpSession session, FtpRequest request)
            throws FtpException, IOException {
        session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                UNSUPPORTED_METHOD_RESPONSE));
        return FtpletResult.SKIP;
    }

    private void notNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }
}
