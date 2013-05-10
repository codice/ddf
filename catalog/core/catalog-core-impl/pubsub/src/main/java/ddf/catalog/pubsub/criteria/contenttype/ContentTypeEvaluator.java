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

package ddf.catalog.pubsub.criteria.contenttype;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ddf.catalog.pubsub.predicate.ContentTypePredicate;


public class ContentTypeEvaluator 
{
	private static Logger logger = Logger.getLogger( ContentTypeEvaluator.class );
	
	
	public static boolean evaluate( ContentTypeEvaluationCriteriaImpl ctec ) 
	{
	    String methodName = "evaluate";
	    logger.debug( "ENTERING: " + methodName );
	    
		ContentTypePredicate matchContentTypePredicate = ctec.getContentType();
		String matchType = matchContentTypePredicate.getType();
		String matchVersion = matchContentTypePredicate.getVersion();
		
		// the following block checks for asterisks in the Content Type and Version
		// if it has those, it replaces the asterisk with a dot then an asterisk.  
		// Thus the matchType and matchValue now can be used as regular expressions
		// to handle the wildcard.  
		if(matchType != null)
		{
			matchType = matchType.replaceAll("\\*", ".*");
		}
		if(matchVersion != null)
		{
			matchVersion = matchVersion.replaceAll("\\*", ".*");
		}
		
		String input = ctec.getInputContentType();
		if( logger.isDebugEnabled())
		{
		    logger.debug("Match ContentType: "  + matchContentTypePredicate);
		}
		
		String[] inputTypeVersionPair = input.split( "," );
		
		// All catalog entry inputs should have a type and version
		if ( inputTypeVersionPair.length != 2 ) 
	    {
		    logger.debug( "inputTypeVersionPair length = " + inputTypeVersionPair.length + " and should always be 2" );
		    logger.debug( "EXITING: " + methodName + " - returning false.  Invalid input."  );
		    return false;
	    }

		String inputType = inputTypeVersionPair[0];
		String inputVersion = inputTypeVersionPair[1];
		logger.debug( "inputType = " + inputType + ",   inputVersion = " + inputVersion );
		logger.debug( "matchType = " + matchType + ", matchVersion = " + matchVersion );

		if(matchType != null && !matchType.isEmpty() && inputType.matches(matchType))
		{
			
		    if(matchVersion != null && !matchVersion.isEmpty() && !inputVersion.matches(matchVersion))
		    {
		        logger.debug( "EXITING: " + methodName + " - returning false.  Did not match version." );
		        return false;		        
		    }
		    
		    logger.debug( "EXITING: " + methodName + " - returning true." );
		    return true;
		}
		
		
		
		// Loop through the content types that filter will match on
//		for ( Pair<String,Set<String>> pair : matchContentTypeSet )
//		{
//		    logger.debug( "Comparing inputType [" + inputType + "] to [" + pair.getFirstElement() + "]" );
//		    
//		    // First see if we have a match on the content type
//		    if ( pair.getFirstElement().equals( inputType ) )
//		    {
//		        // If matched content type pair specifies a version, then input version must match too
//		        Set<String> matchVersions = pair.getSecondElement();
//		        if ( matchVersions != null && !matchVersions.isEmpty() )
//		        {
//	                for ( String matchVersion : matchVersions )
//	                {
//                        logger.debug( "Comparing inputVersion [" + inputVersion + "] to [" + matchVersion + "]" );
//	                    if ( matchVersion.equals( inputVersion ) )
//	                    {
//	                        logger.debug( "EXITING: " + methodName + " - returning true" );
//	                        return true;
//	                    }
//	                }
//		        }
//		        else
//		        {
//		            // Only have to match on content type 
//		            // (no version specified to match on, so all versions match by default)
//		            logger.debug( "EXITING: " + methodName + " - returning true" );
//		            return true;
//		        }
//		    }
//		}
		
		logger.debug( "EXITING: " + methodName + " - returning false.");
		
		return false;
	}

}
