package ddf.catalog.transformer.generic.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;

public class XMLSaxEventHandlerImpl implements SaxEventHandler {

    private List<Attribute> attributes;

    private Boolean stillInterested = true;

    private Boolean reading = false;

    private StringBuffer stringBuffer;

    private boolean bTitle = false;

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public void setDocumentLocator(Locator locator) {

    }

    @Override
    public void startDocument() {
        stringBuffer = new StringBuffer();
        attributes = new ArrayList<>();

    }

    @Override
    public void endDocument() throws SAXException {

    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {

    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {

    }

    /* +++++++++++++++++++++++++++++++++++++++++++++++++++++ */

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (!stillInterested) {
            return;
        }
        switch (localName.toLowerCase()) {
        case "string":
            String attribute = attributes.getValue("name");
            if (attribute != null && attribute.equals("title")) {
                bTitle = true;
                break;
            }
        default:
            break;
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (!stillInterested) {
            return;
        }
        switch (localName.toLowerCase()) {
        case "string":
            if (bTitle) {
                String result = stringBuffer.toString().trim();
                stringBuffer.setLength(0);
                bTitle = false;
                attributes.add(new AttributeImpl(Metacard.TITLE, result));
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (!stillInterested) {
            return;
        }
        if (bTitle) {
            stringBuffer.append(new String(ch, start, length));
        }

    }

    /* +++++++++++++++++++++++++++++++++++++++++++++++++++++ */

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {

    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {

    }

    @Override
    public void skippedEntity(String name) throws SAXException {

    }

    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException {

    }

    @Override
    public void unparsedEntityDecl(String name, String publicId, String systemId,
            String notationName) throws SAXException {

    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return null;
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {

    }

    @Override
    public void error(SAXParseException exception) throws SAXException {

    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {

    }
}
