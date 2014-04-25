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

package ddf.catalog.pubsub.criteria.contenttype;

import org.apache.log4j.Logger;

import ddf.catalog.pubsub.predicate.ContentTypePredicate;

public class ContentTypeEvaluator {
    private static Logger logger = Logger.getLogger(ContentTypeEvaluator.class);

    public static boolean evaluate(ContentTypeEvaluationCriteriaImpl ctec) {
        String methodName = "evaluate";
        logger.debug("ENTERING: " + methodName);

        ContentTypePredicate matchContentTypePredicate = ctec.getContentType();
        String matchType = matchContentTypePredicate.getType();
        String matchVersion = matchContentTypePredicate.getVersion();
               
        // the following block checks for asterisks in the Content Type and Version
        // if it has those, it replaces the asterisk with a dot then an asterisk.
        // Thus the matchType and matchValue now can be used as regular expressions
        // to handle the wildcard.
        if (matchType != null) { 
        	matchType = matchType.replaceAll("\\*", ".*");
        }
        
        if (matchVersion != null) {
        	matchVersion = matchVersion.replaceAll("\\*", ".*");
        }

        String input = ctec.getInputContentType();
        if (logger.isDebugEnabled()) {
            logger.debug("Match ContentType: " + matchContentTypePredicate);
        }
        
        String inputType;
        String inputVersion;
        
        //Check if both type and version are blank
        if (input == null || input.matches(",")) {
        	inputType = "null";
        	inputVersion = "null";
        }
        else {
        	String[] inputTypeVersionPair = input.split(",");
        	
        	//Check if content type is blank. If yes, set to null.
        	if (inputTypeVersionPair[0].isEmpty()) {
        		inputType = "null";
        	}
        	else {
        		inputType = inputTypeVersionPair[0];
        	}
            
            //Check if version is blank. If yes, set to null.
            if (inputTypeVersionPair.length == 1) {
            	inputVersion = "null";
            }
            else {
            	inputVersion = inputTypeVersionPair[1];
            }
        }
        	
        logger.debug("inputType = " + inputType + ", inputVersion = " + inputVersion);
        logger.debug("matchType = " + matchType + ", matchVersion = " + matchVersion);

        if (matchType != null && !matchType.isEmpty() && inputType.matches(matchType)) {

            if (matchVersion != null && !matchVersion.isEmpty()
                    && !inputVersion.matches(matchVersion)) {
                logger.debug("EXITING: " + methodName
                        + " - returning false.  Did not match version.");
                return false;
            }

            logger.debug("EXITING: " + methodName + " - returning true.");
            return true;
        }

        logger.debug("EXITING: " + methodName + " - returning false.");

        return false;
    }

}
