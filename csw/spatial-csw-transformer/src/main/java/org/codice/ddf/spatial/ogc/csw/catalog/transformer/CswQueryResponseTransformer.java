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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GetRecordsResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ddf.catalog.transform.QueryResponseTransformer} for CSW 2.0.2
 * GetRecordsResponse
 */
public class CswQueryResponseTransformer implements QueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(CswQueryResponseTransformer.class);

    private XStream xstreamGetRecordsResponse;

    private XStream xstreamGetRecordByIdResponse;

    public CswQueryResponseTransformer(CswTransformProvider provider) {
        xstreamGetRecordsResponse = initXstream(CswConstants.GET_RECORDS_RESPONSE, provider);
        xstreamGetRecordByIdResponse = initXstream(CswConstants.GET_RECORD_BY_ID_RESPONSE,
                provider);
    }

    @Override public BinaryContent transform(SourceResponse sourceResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        LOGGER.debug("Entering CswQueryResponseTransformer.transform()");
        if (sourceResponse.getResults() == null && arguments.get(CswConstants.RESULT_TYPE_PARAMETER) == null) {
            LOGGER.warn("Attempted to Transform and empty Result list.");
            return null;
        }

        BinaryContent transformedContent = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        LOGGER.debug("Creating recordCollection");
        CswRecordCollection recordCollection = new CswRecordCollection();
        for (Result result : sourceResponse.getResults()) {
            recordCollection.getCswRecords().add(result.getMetacard());
        }

        recordCollection.setNumberOfRecordsMatched(sourceResponse.getHits());
        recordCollection.setNumberOfRecordsReturned(sourceResponse.getResults().size());
        recordCollection.setStartPosition(sourceResponse.getRequest().getQuery().getStartIndex());

        LOGGER.debug("Evaluating Arguments");
        evaluateArguments(arguments, recordCollection);

        if (recordCollection.isById()) {
            LOGGER.debug("Transforming GetRecordByIdResponse");
            try {
                xstreamGetRecordByIdResponse.toXML(recordCollection, os);
            } catch (XStreamException e) {
                throw new CatalogTransformerException(e);
            }
            // TODO - should use recordCollection.getResultType() here
        } else if (recordCollection.isValidateQuery()) {
            LOGGER.debug("Transforming Acknowledgement");
            try {
                writeAcknowledgement(recordCollection.getRequest(), os);
            } catch (IOException e) {
                throw new CatalogTransformerException(e);
            }
        } else {
            LOGGER.debug("Transforming GetRecordsResponse");
            try {
                xstreamGetRecordsResponse.toXML(recordCollection, os);
            } catch (XStreamException e) {
                LOGGER.warn("Failed to transform GetRecordsResponse", e);
                throw new CatalogTransformerException(e);
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());
        transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }

    /*
     * Method to handle all the arguments and update the recordCollection based on which arguments are provided.
     */
    private void evaluateArguments(Map<String, Serializable> arguments,
            CswRecordCollection recordCollection) {
        if (arguments != null) {
            Object elementSetTypeArg = arguments.get(CswConstants.ELEMENT_SET_TYPE);
            if (elementSetTypeArg != null && StringUtils.isNotBlank((String) elementSetTypeArg)) {
                ElementSetType elementSetType = ElementSetType.valueOf((String) elementSetTypeArg);
                recordCollection.setElementSetType(elementSetType);
            }

            Object elementNamesArg = arguments.get(CswConstants.ELEMENT_NAMES);
            if (elementNamesArg instanceof QName[]) {
                QName[] qnames = (QName[]) elementNamesArg;
                if (qnames.length > 0) {
                    List<QName> elementNames = new ArrayList<QName>();
                    for (QName entry : qnames) {
                        elementNames.add(entry);
                    }
                    recordCollection.setElementName(elementNames);
                }
            }

            Object isByIdQuery = arguments.get(CswConstants.IS_BY_ID_QUERY);
            if (isByIdQuery != null) {
                recordCollection.setById((Boolean)isByIdQuery);
            }

            Object isValidateQuery = arguments.get(CswConstants.IS_VALIDATE_QUERY);
            if (isValidateQuery != null) {
                recordCollection.setValidateQuery((Boolean)isValidateQuery);
                Object arg = arguments.get((CswConstants.GET_RECORDS));
                if (arg != null && arg instanceof GetRecordsType){
                    recordCollection.setRequest((GetRecordsType) arg);
                }
            }

            Object resultType = arguments.get(CswConstants.RESULT_TYPE_PARAMETER);
            if (resultType != null) {
                recordCollection.setResultType(ResultType.fromValue((String)resultType));
            }

        }
    }

    private XStream initXstream(final String elementName, CswTransformProvider provider) {
        XStream xstream = new XStream(new StaxDriver(new NoNameCoder()));
        xstream.setClassLoader(xstream.getClass().getClassLoader());

        GetRecordsResponseConverter cswGetRecordsResponseConverter = new GetRecordsResponseConverter(
                provider);

        xstream.registerConverter(cswGetRecordsResponseConverter);

        xstream.alias(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                + elementName, CswRecordCollection.class);

        return xstream;
    }

    private void writeAcknowledgement(GetRecordsType request, OutputStream outStream)
            throws IOException {
        try {
            JAXBContext jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:" +
                    "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0");
            Marshaller marshaller = jaxBContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            AcknowledgementType ack = new AcknowledgementType();
            EchoedRequestType echoedRequest = new EchoedRequestType();
            JAXBElement<GetRecordsType> jaxBRequest = new ObjectFactory().createGetRecords(request);
            echoedRequest.setAny(jaxBRequest);
            ack.setEchoedRequest(echoedRequest);
            try {
                ack.setTimeStamp(
                        DatatypeFactory.newInstance()
                                .newXMLGregorianCalendar(new GregorianCalendar()));
            } catch (DatatypeConfigurationException e) {
                LOGGER.warn("Failed to set timestamp on Acknowledgement, Exception {}", e);
            }

            JAXBElement<AcknowledgementType> jaxBAck = new ObjectFactory()
                    .createAcknowledgement(ack);
            marshaller.marshal(jaxBAck, outStream);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
}
