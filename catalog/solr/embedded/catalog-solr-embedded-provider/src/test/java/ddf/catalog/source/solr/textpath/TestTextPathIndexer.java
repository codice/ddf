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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.apache.log4j.Logger;
import org.codehaus.stax2.XMLInputFactory2;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.source.solr.Library;
import ddf.catalog.source.solr.textpath.TextPathIndexer;

public class TestTextPathIndexer {

	private static XMLInputFactory2 xmlInputFactory = null;
	
	private static final Logger LOGGER = Logger.getLogger(TestTextPathIndexer.class);

	@BeforeClass
	public static void setup() {
		xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
		xmlInputFactory.configureForSpeed();
	}

	@Test
	public void testIndexingRecord() {

		TextPathIndexer indexer = new TextPathIndexer(xmlInputFactory);

		List<String> output = indexer.indexTextPath(Library.getIndexingRecord());

		assertThat("The amount of leaves is incorrect.", output.size(), is(27));

		LOGGER.debug("OUTPUT");

		for (String o : output) {
			LOGGER.debug("[" + o + "]");
		}

	}

}
