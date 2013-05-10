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

package ddf.services.schematron;

import java.util.List;

import javax.xml.transform.TransformerException;

import org.w3c.dom.NodeList;


public interface SchematronReport 
{
	/** 
	 * Returns true if Schematron report is valid, false otherwise. The input document is considered
	 * to be valid if it has no failed assertions for errors and no failed reports
	 * for errors. If the suppressWarnings argument is true, then Schematron warnings are also included
	 * in the document's validity assessment. 
	 * 
	 * @param suppressWarnings do not include Schematron warnings in determining validity
	 * 
	 * @return true if no assert or report error messages found in SVRL report, false otherwise
	 */
	public boolean isValid( boolean suppressWarnings );
	
	
	/**
	 * Retrieve all assertion messages, warnings and errors, from the SVRL report.
	 * 
	 * @return list of XML Nodes for all assert nodes
	 */
	public NodeList getAllAssertMessages();
	
	
	/**
	 * Retrieve all report messages, warnings and errors, from the SVRL report.
	 * 
	 * @return list of XML Nodes for all report nodes
	 */
	public NodeList getAllReportMessages();
	
	
	/**
	 * Get a list of all of the assertion and report error messages from the SVRL report.
	 * 
	 * @return list of error strings
	 */
	public List<String> getErrors();
	
	
	/**
	 * Get a list of all of the assertion and report warning messages from the SVRL report.
	 * 
	 * @return list of warning strings
	 */
	public List<String> getWarnings();
	
	
	/**
	 * Retrieve the entire SVRL report as an XML-formatted string.
	 * 
	 * @return XML-formatted string representation of SVRL report
	 */
	public String getReportAsText() throws TransformerException;
	
}
