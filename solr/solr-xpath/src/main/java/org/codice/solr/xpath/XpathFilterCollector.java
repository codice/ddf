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
package org.codice.solr.xpath;

import java.io.IOException;

import lux.Config;
import lux.index.field.TinyBinaryField;
import lux.xml.tinybin.TinyBinary;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrException;
import org.apache.solr.search.DelegatingCollector;

/**
 * Collector that evaluates each Lucene document against a given XPath
 * and collects the results that match.
 */
public class XpathFilterCollector extends DelegatingCollector {

    public static final String LUX_XML_FIELD_NAME = "lux_xml";

    private final String xpath;

    private final XPathSelector selector;

    private final Configuration config;

    public XpathFilterCollector(String query) {
        xpath = query;

        config = new Config();
        Processor processor = new Processor(config);
        XPathCompiler compiler = processor.newXPathCompiler();

        try {
            selector = compiler.compile(xpath).load();
        } catch (SaxonApiException e) {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                    "Unable to compile xpath: " + query, e);
        }
    }

    @Override
    public void collect(int docId) throws IOException {
        Document doc = this.context.reader().document(docId);

        byte[] bytes = doc.getBinaryValue(LUX_XML_FIELD_NAME).bytes;

        // Assuming the lux_xml field is configured to use the Lux TinyBinary xml format in the
        // Lux update chain
        if (bytes.length > 4 && bytes[0] == 'T' && bytes[1] == 'I' && bytes[2] == 'N') {
            TinyBinary tb = new TinyBinary(bytes, TinyBinaryField.UTF8);
            XdmNode node = new XdmNode(tb.getTinyDocument(config));

            try {
                selector.setContextItem(node);
                XdmValue result = selector.evaluate();
                if (result.size() > 0) {
                    super.collect(docId);
                }
            } catch (SaxonApiException e) {
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
                        "Unable to evaluate xpath: " + xpath, e);
            }
        }
    }

}
