/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.content.provider.filesystem;

import java.io.File;
import java.io.IOException;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.data.ContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateResponseImpl;
import ddf.content.operation.impl.DeleteResponseImpl;
import ddf.content.operation.impl.ReadResponseImpl;
import ddf.content.operation.impl.UpdateResponseImpl;
import ddf.content.storage.StorageException;
import ddf.content.storage.StorageProvider;
import ddf.mime.MimeTypeMapper;


/**
 * The File System Provider provides the implementation to create/update/delete content items as 
 * files in the DDF Content Repository. The File System Provider is an implementation of the 
 * @link{StorageProvider} interface. When installed, it is invoked by the @link{ContentFramework} to 
 * create, update, or delete a file in the DDF Content Repository, which is located in the
 * <DDF_INSTALL_DIR>/content/store directory.
 * 
 * @author rodgersh
 * @author ddf.isgs@lmco.com
 *
 */
public class FileSystemProvider implements StorageProvider
{
    private static XLogger LOGGER = new XLogger( LoggerFactory.getLogger( FileSystemProvider.class ) );
    
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    
    public static final String CONTENT_URI_PREFIX = "content:";
    
    public static final String DEFAULT_CONTENT_REPOSITORY = "content" + File.separator + "store";
    
    /** Optional id parameter for mime type, e.g., text/xml;id=ddms20 */
    private static final String ID_PARAMETER = "id";
    
    /** Mapper for file extensions-to-mime types (and vice versa) */
    private MimeTypeMapper mimeTypeMapper;
    
    /** Root directory for entire content repository */
    private String baseContentDirectory;
    

    /**
     * Default constructor, invoked by blueprint.
     */
    public FileSystemProvider()
    {
        LOGGER.info("File System Provider initializing...");
    }
    
    
    @Override
    public CreateResponse create( CreateRequest createRequest ) throws StorageException
    {
        LOGGER.trace( "ENTERING: create" );
        
        ContentItem item = createRequest.getContentItem();
        
        ContentItem createdItem = null;
        try
        {
            // Create the root directory for entire content repository if does not
            // already exist.
            File parentDir = new File( baseContentDirectory );
            if ( !parentDir.exists() && !parentDir.mkdirs() )
            {
                throw new IOException( "Error creating content file system root directory" );
            }
    
            createdItem = generateContentFile( item );
        }
        catch (IOException e)
        {
            throw new StorageException(e);
        }
        
        CreateResponse response = new CreateResponseImpl( createRequest, createdItem );
        
        LOGGER.trace( "EXITING: create" );
        
        return response;
    }

    
    @Override
    public ReadResponse read( ReadRequest readRequest ) throws StorageException
    {
        LOGGER.trace( "ENTERING: read" );
        
        String id = readRequest.getId();
        File file = getFileForContentId(id);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Reading file " + file.getName() + " from directory " + id);
        }
        
        String extension = FilenameUtils.getExtension( file.getName() );

        String mimeType = DEFAULT_MIME_TYPE;
        try
        {
            mimeType = mimeTypeMapper.getMimeTypeForFileExtension( extension );
        }
        catch ( Exception e )
        {
            LOGGER.warn( "Could not determine mime type for file extension = " + extension + 
                "; defaulting to " + DEFAULT_MIME_TYPE );
        }

        LOGGER.debug( "mimeType = " + mimeType );
        ContentFile returnItem = new ContentFile( file, id, mimeType );
        ReadResponse response = new ReadResponseImpl( readRequest, returnItem );
        
        LOGGER.trace( "EXITING: read" );
        
        return response;
    }

    
    @Override
    public UpdateResponse update( UpdateRequest updateRequest ) throws StorageException
    {
        LOGGER.trace( "ENTERING: update" );
        
        ContentItem item = updateRequest.getContentItem();
        ContentItem updatedItem = null;
        LOGGER.debug( "Updating item with id = " + item.getId() );
        
        try
        {
            updatedItem = updateContentFile( item );
        }
        catch (IOException e)
        {
            throw new StorageException(e);
        }
        
        UpdateResponse response = new UpdateResponseImpl( updateRequest, updatedItem );
        
        LOGGER.trace( "EXITING: update" );
        
        return response;
    }

    
    @Override
    public DeleteResponse delete( DeleteRequest deleteRequest ) throws StorageException
    {
        LOGGER.trace( "ENTERING: delete" );
        
        ContentItem itemToBeDeleted = deleteRequest.getContentItem();
        String id = itemToBeDeleted.getId();
        
        boolean isDeleted = false;

        LOGGER.debug( "File to be deleted: " + id );
        File fileToBeDeleted = getFileForContentId(id);
        if ( !fileToBeDeleted.exists() )
        {
            throw new StorageException( "File doesn't exist for id: " + id );
        }

        ContentItem deletedContentItem = null;
        if ( !fileToBeDeleted.isDirectory() )
        {
            isDeleted = fileToBeDeleted.delete();
            if ( !isDeleted )
            {
                throw new StorageException( "Could not delete file: " + id );
            }
            else
            {
                // Delete parent directory (identified by contentId) since it is now empty
                // (always only one file per GUID directory) and will never be used again.
                File dirToBeDeleted = getDirectoryForContentId(id);
                try
                {
                    FileUtils.deleteDirectory(dirToBeDeleted);
                }
                catch (IOException e)
                {
                    LOGGER.info("Unable to delete directory " + dirToBeDeleted.getAbsolutePath() + " for id = " + id);
                }
                deletedContentItem = new ContentFile( null, id, itemToBeDeleted.getMimeTypeRawData() );
                String contentUri = CONTENT_URI_PREFIX + deletedContentItem.getId();
                LOGGER.debug("contentUri = " + contentUri);
                deletedContentItem.setUri(contentUri);
            }
        }
        else
        {
            throw new StorageException( "Invalid ID. Cannot delete directory." );
        }
        
        DeleteResponse response = new DeleteResponseImpl( deleteRequest, deletedContentItem, isDeleted );
        
        LOGGER.trace( "EXITING: delete" );
        
        return response;
    }

    
    private ContentItem generateContentFile( ContentItem item ) throws IOException, StorageException
    {
        LOGGER.trace( "ENTERING: generateContentFile" );
        
        String mimeType = getMimeType(item.getMimeType());
        
        String fileId = item.getId();
        if (!StringUtils.isEmpty(item.getFilename()))
        {
            fileId += File.separator + item.getFilename();
        }
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug( "itemId = " + item.getId() + 
                          ",   mimeType = " + mimeType + 
                          ",   itemFilename = " + item.getFilename() );
            LOGGER.debug( "fileId = " + fileId );
        }

        File createdFile = createFile( fileId ); 
        FileUtils.copyInputStreamToFile(item.getInputStream(), createdFile);
        
        ContentItem contentItem = new ContentFile( createdFile, item.getId(), item.getMimeTypeRawData(), item.getFilename() );
        String contentUri = CONTENT_URI_PREFIX + contentItem.getId();
        LOGGER.debug("contentUri = " + contentUri);
        contentItem.setUri(contentUri);

        LOGGER.trace( "EXITING: generateContentFile" );
        
        return contentItem;
    }

    
    private ContentItem updateContentFile( ContentItem item ) throws IOException, StorageException
    {
        LOGGER.trace( "ENTERING: updateContentFile" );
        
        String fileId = item.getId();
        LOGGER.debug( "File ID: " + fileId );

        File fileToUpdate = getFileForContentId(fileId);
        ContentItem contentItem = null;
        if ( fileToUpdate.exists() )
        {  
            FileUtils.copyInputStreamToFile(item.getInputStream(), fileToUpdate);
            
            contentItem = new ContentFile( fileToUpdate, item.getId(), item.getMimeTypeRawData() );
            String contentUri = CONTENT_URI_PREFIX + contentItem.getId();
            LOGGER.debug("contentUri = " + contentUri);
            contentItem.setUri(contentUri);
            LOGGER.debug( "updated file length = " + contentItem.getSize() );
        }
        else
        {
            String msg = "Unable to update - Content Item does not exist with id " + item.getId() + 
                         "   (fileId = " + fileId + ")";
            LOGGER.debug( msg );
            throw new StorageException( msg );
        }

        LOGGER.trace( "EXITING: updateContentFile" );
        
        return contentItem;
    }

    
    private File createFile( final String newFileID ) throws IOException
    {
        LOGGER.trace( "ENTERING: createFile" );
        
        File file = getFileFromContentRepository( newFileID );
        
        // create directories
        File directory = file.getParentFile();
        if ( !directory.exists() && !directory.mkdirs() )
        {
            throw new IOException( "Error creating directory structure to save file." );
        }

        // create file and write to it, if it doesn't already exist
        if ( !file.exists() && !file.createNewFile() )
        {
            throw new IOException( "Error creating file: " + file.getAbsolutePath() );
        }

        LOGGER.trace( "EXITING: createFile" );
        
        return file;
    } 

    
    private File getFileFromContentRepository( String id )
    {
        LOGGER.trace( "ENTERING: getFileFromContentRepository" );
        LOGGER.debug( "id = " + id );
        
        File baseURIFile = null;
        
        if ( id != null && !id.isEmpty() )
        {       
            // Normalize and concatenate the paths
            String filepath = FilenameUtils.concat( baseContentDirectory, removeSlashPrefix( id ) );
            LOGGER.debug( "Full filepath = " + filepath );
            baseURIFile = new File( filepath );
        } 
        else 
        {
            LOGGER.warn( "Could not obtain reference to content item. Possibly invalid content id: " + id );
        }
        
        LOGGER.trace( "EXITING: getFileFromContentRepository" );
        
        return baseURIFile;
    }    
    
    
    private String removeSlashPrefix( final String path ) 
    {
        String newPath = path;
        char firstChar = path.charAt(0);
        
        if (firstChar == '/' || firstChar == '\\' ) 
        {
            newPath = path.substring(1);
        }
    
        return newPath;
    }
    
    
    public MimeTypeMapper getMimeTypeMapper()
    {
        return mimeTypeMapper;
    }

    
    public void setMimeTypeMapper( MimeTypeMapper mimeTypeMapper ) 
    {
        this.mimeTypeMapper = mimeTypeMapper;
    }
    
    
    public String getBaseContentDirectory()
    {
        return baseContentDirectory;
    }

    
    public void setBaseContentDirectory( final String baseDirectory ) 
    {   
        String newBaseDir = "";
        
        if (!baseDirectory.isEmpty()) 
        {
            String path = FilenameUtils.normalize(baseDirectory);
            File directory = new File(path);
            
            // Create the directory if it doesn't exist
            if ( (!directory.exists() && directory.mkdirs()) || 
                 (directory.isDirectory() && directory.canRead()) ) 
            {
                LOGGER.info("Setting base content directory to: " + path);
                newBaseDir = path;
            }
        }
        
        // if invalid baseDirectory was provided or baseDirectory is 
        // an empty string, default to the DEFAULT_CONTENT_REPOSITORY in <karaf.home>
        if (newBaseDir.isEmpty()) 
        {
            try 
            {
                final File karafHomeDir = new File(System.getProperty("karaf.home"));       
                
                if (karafHomeDir.isDirectory()) 
                {
                    final File fspDir = new File(karafHomeDir
                            + File.separator + DEFAULT_CONTENT_REPOSITORY);
                    
                    // if directory does not exist, try to create it
                    if (fspDir.isDirectory() || fspDir.mkdirs()) {
                        LOGGER.info("Setting base content directory to: " + fspDir.getAbsolutePath());
                        newBaseDir = fspDir.getAbsolutePath();
                    } 
                    else 
                    {
                        LOGGER.warn("Unable to create FileSystemProvider folder: "
                                + fspDir.getAbsolutePath()
                                + ". Please check that DDF has permissions to create this folder.  Using default folder.");
                    }
                } 
                else 
                {
                    LOGGER.warn("Karaf home folder defined by system property karaf.home is not a directory.  Using default folder.");
                }
            } 
            catch (NullPointerException npe) 
            {
                LOGGER.warn("Unable to create FileSystemProvider folder - karaf.home system property not defined. Using default folder.");
            }
        } 
        
        this.baseContentDirectory = newBaseDir;
        
        if ( LOGGER.isDebugEnabled() ) 
        {
            LOGGER.debug( "Set base content directory to: " + this.baseContentDirectory );
        }
    }
    
    
    private String getMimeType(MimeType mimeType)
    {
        LOGGER.trace("ENTERING: getMimeType");
        
        String mimeTypeStr = mimeType.getBaseType();
        String mimeTypeIdValue = mimeType.getParameter(ID_PARAMETER);
        if (!StringUtils.isEmpty(mimeTypeIdValue))
        {
            mimeTypeStr += ";id=" + mimeTypeIdValue;
        }
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("mimeTypeStr = " + mimeTypeStr);
        }
        
        LOGGER.trace("EXITING: getMimeType");
        
        return mimeTypeStr;
    }
    
    
    private File getDirectoryForContentId(String contentId) throws StorageException
    {
        LOGGER.trace("ENTERING: getDirectoryForContentId");
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("contentId = " + contentId);
        }
        
        File dir = getFileFromContentRepository( contentId );
        if ( !dir.exists() || !dir.isDirectory() )
        {
            throw new StorageException("Directory does not exist in content repository with id = " + contentId);
        }   
        
        LOGGER.trace("EXITING: getDirectoryForContentId");
        
        return dir;
    }
    
    
    private File getFileForContentId(String contentId) throws StorageException
    {
        LOGGER.trace("ENTERING: getFileFromContentId");
        
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("contentId = " + contentId);
        }
        
        File dir = getDirectoryForContentId( contentId );        
        File[] files = dir.listFiles();
        if (files.length == 0)
        {
            throw new StorageException("No files in directory " + contentId);
        }        
        else if (files.length > 1)
        {
            throw new StorageException("More than one file in directory " + contentId + " - cannot determine which file to work on.");
        }
        
        LOGGER.trace("EXITING: getFileFromContentId");
        
        return files[0];
    }
}
