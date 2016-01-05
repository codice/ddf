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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.activation.MimeType;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.ResultType;

/**
 * Implementation of {@link QueryResponseTransformer} for CSW 2.0.2
 * GetRecordsResponse
 */
public class CswQueryResponseTransformer implements QueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswQueryResponseTransformer.class);

    private static final String QUERY_POOL_NAME = "csw-query-pool";

    private static final String CSW_PREFIX =
            CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER;

    private static final String XML_PREFIX =
            XMLConstants.XMLNS_ATTRIBUTE + CswConstants.NAMESPACE_DELIMITER;

    private static final String SEARCH_STATUS_NODE_NAME = "SearchStatus";

    private static final String SEARCH_RESULTS_NODE_NAME = "SearchResults";

    private static final String VERSION_ATTRIBUTE = "version";

    private static final String TIMESTAMP_ATTRIBUTE = "timestamp";

    private static final String NUMBER_OF_RECORDS_MATCHED_ATTRIBUTE = "numberOfRecordsMatched";

    private static final String NEXT_RECORD_ATTRIBUTE = "nextRecord";

    private static final String RECORD_SCHEMA_ATTRIBUTE = "recordSchema";

    private static final String ELEMENT_SET_ATTRIBUTE = "elementSet";

    private static final String XML_DECL = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n";

    public static final String RECORDS_RESPONSE_QNAME =
            CSW_PREFIX + CswConstants.GET_RECORDS_RESPONSE;

    public static final String RECORD_BY_ID_RESPONSE_QNAME =
            CSW_PREFIX + CswConstants.GET_RECORD_BY_ID_RESPONSE;

    public static final String SEARCH_STATUS_QNAME = CSW_PREFIX + SEARCH_STATUS_NODE_NAME;

    public static final String SEARCH_RESULTS_QNAME = CSW_PREFIX + SEARCH_RESULTS_NODE_NAME;

    public static final String NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE = "numberOfRecordsReturned";

    private TransformerManager metacardTransformerManager;

    private ThreadPoolExecutor queryExecutor;

    private PrintWriterProvider writerProvider;

    private static final int BLOCKING_Q_INITIAL_SIZE = 1024;

    private static final int ACCUM_INITIAL_SIZE = 4096;

    public CswQueryResponseTransformer(TransformerManager metacardTransformerManager,
            PrintWriterProvider writerProvider) {
        this.metacardTransformerManager = metacardTransformerManager;
        this.writerProvider = writerProvider;
    }

    @Override
    public BinaryContent transform(SourceResponse sourceResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        validateInput(sourceResponse, arguments);

        CswRecordCollection recordCollection = buildCollection(sourceResponse, arguments);

        ByteArrayInputStream bais;

        if (ResultType.VALIDATE.equals(recordCollection.getResultType())) {
            ByteArrayOutputStream baos = writeAcknowledgement(recordCollection.getRequest());
            bais = new ByteArrayInputStream(baos.toByteArray());
        } else {
            // "catches" recordCollection.getResultType() == null
            List<Result> results = sourceResponse.getResults();
            String xmlString = convert(recordCollection, results, arguments);
            bais = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
        }

        BinaryContent transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }

    private String convert(CswRecordCollection cswRecordCollection, List<Result> results,
            Map<String, Serializable> arguments) throws CatalogTransformerException {

        PrintWriter writer = writerProvider.build(Metacard.class);
        writer.setRawValue(XML_DECL);

        if (cswRecordCollection.isById()) {
            writer.startNode(RECORD_BY_ID_RESPONSE_QNAME);
        } else {
            writer.startNode(RECORDS_RESPONSE_QNAME);
        }

        for (Map.Entry<String, String> entry : DefaultCswRecordMap.getDefaultCswRecordMap()
                .getPrefixToUriMapping().entrySet()) {
            writer.addAttribute(XML_PREFIX + entry.getKey(), entry.getValue());
        }

        long start = (cswRecordCollection.getStartPosition() > 0) ?
                cswRecordCollection.getStartPosition() :
                1;
        long nextRecord = start + cswRecordCollection.getNumberOfRecordsReturned();
        if (nextRecord > cswRecordCollection.getNumberOfRecordsMatched()) {
            nextRecord = 0;
        }

        if (!cswRecordCollection.isById()) {
            writer.addAttribute(VERSION_ATTRIBUTE, CswConstants.VERSION_2_0_2);

            writer.startNode(SEARCH_STATUS_QNAME);
            writer.addAttribute(TIMESTAMP_ATTRIBUTE,
                    ISODateTimeFormat.dateTime().print(new DateTime()));
            writer.endNode();

            writer.startNode(SEARCH_RESULTS_QNAME);
            writer.addAttribute(NUMBER_OF_RECORDS_MATCHED_ATTRIBUTE,
                    Long.toString(cswRecordCollection.getNumberOfRecordsMatched()));

            if (ResultType.HITS.equals(cswRecordCollection.getResultType())) {
                writer.addAttribute(NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE, Long.toString(0));
            } else {
                writer.addAttribute(NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE,
                        Long.toString(cswRecordCollection.getNumberOfRecordsReturned()));
                writer.addAttribute(NEXT_RECORD_ATTRIBUTE, Long.toString(nextRecord));
            }


            writer.addAttribute(RECORD_SCHEMA_ATTRIBUTE, cswRecordCollection.getOutputSchema());

            if (cswRecordCollection.getElementSetType() != null && StringUtils
                    .isNotBlank(cswRecordCollection.getElementSetType().value())) {
                writer.addAttribute(ELEMENT_SET_ATTRIBUTE,
                        cswRecordCollection.getElementSetType().value());
            }
        }

        if (!ResultType.HITS.equals(cswRecordCollection.getResultType())) {
            arguments.put(CswConstants.OMIT_XML_DECLARATION, Boolean.TRUE);

            String metacardsString = multiThreadedMarshal(results,
                    cswRecordCollection.getOutputSchema(), arguments);
            writer.setRawValue(metacardsString);
        }

        if (!cswRecordCollection.isById()) {
            writer.endNode(); // SEARCH_RESULTS_QNAME
        }

        writer.endNode(); // RECORDS_RESPONSE_QNAME

        return writer.makeString();
    }

    /*
        Multi-threaded marshal of metacard assumes:
        - cpu-bound => optimum utilization from availableProcessors()+1 thread pool.
        - query size is unbounded => guard against resource exhaustion with fixed thread-pool,
          fixed work-queue.
     */
    //private void multiThreadedMarshal(PrintWriter writer, List<Result> results,
    private String multiThreadedMarshal(List<Result> results, String recordSchema,
            final Map<String, Serializable> arguments) throws CatalogTransformerException {

        CompletionService<BinaryContent> completionService = new ExecutorCompletionService<>(
                queryExecutor);

        try {
            for (Result result : results) {
                final Metacard mc = result.getMetacard();

                final MetacardTransformer transformer = metacardTransformerManager
                        .getTransformerBySchema(recordSchema);

                if (transformer == null) {
                    throw new CatalogTransformerException(
                            "Cannot find transformer for schema: " + recordSchema);
                }

                // the "current" thread will run submitted task when queueSize exceeded; effectively
                // blocking enqueue of more tasks.
                completionService.submit(new Callable<BinaryContent>() {
                    @Override
                    public BinaryContent call() throws Exception {
                        BinaryContent content = transformer.transform(mc, arguments);
                        return content;
                    }
                });
            }

            int metacardCount = results.size();
            CharArrayWriter accum = new CharArrayWriter(ACCUM_INITIAL_SIZE);
            for (int i = 0; i < metacardCount; i++) {
                Future<BinaryContent> binaryContentFuture = completionService.take(); // blocks
                BinaryContent binaryContent = binaryContentFuture.get();
                IOUtils.copy(binaryContent.getInputStream(), accum);
            }

            return accum.toString();

        } catch (IOException | InterruptedException | ExecutionException xe) {
            throw new CatalogTransformerException(xe);
        }

    } // end multiThreadedMarshal()

    private boolean isByIdQuery(Map<String, Serializable> arguments) {
        Serializable isByIdQuery = arguments.get(CswConstants.IS_BY_ID_QUERY);
        if (isByIdQuery instanceof Boolean) {
            return (Boolean) isByIdQuery;
        }
        return false;
    }

    private void validateInput(SourceResponse sourceResponse, Map<String, Serializable> arguments) {

        if (null == sourceResponse) {
            throw new IllegalArgumentException("Null source response.");
        } else if (null == arguments) {
            throw new IllegalArgumentException("Null argument map.");
        } else if (null == sourceResponse.getResults()) {
            throw new IllegalArgumentException("Null results list.");
        } else if (!isByIdQuery(arguments) && null == arguments
                .get(CswConstants.RESULT_TYPE_PARAMETER)) {
            // An exception is thrown only if the query isn't by ID (i.e. it's not a GetRecordById
            // request) because GetRecordById does not use the ResultType attribute.
            throw new IllegalArgumentException("Null result type argument.");
        } else if (null == sourceResponse.getRequest()) {
            throw new IllegalArgumentException("Null source response query request.");
        } else if (null == sourceResponse.getRequest().getQuery()) {
            throw new IllegalArgumentException("Null source response query.");
        }

    }

    private CswRecordCollection buildCollection(SourceResponse sourceResponse,
            Map<String, Serializable> arguments) {

        CswRecordCollection recordCollection = new CswRecordCollection();

        recordCollection.setNumberOfRecordsMatched(sourceResponse.getHits());
        recordCollection.setNumberOfRecordsReturned(sourceResponse.getResults().size());
        recordCollection.setStartPosition(sourceResponse.getRequest().getQuery().getStartIndex());

        Object elementSetTypeArg = arguments.get(CswConstants.ELEMENT_SET_TYPE);
        if (elementSetTypeArg instanceof ElementSetType) {
            ElementSetType elementSetType = (ElementSetType) elementSetTypeArg;
            recordCollection.setElementSetType(elementSetType);
        }

        Object elementNamesArg = arguments.get(CswConstants.ELEMENT_NAMES);
        if (elementNamesArg instanceof QName[]) {
            QName[] qnames = (QName[]) elementNamesArg;
            if (qnames.length > 0) {
                List<QName> elementNames = new ArrayList();
                for (QName entry : qnames) {
                    elementNames.add(entry);
                }
                recordCollection.setElementName(elementNames);
            }
        }

        Object isByIdQuery = arguments.get(CswConstants.IS_BY_ID_QUERY);
        if (isByIdQuery != null) {
            recordCollection.setById((Boolean) isByIdQuery);
        }

        Object arg = arguments.get((CswConstants.GET_RECORDS));
        if (arg != null && arg instanceof GetRecordsType) {
            recordCollection.setRequest((GetRecordsType) arg);
        }

        Object resultType = arguments.get(CswConstants.RESULT_TYPE_PARAMETER);
        if (resultType instanceof ResultType) {
            recordCollection.setResultType((ResultType) resultType);
        }

        Object outputSchema = arguments.get(CswConstants.OUTPUT_SCHEMA_PARAMETER);
        if (outputSchema instanceof String) {
            recordCollection.setOutputSchema((String) outputSchema);
        } else {
            recordCollection.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        }

        Object doWriteNamespaces = arguments.get(CswConstants.WRITE_NAMESPACES);
        if (doWriteNamespaces instanceof Boolean) {
            recordCollection.setDoWriteNamespaces((Boolean) doWriteNamespaces);
        }

        return recordCollection;
    }

    private ByteArrayOutputStream writeAcknowledgement(GetRecordsType request)
            throws CatalogTransformerException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JAXBContext jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:"
                    + "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0");
            Marshaller marshaller = jaxBContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            AcknowledgementType ack = new AcknowledgementType();
            EchoedRequestType echoedRequest = new EchoedRequestType();
            JAXBElement<GetRecordsType> jaxBRequest = new ObjectFactory().createGetRecords(request);
            echoedRequest.setAny(jaxBRequest);
            ack.setEchoedRequest(echoedRequest);
            try {
                ack.setTimeStamp(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(new GregorianCalendar()));
            } catch (DatatypeConfigurationException e) {
                LOGGER.warn("Failed to set timestamp on Acknowledgement, Exception {}", e);
            }

            JAXBElement<AcknowledgementType> jaxBAck = new ObjectFactory()
                    .createAcknowledgement(ack);
            marshaller.marshal(jaxBAck, byteArrayOutputStream);
            return byteArrayOutputStream;
        } catch (JAXBException e) {
            throw new CatalogTransformerException(e);
        }
    }

    public void init() {
        int numThreads = Runtime.getRuntime().availableProcessors();
        LOGGER.debug(QUERY_POOL_NAME + " size: {}", numThreads);

        /*
            - when first two args the same, get fixed size thread pool.
            - 3rd arg, keepAliveTime, ignored when !allowsCoreThreadTimeOut (the default); thus pass zero.
            - fixed (and arbitrarily) size blocking queue.
            - CswThreadFactory gives pool threads a name to ease debug.
            - tried arbitrarily large numThreads/queue-size, but did not see performance gain.
            - big queue + small pool minimizes CPU usage, OS resources, and context-switching overhead,
              but *can* lead to artificially low throughput.
            - todo: externalize config to support runtime tuning.
        */
        queryExecutor = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(BLOCKING_Q_INITIAL_SIZE), new CswThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        queryExecutor.prestartAllCoreThreads();
    }

    public void destroy() {
        queryExecutor.shutdown(); // wait for all submitted task to finish.

        // Block until shutdown() complete, or the timeout occurs, or
        // the current thread is interrupted, whichever happens first.
        try {
            queryExecutor.awaitTermination(2L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalStateException(QUERY_POOL_NAME + " graceful shutdown interrupted.", e);
        }
    }

    private static class CswThreadFactory implements ThreadFactory {
        private static AtomicInteger suffix = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, QUERY_POOL_NAME + "-" + suffix.incrementAndGet());
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.error("UNCAUGHT exception in thread " + t.getName(), e);
                }
            });
            return thread;
        }
    }
}
