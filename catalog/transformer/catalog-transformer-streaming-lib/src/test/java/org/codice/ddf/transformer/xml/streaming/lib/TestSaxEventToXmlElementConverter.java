/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.xml.sax.SAXException;

public class TestSaxEventToXmlElementConverter {

    @Test
    public void testSaxEventToXmlElementConverterRedeclaredNamespaceUri()
            throws XMLStreamException, IOException, SAXException {
        String doubleDeclaredNamespaceUriSnippet =
                "<x xmlns:n1=\"foobar\" \n" + "   xmlns=\"foobar\">\n"
                        + "  <good a=\"1\"     b=\"2\" />\n" + "  <good a=\"1\"     n1:a=\"2\" />\n"
                        + "</x>";

        String reconstructedExpectation =
                "<x xmlns=\"foobar\">\n  <good a=\"1\" b=\"2\"></good>\n  <good a=\"1\" xmlns:ns1=\"foobar\" ns1:a=\"2\"></good>\n</x>";
        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(doubleDeclaredNamespaceUriSnippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

    // This is broken
    @Test
    public void testSaxEventToXmlElementConverterRedeclaredNamespaceUriVariation()
            throws XMLStreamException, IOException, SAXException {
        String doubleDeclaredNamespaceUriSnippet =
                "<x:y xmlns=\"default\" xmlns:x=\"www.x.com\" x:one=\"2\" x:two=\"2\">\n"
                        + "    <y:z xmlns:y=\"www.x.com\" x:one=\"1\" y:two=\"2\">\n"
                        + "     <x:z>abcdefg</x:z>\n" + "    </y:z>\n" + "</x:y>";

        String reconstructedExpectation = "<x:y xmlns:x=\"www.x.com\" x:one=\"2\" x:two=\"2\">\n"
                + "    <y:z xmlns:y=\"www.x.com\" x:one=\"1\" y:two=\"2\">\n"
                + "     <x:z>abcdefg</x:z>\n" + "    </y:z>\n" + "</x:y>";
        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(doubleDeclaredNamespaceUriSnippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

    @Test
    public void testSaxEventToXmlElementConverterRedeclaredNamespaceUriVariation2()
            throws XMLStreamException, IOException, SAXException {
        String snippet = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
                + "<outer xmlns:aaa=\"whocares\" xmlns=\"http://www.w3.com\">\n"
                + "    <aaa:foo name=\"outside\">\n" + "        <aaa:bar xmlns:bbb=\"inside1\">\n"
                + "            <bbb:baz xmlns:aaa=\"inside2\">\n"
                + "                <aaa:verybad name=\"scope matters\"/>\n"
                + "            </bbb:baz>\n" + "        </aaa:bar>\n" + "    </aaa:foo>\n"
                + "</outer>";

        /*
         * The reconstructed slightly different than the original snippet:
         *   - it's missing the xml version declaration
         *   - it has the aaa and bbb namespace declared later, however this is still perfectly valid
         */
        String reconstructedExpectation = "<outer xmlns=\"http://www.w3.com\">\n"
                + "    <aaa:foo xmlns:aaa=\"whocares\" name=\"outside\">\n" + "        <aaa:bar>\n"
                + "            <bbb:baz xmlns:bbb=\"inside1\">\n"
                + "                <aaa:verybad xmlns:aaa=\"inside2\" name=\"scope matters\"></aaa:verybad>\n"
                + "            </bbb:baz>\n" + "        </aaa:bar>\n" + "    </aaa:foo>\n"
                + "</outer>";

        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(snippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

    @Test
    public void testSaxEventToXmlConverterNormal()
            throws XMLStreamException, IOException, SAXException {
        String normalSnippet = "<?xml version=\"1.0\"?>\n"
                + "<!-- both namespace prefixes are available throughout -->\n"
                + "<bk:book xmlns:bk='urn:loc.gov:books'\n"
                + "         xmlns:isbn='urn:ISBN:0-395-36341-6'>\n"
                + "    <bk:title>Cheaper by the Dozen</bk:title>\n"
                + "    <isbn:number>1568491379</isbn:number>\n" + "</bk:book>";

        /*
         * The reconstructed slightly different than the original snippet:
         *   - it's missing the xml version declaration
         *   - it's missing the comment
         *   - it has the isbn namespace declared later, however this is still perfectly valid
         */
        String reconstructedExpectation = "<bk:book xmlns:bk=\"urn:loc.gov:books\">\n"
                + "    <bk:title>Cheaper by the Dozen</bk:title>\n"
                + "    <isbn:number xmlns:isbn=\"urn:ISBN:0-395-36341-6\">1568491379</isbn:number>\n"
                + "</bk:book>";

        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(normalSnippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

    @Test
    public void testSaxEventToXmlConverterScopedPrefixRedeclaration()
            throws XMLStreamException, IOException, SAXException {
        String snippet = "<?xml version=\"1.0\"?>\n"
                + "<!-- initially, the default namespace is \"books\" -->\n"
                + "<book xmlns='urn:loc.gov:books'\n"
                + "      xmlns:isbn='urn:ISBN:0-395-36341-6'>\n"
                + "    <title>Cheaper by the Dozen</title>\n"
                + "    <isbn:number>1568491379</isbn:number>\n" + "    <notes>\n"
                + "      <!-- make HTML the default namespace for some commentary -->\n"
                + "      <p xmlns='http://www.w3.org/1999/xhtml'>\n"
                + "          This is a <i>funny</i> book!\n" + "      </p>\n" + "    </notes>\n"
                + "</book>";

        /*
         * The reconstructed slightly different than the original snippet:
         *   - it's missing the xml version declaration
         *   - it's missing the comment
         *   - it has the isbn namespace declared later, however this is still perfectly valid
         */
        String reconstructedExpectation =
                "<book xmlns=\"urn:loc.gov:books\">\n" + "    <title>Cheaper by the Dozen</title>\n"
                        + "    <isbn:number xmlns:isbn=\"urn:ISBN:0-395-36341-6\">1568491379</isbn:number>\n"
                        + "    <notes>\n" + "      \n"
                        + "      <p xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                        + "          This is a <i>funny</i> book!\n" + "      </p>\n"
                        + "    </notes>\n" + "</book>";

        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(snippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

    @Test
    public void testSaxEventToXmlConverterScopedPrefixRedeclaration2()
            throws XMLStreamException, IOException, SAXException {
        String snippet = "<?xml version=\"1.0\"?>\n"
                + "<!-- initially, the default namespace is \"books\" -->\n"
                + "<book xmlns='urn:loc.gov:books'\n"
                + "      xmlns:isbn='urn:ISBN:0-395-36341-6'>\n"
                + "    <title>Cheaper by the Dozen</title>\n"
                + "    <isbn:number>1568491379</isbn:number>\n" + "    <notes>\n"
                + "      <!-- make HTML the default namespace for some commentary -->\n"
                + "      <p xmlns='http://www.w3.org/1999/xhtml'>\n"
                + "          This is a <i>funny</i> book!\n" + "      </p>\n" + "    </notes>\n"
                + "    <title>Cheaper by the Baker's Dozen</title>\n" + "</book>";

        /*
         * The reconstructed slightly different than the original snippet:
         *   - it's missing the xml version declaration
         *   - it's missing the comment
         *   - it has the isbn namespace declared later, however this is still perfectly valid
         */
        String reconstructedExpectation =
                "<book xmlns=\"urn:loc.gov:books\">\n" + "    <title>Cheaper by the Dozen</title>\n"
                        + "    <isbn:number xmlns:isbn=\"urn:ISBN:0-395-36341-6\">1568491379</isbn:number>\n"
                        + "    <notes>\n" + "      \n"
                        + "      <p xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                        + "          This is a <i>funny</i> book!\n" + "      </p>\n"
                        + "    </notes>\n" + "    <title>Cheaper by the Baker's Dozen</title>\n"
                        + "</book>";

        TestSaxParser parser = new TestSaxParser();
        String reconstructedXml = parser.parseAndReconstruct(snippet);
        assertThat(reconstructedExpectation, is(reconstructedXml));
    }

}
