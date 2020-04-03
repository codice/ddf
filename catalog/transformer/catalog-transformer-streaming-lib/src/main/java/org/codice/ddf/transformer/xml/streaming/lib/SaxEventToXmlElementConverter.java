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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * A library class used to turn SAX events back into their corresponding XML snippets
 *
 * <p>Not threadsafe
 */
public class SaxEventToXmlElementConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaxEventToXmlElementConverter.class);

  private ByteArrayOutputStream outputStream;

  private XMLOutputFactory xmlOutputFactory;

  private XMLStreamWriter out;

  /*
   * Stack of scoped namespace URI to prefix mappings
   */
  private Deque<Multimap<String, String>> scopeOfNamespacesAdded = new ArrayDeque<>();

  private Deque<NamespaceMapping> namespaceStack = new ArrayDeque<>();

  public SaxEventToXmlElementConverter() throws UnsupportedEncodingException, XMLStreamException {

    outputStream = new ByteArrayOutputStream();
    xmlOutputFactory = XMLOutputFactory.newInstance();
    out =
        xmlOutputFactory.createXMLStreamWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new BufferedOutputStream(outputStream), StandardCharsets.UTF_8)));
  }

  /**
   * Used to reconstruct the start tag of an XML element.
   *
   * @param uri the URI that is passed in by {@link SaxEventHandler}
   * @param localName the localName that is passed in by {@link SaxEventHandler}
   * @param atts the attributes that are passed in by {@link SaxEventHandler}
   * @return this {@see SaxEventHandler#startElement}
   */
  public SaxEventToXmlElementConverter toElement(String uri, String localName, Attributes atts)
      throws XMLStreamException {
    return startConstructingElement(uri, localName, atts);
  }

  private SaxEventToXmlElementConverter startConstructingElement(
      String uri, String localName, Attributes atts) throws XMLStreamException {
    Multimap<String, String> addedNamespaces = ArrayListMultimap.create();
    if (scopeOfNamespacesAdded.peek() != null) {
      addedNamespaces.putAll(scopeOfNamespacesAdded.peek());
    }
    scopeOfNamespacesAdded.push(addedNamespaces);
    // URI to prefix
    Map<String, String> scopedNamespaces = new HashMap<>();
    Iterator<NamespaceMapping> iter = namespaceStack.descendingIterator();
    while (iter.hasNext()) {
      NamespaceMapping tmpPair = iter.next();

      // switch prefix and URI
      scopedNamespaces.put(tmpPair.getUri(), tmpPair.getPrefix());
    }

    /*
     * Use the uri to look up the namespace prefix and append it and the localName to the start tag
     */
    out.writeStartElement(scopedNamespaces.get(uri), localName, uri);
    if (!checkNamespaceAdded(uri, scopedNamespaces)) {
      out.writeNamespace(scopedNamespaces.get(uri), uri);
      addedNamespaces.put(uri, scopedNamespaces.get(uri));
    }
    /*
     * Loop through the attributes and append them, prefixed with the proper namespace
     * We loop through the attributes twice to ensure all "xmlns" attributes are declared before
     * other attributes
     */
    for (int i = 0; i < atts.getLength(); i++) {
      if (atts.getURI(i).isEmpty()) {
        out.writeAttribute(atts.getLocalName(i), atts.getValue(i));
      } else {
        String attUri = atts.getURI(i);

        if (!checkNamespaceAdded(attUri, scopedNamespaces)) {
          out.writeNamespace(scopedNamespaces.get(attUri), attUri);
          addedNamespaces.put(attUri, scopedNamespaces.get(attUri));
        }
        try {
          out.writeAttribute(
              scopedNamespaces.get(attUri), attUri, atts.getLocalName(i), atts.getValue(i));

          /*
           * XML doesn't allow for duplicate attributes in an element, e.g.
           * no <element attribute=1 attribute=2>
           * no <element ns1:attribute=1 ns2:attribute=2 xlmns:ns1=foobar xlmns:ns2=foobar>
           * however - if one of the namespaces is the default namespace, this duplication is okay,
           * yes <element attribute=1 ns1:attribute=2 xlmns=foobar xmlns:ns1=foobar>
           *
           * This catch block handles this edge case
           */
        } catch (XMLStreamException e) {
          /*
           * Get the first non-empty prefix that is associated with the URI (the other, non-default prefix)
           */
          handleNamespaceDupes(atts, addedNamespaces, i, attUri);
        }
      }
    }

    return this;
  }

  private void handleNamespaceDupes(
      Attributes atts, Multimap<String, String> addedNamespaces, int i, String attUri)
      throws XMLStreamException {
    NamespaceMapping altNSMap =
        namespaceStack.stream()
            .filter(p -> attUri.equals(p.getUri()) && !(p.getPrefix().isEmpty()))
            .findFirst()
            .orElse(null);

    if (altNSMap != null) {
      String altNS = altNSMap.getPrefix();
      out.writeNamespace(altNS, attUri);
      addedNamespaces.put(attUri, altNS);
      out.writeAttribute(altNS, attUri, atts.getLocalName(i), atts.getValue(i));
    } else {
      LOGGER.debug("No prefix matching {}", attUri);
    }
  }

  /**
   * Method used to reconstruct the end tag of an XML element.
   *
   * @param uri the namespaceURI that is passed in by {@link SaxEventHandler}
   * @param localName the localName that is passed in by {@link SaxEventHandler}
   * @return this {@see SaxEventHandler#endElement}
   */
  public SaxEventToXmlElementConverter toElement(String uri, String localName)
      throws XMLStreamException {
    return finishConstructingElement();
  }

  private SaxEventToXmlElementConverter finishConstructingElement() throws XMLStreamException {
    scopeOfNamespacesAdded.removeFirst();
    /*
     * Append the properly prefixed end tag to the XML snippet
     */
    out.writeEndElement();
    return this;
  }

  /**
   * Method used to reconstruct the characters/value of an XML element.
   *
   * @param ch the ch that is passed in by {@link SaxEventHandler}
   * @param start the start that is passed in by {@link SaxEventHandler}
   * @param length the length that is passed in by {@link SaxEventHandler}
   * @return this {@see SaxEventHandler#characters}
   */
  public SaxEventToXmlElementConverter toElement(char[] ch, int start, int length)
      throws XMLStreamException {
    return addCharactersToElement(ch, start, length);
  }

  private SaxEventToXmlElementConverter addCharactersToElement(char[] ch, int start, int length)
      throws XMLStreamException {
    out.writeCharacters(ch, start, length);
    return this;
  }

  /**
   * Overridden toString method to return the XML snippet that has been reconstructed
   *
   * @return the reconstructed XML snippet
   */
  @Override
  public String toString() {
    try {
      out.flush();
      return outputStream.toString(String.valueOf(StandardCharsets.UTF_8));

    } catch (XMLStreamException | UnsupportedEncodingException e) {
      LOGGER.debug("Could not convert XML Stream writer to String");
      return "";
    }
  }

  /**
   * Resets all stateful variables of the {@link SaxEventToXmlElementConverter} Should be used
   * before expecting a fresh XML snippet Can be used instead of declaring a new one
   *
   * @return this
   */
  public SaxEventToXmlElementConverter reset() {
    outputStream.reset();
    try {
      out = xmlOutputFactory.createXMLStreamWriter(outputStream);
    } catch (XMLStreamException e) {
      LOGGER.debug("Could not reset XMLStreamWriter");
    }
    return this;
  }

  /**
   * Method used in a {@link SaxEventHandler#startElement(String, String, String, Attributes)} to
   * populate the {@link SaxEventToXmlElementConverter#namespaceMapping}, which allows
   * namespaceURI/prefix lookup. (Could potentially be used elsewhere, but one would have to ensure
   * correct use)
   *
   * @param prefix the namespace prefix that is passed in by {@link SaxEventHandler}
   * @param uri the namespace uri that is passed in by {@link SaxEventHandler}
   */
  public void addNamespace(String prefix, String uri) throws XMLStreamException {
    namespaceStack.push(new NamespaceMapping(prefix, uri));
  }

  public void removeNamespace(String prefix) {
    Iterator<NamespaceMapping> iter = namespaceStack.iterator();
    while (iter.hasNext()) {
      NamespaceMapping mapping = iter.next();
      if (mapping.getPrefix().equals(prefix)) {
        iter.remove();
        break;
      }
    }
  }

  private boolean checkNamespaceAdded(String uri, Map<String, String> scopedNamespaces) {
    Multimap<String, String> peek = scopeOfNamespacesAdded.peek();
    return peek != null
        && peek.containsKey(uri)
        && peek.get(uri).contains(scopedNamespaces.get(uri));
  }

  private static class NamespaceMapping {

    private String prefix;

    private String uri;

    NamespaceMapping(String prefix, String uri) {
      this.prefix = prefix;
      this.uri = uri;
    }

    String getPrefix() {
      return prefix;
    }

    String getUri() {
      return uri;
    }
  }
}
