package ddf.catalog.transformer.generic.xml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class SaxEventHandlerDelegate extends DefaultHandler {

    private static SAXParserFactory factory;

    private static SAXParser parser;

    private List<SaxEventHandler> eventHandlers;

    private String namespace;

    public SaxEventHandlerDelegate() {
        try {
            // Read set up
            factory = SAXParserFactory.newInstance();

            factory.setSchema(null);
            factory.setNamespaceAware(true);
            factory.setValidating(false);
//            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

            parser = factory.newSAXParser();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SaxEventHandlerDelegate(String namespace, SaxEventHandler... eventHandlers) {
        this();
        this.namespace = namespace;
        this.eventHandlers = Arrays.asList(eventHandlers);
        //this.eventHandlers = new ArrayList<>(this.eventHandlers);
    }

    public SaxEventHandlerDelegate(SaxEventHandler... eventHandlers) {
        this();
        this.eventHandlers = Arrays.asList(eventHandlers);
        //this.eventHandlers = new ArrayList<>(this.eventHandlers);
    }

    public SaxEventHandlerDelegate(List<SaxEventHandler> eventHandlers) {
        this();
        this.eventHandlers = eventHandlers;
        //this.eventHandlers = new ArrayList<>(this.eventHandlers);
    }

    public Metacard read(InputStream inputStream) {
        Metacard metacard = new MetacardImpl();
        try {

            parser.parse(new BufferedInputStream(inputStream), this);
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }

        // Populate metacard with all attributes constructed in SaxEventHandlers during parsing
        for (SaxEventHandler eventHandler : eventHandlers) {
            List<Attribute> attributes = eventHandler.getAttributes();
            attributes.forEach(metacard::setAttribute);
        }
        return metacard;
    }

    @Override
    public void startDocument() {
        for (SaxEventHandler transformer : eventHandlers) {
            try {
                transformer.startDocument();
            } catch (SAXException e) {
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.startElement(uri, localName, qName, attributes);
        }

    }

    @Override
    public void characters(char ch[], int start, int length) {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.characters(ch, start, length);
        }
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        for (SaxEventHandler transformer : eventHandlers) {
            transformer.endElement(namespaceURI, localName, qName);
        }

    }

}
