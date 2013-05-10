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
package ddf.catalog.source.solr.textpath;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;

/**
 * Mostly used to aggregate xml leaf text. Parsing using STAX makes it difficult
 * to know when you have reached a leaf node of a xml tree. This tracks that
 * state and tracks the xml leaf element value text.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TextPathState {

	private List<String> values;

	private boolean observedStartElement = false;

	private String possibleLeafText;

	private String currentPath;

	public TextPathState() {
		values = new ArrayList<String>();
	}

	public List<String> getTextPathValues() {
		return this.values;
	}

	/**
	 * Called when you have encountered a start element
	 * 
	 * @param currentPath
	 *            the current path up to this point including this element.
	 */
	public void startElement(String currentPath) {
		observedStartElement = true;
		this.currentPath = currentPath;
		reset();
	}

	/**
	 * Called when {@link XMLStreamConstants#CHARACTERS} have been encountered
	 * 
	 * @param text
	 *            the {@link XMLStreamConstants#CHARACTERS} text
	 */
	public void setCharacters(String text) {

		if (text != null) {
			possibleLeafText = possibleLeafText + text.trim();
		}
	}

	/**
	 * Called when you have reached the end of an element
	 */
	public void endElement(char seperator) {

		if (observedStartElement) {
			values.add(currentPath + seperator + possibleLeafText);
			reset();
			observedStartElement = false;
		}
	}

	protected void reset() {
		possibleLeafText = "";
	}

}
