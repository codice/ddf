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
package org.codice.ddf.spatial.kml.transformer;

import com.github.jknack.handlebars.Options;
import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * A handlebars template helper class that creates handlebar helpers which are used in the
 * description.hbt handlebars template
 */
public class DescriptionTemplateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DescriptionTemplateHelper.class);

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private ActionProvider resourceActionProvider;

    private static final List<String> NON_PRINTABLE_ATTRIBUTES = Collections.unmodifiableList(
            Arrays.asList(Metacard.RESOURCE_SIZE, Metacard.RESOURCE_URI, Metacard.GEOGRAPHY,
                    Metacard.METADATA, Metacard.THUMBNAIL, Metacard.CONTENT_TYPE_VERSION));

    public DescriptionTemplateHelper(ActionProvider actionProvider) {
        this.resourceActionProvider = actionProvider;
    }

    public CharSequence isPrintableAttribute(Attribute attribute, AttributeFormat format,
            Options options) throws IOException {
        if (attribute == null || attribute.getValue() == null ||
                NON_PRINTABLE_ATTRIBUTES.contains(attribute.getName()) ||
                prettyPrint(attribute, format) == null) {
            return options.inverse();
        }
        return options.fn();
    }

    public String prettyPrint(Attribute attribute, AttributeFormat format) {
        switch (format) {
        case BINARY:
            return DatatypeConverter.printBase64Binary((byte[]) attribute.getValue());
        case DATE:
            if (attribute != null && attribute.getValue() != null) {
                return dateFormat.format((Date) attribute.getValue());
            } else {
                return dateFormat.format(new Date());
            }
            // There is no way to prettyPrint these
        case GEOMETRY:
        case OBJECT:
        case XML:
            return null;
        // Nothing to do
        case STRING:
        case BOOLEAN:
        case DOUBLE:
        case FLOAT:
        case INTEGER:
        case LONG:
        case SHORT:
        default:
            return attribute.getValue().toString();
        }
    }

    public String printAttributeName(Attribute attribute) {
        String title = attribute.getName();
        title = StringUtils.replace(title, "-", " ");
        title = StringUtils.replace(title, ".", " ");
        title = WordUtils.capitalize(title);
        return title;
    }

    public CharSequence hasThumbnail(Metacard context, Options options) {
        if ((context.getThumbnail() != null && context
                .getThumbnail().length != 0)) {
            try {
                return options.fn();
            } catch (IOException e) {
                LOGGER.error("Failed to execute thumbnail template", e);
                return "";
            }
        } else {
            try {
                return options.inverse();
            } catch (IOException e) {
                LOGGER.error("Failed to execute noThumbnail template", e);
                return "";
            }
        }
    }

    public String base64Thumbnail(Metacard context) {
        if(null != context && context.getThumbnail().length != 0) {
            return DatatypeConverter.printBase64Binary(context.getThumbnail());
        } else {
            return null;
        }
    }

    public String resourceUrl(Metacard context) {
        if (resourceActionProvider != null) {
            Action action = resourceActionProvider.getAction(context);
            if (action != null) {
                return action.getUrl().toString();
            }
        }
        return context.getResourceURI().toString();

    }

    public String resourceSizeString(Metacard context) {
        String resourceSize = context.getResourceSize();
        String sizePrefixes = " KMGTPEZYXWVU";

        if (resourceSize == null || resourceSize.trim().length() == 0
                || resourceSize.toLowerCase().indexOf("n/a") >= 0) {
            return null;
        }

        long size = 0;
        // if the size is not a number, and it isn't 'n/a', assume it is
        // already formatted, ie "10 MB"
        try {
            size = Long.parseLong(resourceSize);
        } catch (NumberFormatException nfe) {
            LOGGER.debug(
                    "Failed to parse resourceSize ({}), assuming already formatted.",
                    StringUtils.trim(resourceSize));
            return resourceSize;
        }

        if (size <= 0) {
            return "0";
        }
        int t2 = (int) Math
                .min(Math.floor(Math.log(size) / Math.log(1024)), 12);
        char c = sizePrefixes.charAt(t2);
        return (Math.round(size * 100 / Math.pow(1024, t2)) / 100) + " "
                + (c == ' ' ? "" : c) + "B";
    }


}
