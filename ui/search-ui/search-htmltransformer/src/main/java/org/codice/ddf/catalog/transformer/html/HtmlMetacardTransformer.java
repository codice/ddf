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

package org.codice.ddf.catalog.transformer.html;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class HtmlMetacardTransformer implements MetacardTransformer {

    private static final MimeType DEFAULT_MIME_TYPE;

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlMetacardTransformer.class);

    private static final String TEMPLATE_DIRECTORY = "/templates";

    private static final String TEMPLATE_SUFFIX = ".hbt";

    private static final String RECORD_TEMPLATE = "recordContents";
    private static final String RECORD_HTML_TEMPLATE = "recordHtml";

    private Handlebars handlebars;
    private Template template;
    private ValueResolver[] resolvers;
    
    private static ClassPathTemplateLoader templateLoader;
        
    static {
        MimeType mimeType = null;
        try {
            mimeType = new MimeType("text/html");
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to parse mimeType", e);
        }
        DEFAULT_MIME_TYPE = mimeType;
        
        templateLoader = new ClassPathTemplateLoader();
        templateLoader.setPrefix(TEMPLATE_DIRECTORY);
        templateLoader.setSuffix(TEMPLATE_SUFFIX);
    }

    public HtmlMetacardTransformer() {        
        handlebars = new Handlebars(templateLoader);
        handlebars.registerHelpers(new RecordViewHelpers());
        
        resolvers = new ValueResolver[] {new MetacardValueResolver(),
                MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE};
        try {
            handlebars.compile(RECORD_TEMPLATE);
            template = handlebars.compile(RECORD_HTML_TEMPLATE);
        } catch (IOException e) {
            LOGGER.error("Failed to load templates", e);
        }
    }
    
    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {

        if (metacard == null) {
            throw new CatalogTransformerException("Cannot transform null metacard.");
        }
        
        String html = buildHtml(metacard);

        return new ddf.catalog.data.BinaryContentImpl(
                new ByteArrayInputStream(html.getBytes()), DEFAULT_MIME_TYPE);
    }

    private String buildHtml(Metacard metacard) {


        try {
            Context context = Context.newBuilder(metacard).resolver(resolvers).build();
            return template.apply(context);            
        } catch (IOException e) {
            LOGGER.error("Failed to apply template", e);
        }

        return null;
    }

}
