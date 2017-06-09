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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

/**
 * Applies Metacard attributes to a provided template.
 */
public class MetacardTemplate {
    private Template template;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTemplate.class);

    public MetacardTemplate(String template) throws IOException {
        Handlebars handleBars = new Handlebars();
        handleBars.registerHelpers(StringHelpers.class);
        this.template = handleBars.compileInline(template);
    }

    public String applyTemplate(Metacard metacard) {
        return applyTemplate(template, metacard);
    }

    private static String applyTemplate(Template template, Metacard metacard) {
        String templatedString = null;

        if (template != null) {
            Map<String, Serializable> templateValueMap = getTemplateValueMap(metacard);
            try {
                templatedString = template.apply(templateValueMap);
            } catch (IOException e) {
                LOGGER.error("Unable to apply values to handle bars template", e);
            }
        }

        return templatedString;
    }

    private static Map<String, Serializable> getTemplateValueMap(Metacard metacard) {
        Map<String, Serializable> metacardValueMap = new HashMap<>();
        Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                .getAttributeDescriptors();
        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            String attributeName = attributeDescriptor.getName();
            Attribute attribute = metacard.getAttribute(attributeName);
            if (attribute != null && attribute.getValue() != null) {
                metacardValueMap.put(attributeName, attribute.getValue());
            }
        }

        return metacardValueMap;
    }
}
