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
package ddf.catalog.transformer.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.codice.ddf.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;

/**
 * Transforms a {@link SourceResponse} object into Metacard Element XML text, which is GML 3.1.1.
 * compliant XML.
 */
public class XmlResponseQueueTransformer extends AbstractXmlTransformer
        implements QueryResponseTransformer {

    public static final int BUFFER_SIZE = 1024;

    private static class MetacardForkTask extends RecursiveTask<StringWriter> {
        private final ImmutableList<Result> resultList;

        private final ForkJoinPool fjp;

        private final GeometryTransformer geometryTransformer;

        private final int threshold;

        private final AtomicBoolean cancelOperation;

        private final MetacardMarshaller metacardMarshaller;

        MetacardForkTask(ImmutableList<Result> resultList, ForkJoinPool fjp,
                GeometryTransformer geometryTransformer, int threshold, MetacardMarshaller mcm) {
            this(resultList, fjp, geometryTransformer, threshold, new AtomicBoolean(false), mcm);
        }

        private MetacardForkTask(ImmutableList<Result> resultList, ForkJoinPool fjp,
                GeometryTransformer geometryTransformer, int threshold,
                AtomicBoolean cancelOperation, MetacardMarshaller mcm) {
            this.resultList = resultList;
            this.fjp = fjp;
            this.geometryTransformer = geometryTransformer;
            this.threshold = threshold;
            this.cancelOperation = cancelOperation;
            this.metacardMarshaller = mcm;
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
                        geometryTransformer, threshold, cancelOperation, metacardMarshaller);
                fLeft.fork();
                MetacardForkTask fRight = new MetacardForkTask(
                        resultList.subList(half, resultList.size()), fjp, geometryTransformer,
                        threshold, cancelOperation, metacardMarshaller);
                StringWriter rightList = fRight.compute();
                StringWriter leftList = fLeft.join();

                leftList.append(rightList.getBuffer());
                return leftList;
            }
        } // end compute()

        private StringWriter doCompute() {
            StringWriter sw = new StringWriter(BUFFER_SIZE);
            Map<String, Serializable> args = new HashMap<>();
            args.put(MetacardMarshallerImpl.OMIT_XML_DECL, Boolean.TRUE);
            try {
                for (Result result : resultList) {
                    Metacard metacard = result.getMetacard();
                    String xmlString = metacardMarshaller.marshal(metacard, args);
                    sw.append(xmlString);
                }
            } catch (XmlPullParserException | IOException | CatalogTransformerException e) {
                cancelOperation.set(true);
                throw new RuntimeException("Failure to write node; operation aborted", e);
            }
            return sw;
        }

    } // end MetacardForkTask class

    private final ForkJoinPool fjp;

    private final GeometryTransformer geometryTransformer;

    private final PrintWriterProvider printWriterProvider;

    private final MetacardMarshaller metacardMarshaller;

    private int threshold;

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlResponseQueueTransformer.class);

    private final MimeType mimeType;

    private static final Map<String, String> NAMESPACE_MAP;

    private static final String GML_PREFIX = "gml";

    static {
        String nsPrefix = "xmlns";

        NAMESPACE_MAP = new ImmutableMap.Builder<String, String>()
                .put(nsPrefix, "urn:catalog:metacard")
                .put(nsPrefix + ":" + GML_PREFIX, "http://www.opengis.net/gml")
                .put(nsPrefix + ":xlink", "http://www.w3.org/1999/xlink")
                .put(nsPrefix + ":smil", "http://www.w3.org/2001/SMIL20/")
                .put(nsPrefix + ":smillang", "http://www.w3.org/2001/SMIL20/Language").build();
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
    public XmlResponseQueueTransformer(Parser parser, ForkJoinPool fjp, PrintWriterProvider pwp,
            MetacardMarshaller mcm, MimeType mimeType) {
        super(parser);
        this.fjp = fjp;
        geometryTransformer = new GeometryTransformer(parser);
        this.printWriterProvider = pwp;
        this.metacardMarshaller = mcm;
        this.mimeType = mimeType;
        try {
            mimeType.setPrimaryType("text");
            mimeType.setSubType("xml");
        } catch (MimeTypeParseException e) {
            LOGGER.info("Failure creating MIME type", e);
            throw new ExceptionInInitializerError(e);
        }
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
            PrintWriter writer = printWriterProvider.build(Metacard.class);
            writer.setRawValue("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");

            writer.startNode("metacards");
            for (Map.Entry<String, String> nsRow : NAMESPACE_MAP.entrySet()) {
                writer.addAttribute(nsRow.getKey(), nsRow.getValue());
            }

            if (response.getResults() != null && !response.getResults().isEmpty()) {
                StringWriter metacardContent = fjp
                        .invoke(new MetacardForkTask(ImmutableList.copyOf(response.getResults()),
                                fjp, geometryTransformer, threshold, metacardMarshaller));

                writer.setRawValue(metacardContent.getBuffer().toString());
            }

            writer.endNode(); // metacards

            ByteArrayInputStream bais = new ByteArrayInputStream(writer.makeString().getBytes());

            return new BinaryContentImpl(bais, mimeType);
        } catch (Exception e) {
            LOGGER.info("Failed Query response transformation", e);
            throw new CatalogTransformerException("Failed Query response transformation");
        }
    }
}
