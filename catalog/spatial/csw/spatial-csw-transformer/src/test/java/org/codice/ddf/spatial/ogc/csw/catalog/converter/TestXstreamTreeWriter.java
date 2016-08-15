package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.activation.MimeType;

import org.junit.Test;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;

public class TestXstreamTreeWriter {

    @Test
    public void test() throws IOException {

        //PathTracker tracker = new PathTracker();
        //PathTrackingWriter trackingWriter = new PathTrackingWriter(inWriter, tracker);

        StringWriter stringWriter = new StringWriter();
        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter, new NoNameCoder());
        TreeMarshaller context = new TreeMarshaller(writer, (ConverterLookup) null, (Mapper) null);

        class Converter extends AbstractGmdConverter {

            @Override
            protected List<String> getXstreamAliases() {
                return Collections.emptyList();
            }

            @Override
            protected XstreamPathValueTracker buildPaths(MetacardImpl metacard) {
                XstreamPathValueTracker pathValueTracker = new XstreamPathValueTracker();

                pathValueTracker.add(new Path("/foo"), "text");
                pathValueTracker.add(new Path("/foo/@a"), "AttributeA");
                pathValueTracker.add(new Path("/foo/@b"), "AttributeB");

                return pathValueTracker;
            }

            @Override
            protected String getRootNodeName() {
                return "foo";
            }
        }

        (new Converter()).marshal(new MetacardImpl(), writer, context);

        ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString()
                .getBytes(StandardCharsets.UTF_8));
        BinaryContentImpl transformedContent = new BinaryContentImpl(bais, new MimeType());

        //BinaryContent binaryContent = gmdTransformer.transform(metacard, Collections.emptyMap());
        System.out.println(new String(transformedContent.getByteArray()));

    }

}
