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


public class TemporalEvaluationCriteriaImpl implements TemporalEvaluationCriteria 
{
	private Date end;
	private Date start;
	private Date input;

	
	public TemporalEvaluationCriteriaImpl( Date end, Date start, Date input ) 
	{
		super();
		this.end = end;
		this.start = start;
		this.input = input;
	}

	
	public Date getEnd() 
	{
		return end;
	}

	
	public Date getInput() 
	{
		return input;
	}

	
	public Date getStart() 
	{
		return start;
	}

}
