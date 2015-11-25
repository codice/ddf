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
 **/
package org.codice.ddf.catalog.transformer.html;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;

public class RecordViewHelpers {

    public static final SimpleDateFormat ISO_8601_DATE_FORMAT;

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordViewHelpers.class);

    private static DocumentBuilderFactory documentBuilderFactory;

    static {
        ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ISO_8601_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    public CharSequence buildMetadata(String metadata, Options options) {
        if (metadata == null) {
            return "";
        }
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();

            TransformerProperties transformerProperties = new TransformerProperties();
            transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformerProperties.addOutputProperty(OutputKeys.METHOD, "xml");
            transformerProperties.addOutputProperty(OutputKeys.INDENT, "yes");
            transformerProperties.addOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformerProperties
                    .addOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            String result = XMLUtils
                    .format(builder.parse(new InputSource(new StringReader(metadata))),
                            transformerProperties);

            StringBuilder sb = new StringBuilder();
            sb.append("<pre>").append(escapeHtml(result)).append("</pre>");
            return new Handlebars.SafeString(sb.toString());
        } catch (SAXException e) {
            LOGGER.warn("Failed to convert metadata to a pretty string", e);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Failed to convert metadata to a pretty string", e);
        } catch (IOException e) {
            LOGGER.warn("Failed to convert metadata to a pretty string", e);
        }
        return metadata;
    }

    public CharSequence formatGeometry(String geometry, Options options) {
        return geometry;
    }

    public CharSequence formatDate(Date date, Options options) {
        if (date != null) {
            return ISO_8601_DATE_FORMAT.format(date);
        }
        return "";
    }

    public CharSequence hasServicesUrl(Options options) throws IOException {
        return options.fn(this);
    }

    public CharSequence servicesUrl(Options options) {
        return "";
    }
}
