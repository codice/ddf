/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import org.xml.sax.SAXException;

public class SaxEventToXmlElementConverterTest {

  /*
   * Some of the "reconstructedExpectations" in this class are different than the input. This is acceptable and expected,
   * because the output just has to be semantically the same as the input, it doesn't have to be literally the same.
   * For example, sometimes the namespaces are declared at different scopes, in a different order, comments are removed, etc.
   */

  @Test
  public void testSaxEventToXmlElementConverterRedeclaredDefaultNamespaceUri()
      throws XMLStreamException, IOException, SAXException {
    String doubleDeclaredNamespaceUriSnippet =
        // @formatter:off
        "<x xmlns:ns1='foobar' xmlns='foobar'>"
            + "    <good1 a='1' b='2' />"
            + "    <good2 a='1' ns1:a='2' />"
            + "</x>";
    // @formatter:on

    String reconstructedExpectation =
        // @formatter:off
        "<x xmlns='foobar'>"
            + "    <good1 a='1' b='2'></good1>"
            + "    <good2 a='1' xmlns:ns1='foobar' ns1:a='2'></good2>"
            + "</x>";
    // @formatter:on
    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(doubleDeclaredNamespaceUriSnippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedXml, is(reconstructedExpectation));
  }

  @Test
  public void testSaxEventToXmlElementConverterRedeclaredDefaultNamespaceUriVariation()
      throws XMLStreamException, IOException, SAXException {
    String doubleDeclaredNamespaceUriSnippet =
        // @formatter:off
        "<x xmlns:ns1='notfoobar' xmlns:ns2='foobar' xmlns='foobar'>"
            + "    <good1 a='1' b='2' />"
            + "    <good2 a='1' ns2:a='2' ns1:a='3'/>"
            + "</x>";
    // @formatter:on

    String reconstructedExpectation =
        // @formatter:off
        "<x xmlns='foobar'>"
            + "    <good1 a='1' b='2'></good1>"
            + "    <good2 a='1' xmlns:ns2='foobar' ns2:a='2' xmlns:ns1='notfoobar' ns1:a='3'></good2>"
            + "</x>";
    // @formatter:on
    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(doubleDeclaredNamespaceUriSnippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedXml, is(reconstructedExpectation));
  }

  @Test
  public void testSaxEventToXmlElementConverterRedeclaredNamespaceUriVariation()
      throws XMLStreamException, IOException, SAXException {
    String doubleDeclaredNamespaceUriSnippet =
        // @formatter:off
        "<x:y xmlns='default' xmlns:x='www.x.com' x:one='2' x:two='2'>"
            + "    <y:z xmlns:y='www.x.com' x:one='1' y:two='2'>"
            + "        <x:z>abcdefg</x:z>"
            + "    </y:z>"
            + "</x:y>";
    // @formatter:on
    String reconstructedExpectation =
        // @formatter:off
        "<x:y xmlns:x='www.x.com' x:one='2' x:two='2'>"
            + "    <y:z xmlns:y='www.x.com' y:one='1' y:two='2'>"
            + "        <y:z>abcdefg</y:z>"
            + "    </y:z>"
            + "</x:y>";
    // @formatter:on
    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(doubleDeclaredNamespaceUriSnippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedXml, is(reconstructedExpectation));
  }

  @Test
  public void testSaxEventToXmlElementConverterRedeclaredNamespaceUriVariation2()
      throws XMLStreamException, IOException, SAXException {
    String snippet =
        // @formatter:off
        "<?xml version='1.0' encoding='UTF-8' ?>"
            + "<outer xmlns:aaa='whocares' xmlns='http://www.w3.com'>"
            + "    <aaa:foo name='outside'>"
            + "        <aaa:bar xmlns:bbb='inside1'>"
            + "            <bbb:baz xmlns:aaa='inside2'>"
            + "                <aaa:verybad name='scope matters'/>"
            + "            </bbb:baz>"
            + "        </aaa:bar>"
            + "    </aaa:foo>"
            + "</outer>";
    // @formatter:on
    String reconstructedExpectation =
        // @formatter:off
        "<outer xmlns='http://www.w3.com'>"
            + "    <aaa:foo xmlns:aaa='whocares' name='outside'>"
            + "        <aaa:bar>"
            + "            <bbb:baz xmlns:bbb='inside1'>"
            + "                <aaa:verybad xmlns:aaa='inside2' name='scope matters'></aaa:verybad>"
            + "            </bbb:baz>"
            + "        </aaa:bar>"
            + "    </aaa:foo>"
            + "</outer>";
    // @formatter:on

    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(snippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedXml, is(reconstructedExpectation));
  }

  @Test
  public void testSaxEventToXmlConverterNormal()
      throws XMLStreamException, IOException, SAXException {
    String normalSnippet =
        // @formatter:off
        "<?xml version='1.0'?>"
            + "<!-- both namespace prefixes are available throughout -->"
            + "<bk:book xmlns:bk='urn:loc.gov:books' xmlns:isbn='urn:ISBN:0-395-36341-6'>"
            + "    <bk:title>Cheaper by the Dozen</bk:title>"
            + "    <isbn:number>1568491379</isbn:number>"
            + "</bk:book>";
    // @formatter:on

    String reconstructedExpectation =
        // @formatter:off
        "<bk:book xmlns:bk='urn:loc.gov:books'>"
            + "    <bk:title>Cheaper by the Dozen</bk:title>"
            + "    <isbn:number xmlns:isbn='urn:ISBN:0-395-36341-6'>1568491379</isbn:number>"
            + "</bk:book>";
    // @formatter:on

    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(normalSnippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedExpectation, is(reconstructedXml));
  }

  @Test
  public void testSaxEventToXmlConverterScopedPrefixRedeclaration()
      throws XMLStreamException, IOException, SAXException {
    String snippet =
        // @formatter:off
        "<?xml version='1.0'?>"
            + "<!-- initially, the default namespace is 'books' -->"
            + "<book xmlns='urn:loc.gov:books' xmlns:isbn='urn:ISBN:0-395-36341-6'>"
            + "    <title>Cheaper by the Dozen</title>"
            + "    <isbn:number>1568491379</isbn:number>"
            + "    <notes><!-- make HTML the default namespace for some commentary -->"
            + "        <p xmlns='http://www.w3.org/1999/xhtml'>This is a <i>funny</i> book!</p>"
            + "    </notes>"
            + "</book>";
    // @formatter:on
    String reconstructedExpectation =
        // @formatter:off
        "<book xmlns='urn:loc.gov:books'>"
            + "    <title>Cheaper by the Dozen</title>"
            + "    <isbn:number xmlns:isbn='urn:ISBN:0-395-36341-6'>1568491379</isbn:number>"
            + "    <notes>"
            + "        <p xmlns='http://www.w3.org/1999/xhtml'>This is a <i>funny</i> book!</p>"
            + "    </notes>"
            + "</book>";
    // @formatter:on
    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(snippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedExpectation, is(reconstructedXml));
  }

  @Test
  public void testSaxEventToXmlConverterScopedPrefixRedeclarationVariation()
      throws XMLStreamException, IOException, SAXException {
    String snippet =
        // @formatter:off
        "<?xml version='1.0'?>"
            + "<!-- initially, the default namespace is 'books' -->"
            + "<book xmlns='urn:loc.gov:books'>"
            + "    <title>Cheaper by the Dozen</title>"
            + "    <isbn:number xmlns:isbn='urn:ISBN:0-395-36341-6'>1568491379</isbn:number>"
            + "    <notes><!-- make HTML the default namespace for some commentary -->"
            + "        <p xmlns='http://www.w3.org/1999/xhtml'>This is a <i>funny</i> book!</p>"
            + "    </notes>"
            + "    <title xmlns='urn:loc.gov:books'>Cheaper by the Bakers Dozen</title>"
            + "</book>";
    // @formatter:on
    String reconstructedExpectation =
        // @formatter:off
        "<book xmlns='urn:loc.gov:books'>"
            + "    <title>Cheaper by the Dozen</title>"
            + "    <isbn:number xmlns:isbn='urn:ISBN:0-395-36341-6'>1568491379</isbn:number>"
            + "    <notes>"
            + "        <p xmlns='http://www.w3.org/1999/xhtml'>This is a <i>funny</i> book!</p>"
            + "    </notes>"
            + "    <title>Cheaper by the Bakers Dozen</title>"
            + "</book>";
    // @formatter:on
    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(snippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedExpectation, is(reconstructedXml));
  }

  @Test
  public void testSaxEventToXmlConverterRedclaredPrefixAndReusedUri()
      throws XMLStreamException, IOException, SAXException {
    String snippet =
        // @formatter:off
        "<x:y xmlns='default' xmlns:x='www.x.com' x:one='2' x:two='2'>"
            + "    <y:z xmlns:y='www.x.com' xmlns:x='dumb.but.possible' x:one='1' y:two='2'>"
            + "        <x:z>abcdefg</x:z>"
            + "    </y:z>"
            + "</x:y>";
    // @formatter:on

    String reconstructedExpectation =
        // @formatter:off
        "<x:y xmlns:x='www.x.com' x:one='2' x:two='2'>"
            + "    <y:z xmlns:y='www.x.com' xmlns:x='dumb.but.possible' x:one='1' y:two='2'>"
            + "        <x:z>abcdefg</x:z>"
            + "    </y:z>"
            + "</x:y>";
    // @formatter:on

    TestSaxParser parser = new TestSaxParser();
    String reconstructedXml = parser.parseAndReconstruct(snippet);
    reconstructedExpectation = reconstructedExpectation.replaceAll("'", "\"");
    assertThat(reconstructedExpectation, is(reconstructedXml));
  }
}
