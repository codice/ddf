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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.codice.ddf.catalog.subscriptionstore.SubscriptionMetadata;
import org.codice.ddf.catalog.subscriptionstore.internal.SubscriptionFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswQueryFactory;

import ddf.catalog.event.Subscription;
import ddf.catalog.operation.QueryRequest;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;

/**
 * Encapsulate the creation of {@link CswSubscription}s from original requests.
 */
public class CswSubscriptionFactory implements SubscriptionFactory<CswSubscription> {

    private final TransformerManager mimeTypeTransformerManager;

    private final CswQueryFactory cswQueryFactory;

    public CswSubscriptionFactory(TransformerManager mimeTypeTransformerManager,
            CswQueryFactory cswQueryFactory) {
        this.mimeTypeTransformerManager = mimeTypeTransformerManager;
        this.cswQueryFactory = cswQueryFactory;
    }

    @Override
    public CswSubscription createSubscription(String originalRequestSerialized) {
        GetRecordsType originalRequest = regenerateGetRecordsType(originalRequestSerialized);
        return createCswSubscription(originalRequest);
    }

    public CswSubscription createCswSubscription(GetRecordsType originalRequest) {
        try {
            QueryRequest queryRequest = cswQueryFactory.getQuery(originalRequest);
            if (((QueryType) originalRequest.getAbstractQuery()
                    .getValue()).getConstraint() == null) {
                return CswSubscription.getFilterlessSubscription(originalRequest,
                        queryRequest,
                        mimeTypeTransformerManager);
            }
            return new CswSubscription(originalRequest, queryRequest, mimeTypeTransformerManager);
        } catch (CswException e) {
            throw new RuntimeException("CSW Exception when regenerating subscription. ", e);
        }
    }

    public String writeoutGetRecordsType(GetRecordsType originalRequest) {
        ObjectFactory objectFactory = new ObjectFactory();
        String messageBody;
        try {
            StringWriter sw = new StringWriter();
            CswQueryFactory.getJaxBContext()
                    .createMarshaller()
                    .marshal(objectFactory.createGetRecords(originalRequest), sw);
            messageBody = sw.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Error writing out GetRecordsType using JAXB. ", e);
        }

        if (messageBody == null) {
            throw new RuntimeException(
                    "Error writing GetRecordsType to string, result was null. ");
        }

        return messageBody;
    }

    private GetRecordsType regenerateGetRecordsType(String originalRequestSerialized) {
        try (StringReader sr = new StringReader(originalRequestSerialized)) {
            Unmarshaller unmarshaller = CswQueryFactory.getJaxBContext()
                    .createUnmarshaller();
            JAXBElement<GetRecordsType> jaxbElement =
                    (JAXBElement<GetRecordsType>) unmarshaller.unmarshal(sr);
            return jaxbElement.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException("Error regenerating GetRecordsType using JAXB. ", e);
        }
    }
}
