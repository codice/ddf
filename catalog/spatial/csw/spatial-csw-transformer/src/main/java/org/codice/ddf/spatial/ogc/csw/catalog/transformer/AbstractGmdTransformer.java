/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

import javax.activation.MimeType;

import org.apache.commons.collections.MapUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;

public class AbstractGmdTransformer implements MetacardTransformer {

    private Supplier<Converter> converterSupplier;

    /**
     *
     * @param converterSupplier must be non-null
     */
    public AbstractGmdTransformer(Supplier<Converter> converterSupplier) {
        notNull(converterSupplier, "converterSupplier must be non-null");
        this.converterSupplier = converterSupplier;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        StringWriter stringWriter = new StringWriter();
        Boolean omitXmlDec = null;
        if (MapUtils.isNotEmpty(arguments)) {
            omitXmlDec = (Boolean) arguments.get(CswConstants.OMIT_XML_DECLARATION);
        }

        if (omitXmlDec == null || !omitXmlDec) {
            stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        }

        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter, new NoNameCoder());

        MarshallingContext context = new TreeMarshaller(writer, null, null);
        copyArgumentsToContext(context, arguments);

        converterSupplier.get()
                .marshal(metacard, writer, context);

        BinaryContent transformedContent;

        ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString()
                .getBytes(StandardCharsets.UTF_8));
        transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }

    private void copyArgumentsToContext(MarshallingContext context,
            Map<String, Serializable> arguments) {

        if (context == null || arguments == null) {
            return;
        }

        for (Map.Entry<String, Serializable> entry : arguments.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }
    }
}
