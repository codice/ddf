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

package ddf.catalog.pubsub.criteria.temporal;

import java.util.Date;

import org.apache.log4j.Logger;


public class TemporalEvaluator 
{
	private static Logger logger = Logger.getLogger( TemporalEvaluator.class );
	

	public static boolean evaluate( TemporalEvaluationCriteria tec ) 
	{
		String methodName = "evaluate";
		logger.debug( "ENTERING: " + methodName );
		
		boolean evaluation = false;

		if ( tec == null ) 
		{
			return evaluation ;
		}
		
		Date start = tec.getStart();
		Date end = tec.getEnd();
		Date input = tec.getInput();
		
		logger.debug( "start = " + start + ",   end = " + end + ",   input = " + input );

		
		if (start == null) 
		{
			logger.debug( "Doing start = null evaluation" );
			evaluation = input.before(end) || input.equals(end);
		} 
		else if (end == null) 
		{
			logger.debug( "Doing end = null evaluation" );
			evaluation = input.after(start) || input.equals(start);
		} 
		else if (input == null) 
		{
			logger.debug( "Doing input = null evaluation - return false" );
			return false;
		} 
		else 
		{
			logger.debug( "Doing start/end evaluation" );
			evaluation = (input.after(start) || input.equals(start))
					&& (input.before(end) || input.equals(end));
		}

		logger.debug( "evaluation = " + evaluation );
		
		logger.debug( "EXITING: " + methodName );
		
		return evaluation;
	}
	
}
