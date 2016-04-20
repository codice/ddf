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
package org.codice.ddf.catalog.content.plugin.clavin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.bericotech.clavin.gazetteer.CountryCode;
import com.bericotech.clavin.gazetteer.GeoName;
import com.google.common.net.MediaType;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.content.plugin.PostCreateStoragePlugin;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.geo.formatter.CompositeGeometry;
import ddf.geo.formatter.MultiPoint;
import ddf.geo.formatter.Point;

public class ClavinPlugin implements PostCreateStoragePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinPlugin.class);

    private ClavinWrapper clavinWrapper;

    private Set<MediaType> mediaTypes = new HashSet<>();

    private Pattern extraSpacePattern = Pattern.compile("\\p{Space}+");

    public void setClavinWrapper(ClavinWrapper clavinWrapper) {
        this.clavinWrapper = clavinWrapper;
    }

    public ClavinPlugin() {

        mediaTypes.add(MediaType.MICROSOFT_EXCEL);
        mediaTypes.add(MediaType.MICROSOFT_WORD);
        mediaTypes.add(MediaType.MICROSOFT_POWERPOINT);
        mediaTypes.add(MediaType.OOXML_DOCUMENT);
        mediaTypes.add(MediaType.OOXML_PRESENTATION);
        mediaTypes.add(MediaType.OOXML_SHEET);
    }

    @Override
    public CreateStorageResponse process(CreateStorageResponse input)
            throws PluginExecutionException {

        List<ContentItem> contentItems = input.getCreatedContentItems();
        for (ContentItem contentItem : contentItems) {
            if (applyTo(contentItem)) {
                enrich(contentItem);
            }
        }
        return input;
    }

    private void enrich(ContentItem contentItem) throws PluginExecutionException {

        List<List<Double>> points = new ArrayList<>();
        List<CountryCode> countryCodes = new ArrayList<>();

        // heuristic to filter out "meaningless" locations base on single char.
        try {
            clavinWrapper.parse(getText(contentItem))
                    .stream()
                    .filter(resolvedLocation -> resolvedLocation.getMatchedName()
                            .length() > 1)
                    .forEach(resolvedLocation -> {
                        GeoName geoName = resolvedLocation.getGeoname();

                        List<Double> point = new ArrayList<>();
                        Double lon = geoName.getLongitude();
                        Double lat = geoName.getLatitude();
                        if (Double.isFinite(lon) && Double.isFinite(lat)) {
                            point.add(lon);
                            point.add(lat);
                            points.add(point);
                        }

                        CountryCode countryCode = geoName.getPrimaryCountryCode();
                        if (countryCode != null) {
                            countryCodes.add(countryCode);
                        }
                        countryCodes.addAll(geoName.getAlternateCountryCodes());
                    });
        } catch (Exception e) {
            throw new PluginExecutionException("Failed to parse file with clavin.", e);
        }

        Metacard metacard = contentItem.getMetacard();

        // geography
        if (!points.isEmpty()) {
            CompositeGeometry compositeGeometry = points.size() == 1 ? Point.toCompositeGeometry(
                    points.get(0)) : MultiPoint.toCompositeGeometry(points);
            metacard.setAttribute(new AttributeImpl(Metacard.GEOGRAPHY, compositeGeometry.toWkt()));
            LOGGER.debug("Clavin resolved [LON, LAT]'s: " + compositeGeometry.toWkt());
        }

        // countrycodes.
        if (!countryCodes.isEmpty()) {
            String countryCodeString = StringUtils.join(countryCodes, " ");
            metacard.setAttribute(new AttributeImpl("country-codes", countryCodeString));
        }

    }

    private String getText(ContentItem contentItem)
            throws IOException, SAXException, TikaException {

        BodyCapturingXMLHandler handler = new BodyCapturingXMLHandler();

        File file = contentItem.getFile();
        try (FileInputStream fis = new FileInputStream(file)) {
            Parser parser = new AutoDetectParser();
            parser.parse(fis, handler, new Metadata(), new ParseContext());
        }

        // collapse whitespace.
        Matcher extraSpaceMatcher = extraSpacePattern.matcher(handler.getBodyText());

        return extraSpaceMatcher.replaceAll(" ");
    }

    private boolean applyTo(final ContentItem contentItem) throws PluginExecutionException {

        final MimeType mimeType = contentItem.getMimeType();
        if (mimeType == null) {
            throw new PluginExecutionException("Failed to get mimetype from content item.");
        }
        final MediaType mediaType = MediaType.create(mimeType.getPrimaryType(),
                mimeType.getSubType());

        return mediaTypes.contains(mediaType);
    }

    /*
        XML handler that outputs empty body.
     */
    private static class BodyCapturingXMLHandler extends ToXMLContentHandler {

        private boolean bodyStartTag = false;

        private boolean bodyEndTag = false;

        private StringBuilder bodyTextBuilder = new StringBuilder();

        private String bodyTagName = "body";

        private boolean insideBodyTag() {
            return bodyStartTag && !bodyEndTag ? true : false;
        }

        public String getBodyText() {
            return bodyTextBuilder.toString();
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (!this.insideBodyTag()) {
                super.characters(ch, start, length);
            } else {
                bodyTextBuilder.append(ch);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            if (!this.insideBodyTag()) {
                super.startElement(uri, localName, qName, atts);
            }
            /*
                - once found, stop checking.
                - check after, so empty body tag is output
             */
            if (!bodyStartTag) {
                bodyStartTag = bodyTagName.equalsIgnoreCase(localName);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            /*
                - once found, stop checking.
                - check before, so empty body tag is output
             */
            if (!bodyEndTag) {
                bodyEndTag = bodyTagName.equalsIgnoreCase(localName);
            }
            if (!this.insideBodyTag()) {
                super.endElement(uri, localName, qName);
            }
        }
    }

}
