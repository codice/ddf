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
package ddf.catalog.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;

/**
 * Transforms a {@link SourceResponse} object into Metacard Element XML text, which is GML 3.1.1.
 * compliant XML.
 */
public class XmlResponseQueueTransformer extends AbstractXmlTransformer
        implements QueryResponseTransformer {
    public static final int BUFFER_SIZE = 1024;

    /**
     * Writer is not thread-safe; instances should not be shared.
     */
    // @NotThreadSafe
    private static class MetacardPrintWriter extends PrettyPrintWriter {
        private static final char[] NULL = "&#x0;".toCharArray();

        private static final char[] AMP = "&amp;".toCharArray();

        private static final char[] LT = "&lt;".toCharArray();

        private static final char[] GT = "&gt;".toCharArray();

        private static final char[] CR = "&#xd;".toCharArray();

        private static final char[] APOS = "&apos;".toCharArray();

        private boolean isRawText = false;

        public MetacardPrintWriter(Writer writer) {
            super(writer);
        }

        private void setRawValue(String text) {
            try {
                isRawText = true;
                setValue(text);
            } finally {
                isRawText = false;
            }
        }

        @Override
        protected void writeText(QuickWriter writer, String text) {
            if (text == null) {
                return;
            }

            if (isRawText) {
                writer.write(text);
            } else {
                int length = text.length();
                for (int i = 0; i < length; i++) {
                    char c = text.charAt(i);
                    switch (c) {
                    case '\0':
                        writer.write(NULL);
                        break;
                    case '&':
                        writer.write(AMP);
                        break;
                    case '<':
                        writer.write(LT);
                        break;
                    case '>':
                        writer.write(GT);
                        break;
                    case '\'':
                        writer.write(APOS);
                        break;
                    case '\r':
                        writer.write(CR);
                        break;
                    case '\t':
                    case '\n':
                        writer.write(c);
                        break;
                    default:
                        if (Character.isDefined(c) && !Character.isISOControl(c)) {
                            writer.write(c);
                        } else {
                            writer.write("&#x");
                            writer.write(Integer.toHexString(c));
                            writer.write(';');
                        }
                    }
                }
            }
        }
    }

    private static class MetacardForkTask extends RecursiveTask<StringWriter> {
        private static final String DF_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

        private final ImmutableList<Result> resultList;

        private final ForkJoinPool fjp;

        private final int threshold;

        private final AtomicBoolean cancelOperation;

        MetacardForkTask(ImmutableList<Result> resultList, ForkJoinPool fjp, int threshold) {
            this(resultList, fjp, threshold, new AtomicBoolean(false));
        }

        private MetacardForkTask(ImmutableList<Result> resultList, ForkJoinPool fjp, int threshold,
                AtomicBoolean cancelOperation) {
            this.resultList = resultList;
            this.fjp = fjp;
            this.threshold = threshold;
            this.cancelOperation = cancelOperation;
        }

        @Override
        protected StringWriter compute() {
            if (cancelOperation.get()) {
                return null;
            }

            if (resultList.size() < threshold) {
                return doCompute();
            } else {
                int half = resultList.size() / 2;

                MetacardForkTask fLeft = new MetacardForkTask(resultList.subList(0, half), fjp,
                        threshold, cancelOperation);
                fLeft.fork();
                MetacardForkTask fRight = new MetacardForkTask(
                        resultList.subList(half, resultList.size()), fjp, threshold,
                        cancelOperation);
                StringWriter rightList = fRight.compute();
                StringWriter leftList = fLeft.join();

                leftList.append(rightList.getBuffer());
                return leftList;
            }
        }

        private StringWriter doCompute() {
            StringWriter stringWriter = new StringWriter(BUFFER_SIZE);
            MetacardPrintWriter writer = new MetacardPrintWriter(stringWriter);

            for (Result result : resultList) {
                Metacard metacard = result.getMetacard();
                writer.startNode("metacard");
                if (metacard.getId() != null) {
                    writer.addAttribute("ns1:id", metacard.getId());
                }

                writer.startNode("type");
                if (metacard.getMetacardType().getName() == null
                        || metacard.getMetacardType().getName().length() == 0) {
                    writer.setValue(MetacardType.DEFAULT_METACARD_TYPE_NAME);
                } else {
                    writer.setValue(metacard.getMetacardType().getName());
                }
                writer.endNode(); // type

                if (metacard.getSourceId() != null && metacard.getSourceId().length() > 0) {
                    writer.startNode("source");
                    writer.setValue(metacard.getSourceId());
                    writer.endNode(); // source
                }

                Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                        .getAttributeDescriptors();
                for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                    String attributeName = attributeDescriptor.getName();
                    if (attributeName.equals("id")) {
                        continue;
                    }

                    Attribute attribute = metacard.getAttribute(attributeName);

                    if (attribute != null) {
                        AttributeFormat format = attributeDescriptor.getType().getAttributeFormat();
                        try {
                            writeAttributeToXml(writer, attribute, format);
                        } catch (CatalogTransformerException | IOException e) {
                            cancelOperation.set(true);
                            throw new RuntimeException("Failure to write node; operation aborted",
                                    e);
                        }
                    }
                }
                writer.endNode(); // metacard
            }
            writer.flush();
            return stringWriter;
        }

        private void writeAttributeToXml(MetacardPrintWriter writer, Attribute attribute,
                AttributeFormat format) throws IOException, CatalogTransformerException {
            String attributeName = attribute.getName();

            for (Serializable value : attribute.getValues()) {
                String xmlValue = null;

                switch (format) {

                case STRING:
                case BOOLEAN:
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    xmlValue = value.toString();
                    break;
                case DATE:
                    Date date = (Date) value;
                    xmlValue = DateFormatUtils.formatUTC(date, DF_PATTERN);
                    break;
                case GEOMETRY:
                    xmlValue = geoToXml(new GeometryTransformer().transform(attribute));
                    break;
                case OBJECT:
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try (ObjectOutput output = new ObjectOutputStream(bos)) {
                        output.writeObject(attribute.getValue());
                        xmlValue = Base64.encode(bos.toByteArray());
                    }
                    break;
                case BINARY:
                    xmlValue = Base64.encode((byte[]) value);
                    break;
                case XML:
                    xmlValue = value.toString().replaceAll("[<][?]xml.*[?][>]", "");
                    break;
                }

                // Write the node if we were able to convert it.
                if (xmlValue != null) {
                    writer.startNode(TYPE_NAME_LOOKUP.get(format));
                    writer.addAttribute("name", attributeName);
                    writer.startNode("value");

                    if (format == AttributeFormat.XML || format == AttributeFormat.GEOMETRY) {
                        writer.setRawValue(xmlValue);
                    } else {
                        writer.setValue(xmlValue);
                    }

                    writer.endNode(); // value
                    writer.endNode(); // type
                }
            }
        }

        private String geoToXml(BinaryContent content) {
            try {
                XmlPullParser parser = XppFactory.createDefaultParser();
                XppReader source = new XppReader(new InputStreamReader(content.getInputStream()),
                        parser);

                // Skip the first two nodes, the root nodes of the ns3: namespace
                source.moveDown();
                source.moveDown();

                StringWriter stringWriter = new StringWriter(BUFFER_SIZE);
                PrettyPrintWriter destination = new PrettyPrintWriter(stringWriter);

                new HierarchicalStreamCopier().copy(source, destination);

                // Ideally, we would not intermittently get different namespace prefixes from
                // marshalling; however, we are and can't safely use a NamespacePrefixMapper.
                // So we have this. We specifically look for the 'ns1' prefix which this
                // transformer has locked to the http://www.opengis.net/gml URI. If we do not
                // find it, we do a regex replace of the 'ns?' prefix we do find.
                String geoNode = stringWriter.toString();
                if (!geoNode.startsWith("<ns1:")) {
                    geoNode = geoNode.replaceAll("([</])ns\\d:", "$1ns1:");
                }

                return geoNode;
            } catch (XmlPullParserException e) {
                throw new ConversionException("Unable to copy metadata to XML Output.", e);
            }
        }
    }

    private final ForkJoinPool fjp;

    private int threshold;

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlResponseQueueTransformer.class);

    public static final MimeType MIME_TYPE = new MimeType();

    /**
     * This lookup map is...unfortunate. The current JAXB, which will remain in use for many
     * contexts until/unless we refactor and rewrite all XML processing, determines the attribute
     * names from the metacard schema. This lookup map provides an ugly shortcut for our purposes.
     */
    private static final Map<AttributeType.AttributeFormat, String> TYPE_NAME_LOOKUP;

    private static final Map<String, String> NAMESPACE_MAP;

    static {
        try {
            MIME_TYPE.setPrimaryType("text");
            MIME_TYPE.setSubType("xml");
        } catch (MimeTypeParseException e) {
            LOGGER.info("Failure creating MIME type", e);
            throw new ExceptionInInitializerError(e);
        }

        TYPE_NAME_LOOKUP = new ImmutableMap.Builder<AttributeType.AttributeFormat, String>()
                .put(AttributeFormat.BINARY, "base64Binary").put(AttributeFormat.STRING, "string")
                .put(AttributeFormat.BOOLEAN, "boolean").put(AttributeFormat.DATE, "dateTime")
                .put(AttributeFormat.DOUBLE, "double").put(AttributeFormat.SHORT, "short")
                .put(AttributeFormat.INTEGER, "int").put(AttributeFormat.LONG, "long")
                .put(AttributeFormat.FLOAT, "float").put(AttributeFormat.GEOMETRY, "geometry")
                .put(AttributeFormat.XML, "stringxml").put(AttributeFormat.OBJECT, "object")
                .build();

        String nsPrefix = "xmlns";

        NAMESPACE_MAP = new ImmutableMap.Builder<String, String>()
                .put(nsPrefix, "urn:catalog:metacard")
                .put(nsPrefix + ":ns" + 1, "http://www.opengis.net/gml")
                .put(nsPrefix + ":ns" + 2, "http://www.w3.org/1999/xlink")
                .put(nsPrefix + ":ns" + 4, "http://www.w3.org/2001/SMIL20/")
                .put(nsPrefix + ":ns" + 5, "http://www.w3.org/2001/SMIL20/Language").build();
    }

    /**
     * Constructs a transformer that will convert query responses to XML.
     * The {@code ForkJoinPool} is used for splitting large collections of {@link Metacard}s
     * into smaller collections for concurrent processing. Currently injected through Blueprint,
     * if we choose to use fork-join for other tasks in the application, we should move the
     * construction of the pool from its current location. Conversely, if we move to Java 8 we
     * can simply use the new {@code commonPool} static method provided on {@code ForkJoinPool}.
     *
     * @param fjp the {@code ForkJoinPool} to inject
     */
    public XmlResponseQueueTransformer(ForkJoinPool fjp) {
        this.fjp = fjp;
    }

    /**
     * @param threshold the fork threshold: result lists smaller than this size will be
     *                  processed serially; larger than this size will be processed in
     *                  threshold-sized chunks in parallel
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold <= 1 ? 2 : threshold;
    }

    @Override
    public BinaryContent transform(SourceResponse response, Map<String, Serializable> args)
            throws CatalogTransformerException {
        try {
            Writer stringWriter = new StringWriter(BUFFER_SIZE);
            stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");

            MetacardPrintWriter writer = new MetacardPrintWriter(stringWriter);

            writer.startNode("metacards");
            for (Map.Entry<String, String> nsRow : NAMESPACE_MAP.entrySet()) {
                writer.addAttribute(nsRow.getKey(), nsRow.getValue());
            }

            if (response.getResults() != null && !response.getResults().isEmpty()) {
                StringWriter metacardContent = fjp
                        .invoke(new MetacardForkTask(ImmutableList.copyOf(response.getResults()),
                                fjp, threshold));

                writer.setRawValue(metacardContent.getBuffer().toString());
            }

            writer.endNode(); // metacards

            ByteArrayInputStream bais = new ByteArrayInputStream(
                    stringWriter.toString().getBytes());

            return new BinaryContentImpl(bais, MIME_TYPE);
        } catch (Exception e) {
            LOGGER.info("Failed Query response transformation", e);
            throw new CatalogTransformerException("Failed Query response transformation");
        }
    }
}
