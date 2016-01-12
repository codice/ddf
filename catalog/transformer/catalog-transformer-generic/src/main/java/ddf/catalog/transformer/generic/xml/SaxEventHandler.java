package ddf.catalog.transformer.generic.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import ddf.catalog.data.Attribute;

public interface SaxEventHandler extends EntityResolver, DTDHandler, ContentHandler, ErrorHandler {

    /**
     *
     *
     * @return a list of attributes that has been constructed during the parsing of an XML document.
     */
    List<Attribute> getAttributes();

    void startElement(String uri, String localName, String qName, Attributes attributes);

    void endElement(String namespaceURI, String localName, String qName);

    void characters(char ch[], int start, int length);

}
