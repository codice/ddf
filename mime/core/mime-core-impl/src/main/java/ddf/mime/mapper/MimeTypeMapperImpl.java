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
package ddf.mime.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeResolver;


/**
 * Implementation of the {@link MimeTypeMapper} interface that searches through all of
 * the registered {@link MimeTypeResolver}s to retieve file extension for a given mime 
 * type, and vice versa. Once a file extension (or mime type) is resolved, this mapper
 * stops searching through any remaining {@link MimeTypeResolver}s and returns.
 * 
 * @since 2.1.0
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class MimeTypeMapperImpl implements MimeTypeMapper
{
    private static XLogger logger = new XLogger( LoggerFactory.getLogger( MimeTypeMapperImpl.class ) );
    
    private static Comparator<MimeTypeResolver> COMPARATOR = new Comparator<MimeTypeResolver>()
    {
        public int compare( MimeTypeResolver o1, MimeTypeResolver o2 )
        {
            return o1.getPriority() - o2.getPriority();
        }
    };
    
    
    /**
     * The {@link List} of {@link MimeTypeResolver}s configured for this mapper and 
     * will be searched on mime type/file extension mapping requests.
     */
    protected List<MimeTypeResolver> mimeTypeResolvers;
    
    protected MimeTypeResolver mimeTypeResolver;
    
    
    /**
     * Constructs the MimeTypeMapper with a list of {@link MimeTypeResolver}s.
     * 
     * @param mimeTypeResolvers the {@link List} of {@link MimeTypeResolver}s
     */
    public MimeTypeMapperImpl( List<MimeTypeResolver> mimeTypeResolvers )
    {
        logger.debug( "INSIDE: MimeTypeMapperImpl constructor" );
        this.mimeTypeResolvers = mimeTypeResolvers;
    }
    
    
    @Override
    public String getFileExtensionForMimeType( String mimeType ) throws MimeTypeResolutionException
    {
        logger.trace( "ENTERING: getFileExtensionForMimeType" );
        
        String extension = null;
        
        logger.debug( "Looping through " + mimeTypeResolvers.size() + " MimeTypeResolvers" );
        
        // Sort the mime type resolvers in descending order of priority. This should
        // insure custom mime type resolvers are called before the (default) Apache Tika
        // mime type resolver.
        List<MimeTypeResolver> sortedResolvers = sortResolvers( mimeTypeResolvers );
        
        // Loop through all of the configured MimeTypeResolvers. The order of their
        // invocation is determined by their OSGi service ranking. The default
        // TikaMimeTypeResolver should be called last, allowing any configured custom
        // mime type resolvers to be invoked first - this allows custom mime type
        // resolvers that may override mime types supported by Tika to be invoked first.
        // Once a file extension is find for the given mime type, exit the loop.
        for ( MimeTypeResolver resolver : sortedResolvers )
        {
            logger.debug( "Calling MimeTypeResolver " + resolver.getName() );
            try
            {
                extension = resolver.getFileExtensionForMimeType( mimeType );
            }
            catch ( Exception e )
            {
                logger.warn( "Error resolving file extension for mime type: " + mimeType );
                throw new MimeTypeResolutionException( e );
            }
            
            if ( extension != null && !extension.isEmpty() ) 
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "extension [" + extension + "] retrieved from MimeTypeResolver:  " + resolver.getName() );
                }
                break;
            }
        }
                
        logger.debug( "mimeType = " + mimeType + ",   file extension = [" + extension + "]" );
        
        logger.trace( "EXITING: getFileExtensionForMimeType" );
        
        return extension;
    }
    
    
    @Override
    public String getMimeTypeForFileExtension( String fileExtension ) throws MimeTypeResolutionException
    {
        logger.trace( "ENTERING: getMimeTypeForFileExtension" );
        
        String mimeType = null;
        
        logger.debug( "Looping through " + mimeTypeResolvers.size() + " MimeTypeResolvers" );
        
        //TODO: This is a KLUDGE to force the TikaMimeTypeResolver to be called
        // after the CustomMimeTypeResolvers to prevent Tika default mapping
        // from being used when a CustomMimeTypeResolver may be more appropriate.
        List<MimeTypeResolver> sortedResolvers = sortResolvers( mimeTypeResolvers );

        // Loop through all of the configured MimeTypeResolvers. The order of their
        // invocation is determined by their OSGi service ranking. The default
        // TikaMimeTypeResolver should be called last, allowing any configured custom
        // mime type resolvers to be invoked first - this allows custom mime type
        // resolvers that may override mime types supported by Tika to be invoked first.
        // Once a file extension is find for the given mime type, exit the loop.
        for ( MimeTypeResolver resolver : sortedResolvers )
        {
            logger.debug( "Calling MimeTypeResolver " + resolver.getName() );
            try
            {
                mimeType = resolver.getMimeTypeForFileExtension( fileExtension );
            }
            catch ( Exception e )
            {
                logger.warn( "Error resolving mime type for file extension: " + fileExtension );
                throw new MimeTypeResolutionException( e );
            }
            
            if ( mimeType != null && !mimeType.isEmpty() ) 
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "mimeType [" + mimeType + "] retrieved from MimeTypeResolver:  " + resolver.getName() );
                }
                break;
            }
        }
        
        logger.debug( "mimeType = " + mimeType + ",   file extension = [" + fileExtension + "]" );
        
        logger.trace( "EXITING: getMimeTypeForFileExtension" );
        
        return mimeType;
    }
    

    /**
     * Sort the list of {@link MimeTypeResolver}s by their descending priority, i.e., the
     * lower the priority the later the {@link MimeTypeResolver} is invoked.
     * 
     * @param resolvers the {@link List} of {@link MimeTypeResolver}s
     * @return the sorted list of {@link MimeTypeResolver}s by descending priority
     */
    private List<MimeTypeResolver> sortResolvers( List<MimeTypeResolver> resolvers ) 
    {
        logger.debug( "ENTERING: sortResolvers" );
        
        List<MimeTypeResolver> sortedResolvers = null;
        
        if ( resolvers != null )
        {       
            // Log sorted list of PreIngestServices for debugging
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Unsorted services" );
                logger.debug( "------------------" );
                
                for ( MimeTypeResolver resolver : resolvers )
                {   
                    logger.debug( resolver.getName() + "   (priority: " + resolver.getPriority() + ")" );
                }
            }
            
            // Make copy of input services list because OSGi/Blueprint marks this input list as read-only
            sortedResolvers = new ArrayList<MimeTypeResolver>( resolvers );
            
            // Inner class Comparator for comparing/sorting 
            Comparator<MimeTypeResolver> comparator = new Comparator<MimeTypeResolver>()
            {
                @Override
                public int compare( MimeTypeResolver arg0, MimeTypeResolver arg1 ) 
                {
                    logger.debug( "INSIDE: Comparator" );
                    return ( arg0.getPriority() - arg1.getPriority() );
                }
            };
            
            if ( sortedResolvers.size() > 1 )
            {
                logger.debug( "Sorting resolvers" );
                Collections.sort( sortedResolvers, comparator );
                Collections.reverse( sortedResolvers );
            }
            
            // Log sorted list of PreIngestServices for debugging
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Sorted/prioritized services" );
                logger.debug( "---------------------------" );
                
                for ( MimeTypeResolver resolver : sortedResolvers )
                {   
                    logger.debug( resolver.getName() + "   (priority: " + resolver.getPriority() + ")" );
                }
            }
        }
        
        logger.debug( "EXITING: sortResolvers" );
        
        return sortedResolvers;
    }    
}
