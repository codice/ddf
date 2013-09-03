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

package ddf.catalog.pubsub.predicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
import org.osgi.service.event.Event;
import org.w3c.dom.Document;

import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluationCriteria;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;


public class ContextualPredicate implements Predicate
{
	private String searchPhrase;
	private boolean fuzzy;
	private boolean caseSensitiveSearch;
	private Collection<String> textPaths;
	
	
	private static final Logger logger = Logger.getLogger( ContextualPredicate.class );

	
	public ContextualPredicate( String searchPhrase, boolean fuzzy, boolean caseSensitiveSearch, Collection<String> textPaths ) 
	{
		this.fuzzy = fuzzy;
		this.caseSensitiveSearch = caseSensitiveSearch;
		
		if ( textPaths != null && !textPaths.isEmpty() ) 
		{
           logger.debug( "text paths size: " + textPaths.size() );
           this.textPaths = new ArrayList<String>( textPaths );
       }
	   //this.searchPhrase = searchPhrase;	
	   this.searchPhrase = normalizePhrase( searchPhrase, fuzzy );
	}

	
	public boolean matches( Event properties ) 
	{
		String methodName = "matches";
		logger.debug( "ENTERING: " + methodName );
		
		logger.debug( "Headers: " + properties );
		
		ContextualEvaluationCriteria cec = null;
		Map<String, Object> contextualMap = (Map<String, Object>) properties.getProperty( PubSubConstants.HEADER_CONTEXTUAL_KEY );
		
		if(contextualMap == null){
		    logger.debug("No contextual metadata to search against.");
		    return false;
		}
		
		String operation = (String) properties.getProperty( PubSubConstants.HEADER_OPERATION_KEY );
		logger.debug( "operation = " + operation );
		String metadata = (String) contextualMap.get( "METADATA" );
		logger.debug( "metadata = [" + metadata + "]" );
		
		// If deleting a catalog entry and the entry's metadata is only the word "deleted" (i.e., the
		// source is deleting the catalog entry and did not send any metadata with the delete event), then
		// cannot apply any contextual filtering - just send the event on to the subscriber
		if ( operation.equals( PubSubConstants.DELETE ) && metadata.equals(PubSubConstants.METADATA_DELETED ) )
		{
		    logger.debug( "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber" );
		    return true;
		}
		
		// If predicate specified one or more text paths, then extract the entry's metadata from the Event properties and
		// pass it and the text path(s) to the evaluation criteria (which will build a Lucene index on the metadata using the
		// text paths)
		if ( this.textPaths != null && !this.textPaths.isEmpty() )
		{
		    logger.debug( "creating criteria with textPaths and metadata document" );
		    try
		    {
		        cec = new ContextualEvaluationCriteriaImpl( searchPhrase, fuzzy, caseSensitiveSearch,
		            (String[]) this.textPaths.toArray( new String[this.textPaths.size()] ), (String) contextualMap.get( "METADATA" ) );
    		} 
            catch ( IOException e ) 
            {
                logger.error( e );
                return false;
            }
		}
		
		// This predicate has no text paths specified, so can use default Lucene search index, which indexed the entry's entire metadata
		// per the default XPath expressions in ContextualEvaluator, from the event's properties data
		else
		{
		    logger.debug( "using default Lucene search index for metadata" );
		    cec = new ContextualEvaluationCriteriaImpl( searchPhrase, fuzzy, caseSensitiveSearch, (Directory) contextualMap.get( "DEFAULT_INDEX" ) );
		}
		
		try 
		{
			return ContextualEvaluator.evaluate( cec );
		} 
		catch ( IOException e ) 
		{
			logger.error( e );
		} 
		catch ( ParseException e ) 
		{
			logger.error( e );
		}
		
		logger.debug( "EXITING: " + methodName );
		
		return false;
	}
	
	
	public static boolean isContextual( String searchPhrase ) 
	{
		return !searchPhrase.isEmpty();
	}
	
	
	public String getSearchPhrase() 
	{
		return searchPhrase;
	}
   
    
    public boolean isFuzzy() 
    {
        return fuzzy;
    }
   
    
    public boolean isCaseSensitive() 
    {
        return caseSensitiveSearch;
    }
   
    
    public boolean hasTextPaths() 
    {
        return textPaths != null && !textPaths.isEmpty();
    }
   
    
    public Collection<String> getTextPaths() 
    {
        return textPaths;
    }


    /**
     * Normalizes a search phrase for a Lucene query
     * 
     * @param inputPhrase the input phrase
     * @param isFuzzy true indicates the criteria is fuzzy
     * 
     * @return a search phrase aligned to Lucene syntax
     */
    public static String normalizePhrase( String inputPhrase, boolean isFuzzy )
    {
        String phrase = inputPhrase.trim();
        String parts[] = phrase.split( "\"" );
        logger.debug( "phrase = [" + phrase + "]    parts.length = " + parts.length );

        if ( inputPhrase != null && !inputPhrase.equals( "" ) )
        {
            // if multiple parts found, then exact (quoted) phrases are present
            if ( parts.length > 1 )
            { 
                // Odd parts are in quotes, i.e., exact (quoted) phrases, so skip them
                // Even parts are individual words or operators
                for( int i = 0; i < parts.length; i++ )
                {
                    logger.debug( "parts[" + i + "] = " + parts[i] );
                    if ( i % 2 == 0 )
                    {
                        if ( !parts[i].isEmpty() )
                        {
                            parts[i] = normalizeBooleanOperators( parts[i] );
                            
                            if ( isFuzzy && !isBooleanOperator( parts[i] ) ) 
                            {
                                parts[i] = parts[i] + "~";
                                parts[i] = parts[i].replace( "~~", "~" );
    
                                logger.debug( "Fuzzy Search adding a tilde: " + parts[i] );
                            }
                        }
                        else
                        {
                            logger.debug( "part[" + i + "] was empty" );
                        }
                    }
                }
                
                phrase = "";
                for( int i = 0; i < parts.length; i++ )
                {
                    phrase = phrase + parts[i];
                    if ( i < ( parts.length - 1 ) ) phrase = phrase + "\"";
                }
            }
            else
            {
                logger.debug( "parts.length <= 1:  phrase = " + phrase );
                phrase = normalizeBooleanOperators( phrase );
                
//                if ( isFuzzy && !isBooleanOperator( phrase ) ) 
//                {
//                    phrase = phrase.trim().replace(" ", "~ ");
//
//                    // add to last word
//                    phrase = phrase + "~";
//                    phrase = phrase.replace( "~~", "~" );
//
//                    logger.debug( "Fuzzy Search adding a tilde: " + phrase );
//                }
                if ( isFuzzy ) 
                {
                    String[] words = phrase.trim().split( "[ ]+" );
                    for ( int i=0; i < words.length; i++ )
                    {
                        String[] subParts = words[i].split( "[\\(\\)]+" );
                        for ( String subPart : subParts )
                        {
                            if ( !subPart.isEmpty() && !isBooleanOperator( subPart ) )
                            {
                                String fuzzySubPart = subPart + "~";
                                phrase = phrase.replaceFirst( subPart, fuzzySubPart );
                                logger.debug( "2. Fuzzy Search adding a tilde: " + subPart );
                                logger.debug( "phrase = " + phrase );
                            }
                        }
                        
                        phrase = phrase.replace( "~~", "~" );
                    }
                    
                    // append a tilde to each word in the phrase
                    //phrase = phrase.trim().replace(" ", "~ ");

                    // add tilde to last word in entire search phrase
                    //phrase = phrase + "~";

                    logger.debug( "2. Fuzzy-fied phrase: " + phrase );
                }
            }
/*
            if ( isFuzzy )
            {
                phrase = phrase.trim().replace( " ", "~ " );

                // add to last word
                phrase = phrase + "~";

                logger.debug( "Fuzzy Search adding a tilde: " + phrase );

                phrase = phrase.replace( " NOT~ ", " NOT " );
                phrase = phrase.replace( " AND~ ", " AND " );
                phrase = phrase.replace( " OR~ ", " OR " );
                phrase = phrase.replace( "~~", "~" );
            }
*/
            // The keyword NOT should not be the last word in the search phrase
//            if ( phrase.length() > 3 )
//            {
//                if ( phrase.lastIndexOf( "NOT" ) == phrase.length() - 4 )
//                {
//                    phrase = phrase.substring( 0, phrase.length() - 4 );
//                    phrase = phrase.concat( "~" );
//                }
//            }

            // Pass thru the last literal double quote
            if ( inputPhrase.lastIndexOf( "\"" ) == inputPhrase.length() - 1 )
            {
                phrase = phrase + "\"";
            }
        }
        else
        {
            phrase = "";
        }
        logger.debug( "Normalization complete. \nBefore: " + inputPhrase + "\nAfter: " + phrase );

        return phrase;
    }
    
    
    /**
     * Normalize all Boolean operators in the phrase since Lucene grammar requires all 
     * boolean operators to be uppercase.
     * 
     * @param phrase the input phrase
     * 
     * @return the normalized phrase
     */
    private static String normalizeBooleanOperators( String phrase )
    {
        //phrase = phrase.replace( "~", "NOT " );
        phrase = phrase.replace( " not ", " NOT " );
        phrase = phrase.replace( " or ", " OR " );
        phrase = phrase.replace( " and ", " AND " );
        phrase = phrase.replace( "&", "AND" );
        phrase = phrase.replace( "|", "OR" );
        
        return phrase;
    }
    
    
    private static boolean isBooleanOperator( String input )
    {
        int index = StringUtils.indexOfAny( input.trim().toLowerCase(), 
            new String[] { "not", "and", "or", "&", "|" } );
        
        return index == 0;
    }
    
    
	public String toString()
	{
	    return ToStringBuilder.reflectionToString( this );
	}
	
}
