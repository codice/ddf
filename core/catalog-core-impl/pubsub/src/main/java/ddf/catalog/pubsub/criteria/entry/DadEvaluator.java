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

package ddf.catalog.pubsub.criteria.entry;

import java.net.URI;

import org.apache.log4j.Logger;

public final class DadEvaluator 
{
    private static Logger logger = Logger.getLogger( DadEvaluator.class );
    
    private DadEvaluator()
    {
        throw new UnsupportedOperationException( "This is a utility class - it should never be instantiated" );
    }
    
    
	public static boolean evaluate( DadEvaluationCriteria dec ) 
	{
	    String methodName = "evaluate";
	    logger.debug( "ENTERING: " + methodName );
	    
	    boolean status = false;
	    
	    URI inputDad = dec.getInputDad();
	    
        if ( inputDad != null )
        {
              logger.debug( "inputDad = " + inputDad.toString().toString() );
              logger.debug( "reference DAD = " + dec.getDad().toString() );
              
              if ( inputDad.compareTo( dec.getDad() ) == 0 )
              {
                  status = true;
              }

        }
        else
        {
            logger.debug(  "inputDad is NULL" );
        }
        
        logger.debug( "status = " + status );
        
        logger.debug( "EXITING: " + methodName );
        
        return status;
	}

}
