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
