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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateAction;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

import ddf.catalog.data.impl.MetacardImpl;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

public class TestTransactionRequestConverter {

    String expectedInsertXML =
            "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                    + "  <csw:Insert typeName=\"csw:Record\"/>\n" + "</csw:Transaction>";

    String expectedUpdateXML =
            "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                    + "  <csw:Update typeName=\"csw:Record\"/>\n" + "</csw:Transaction>";

    String expectedDeleteXML =
            "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                    + "  <csw:Delete typeName=\"csw:Record\">\n"
                    + "    <csw:Constraint version=\"1.1.0\">\n"
                    + "      <ogc:CqlText>identifier = metacard1</ogc:CqlText>\n"
                    + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";

    String expectedMultiOpXml = "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
            + "  <csw:Insert typeName=\"csw:Record\"/>\n"
            + "  <csw:Update typeName=\"csw:Record\" reference=\"../csw:Insert\"/>\n"
            + "  <csw:Delete typeName=\"csw:Record\">\n"
            + "    <csw:Constraint version=\"1.1.0\">\n"
            + "      <ogc:CqlText>identifier = metacard1</ogc:CqlText>\n"
            + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";

    Converter cswRecordConverter;

    XStream xStream;

    @Before
    public void setup() {
        cswRecordConverter = mock(Converter.class);
        when(cswRecordConverter.canConvert(any())).thenReturn(true);
        xStream = new XStream(new Xpp3Driver());
        xStream.registerConverter(new TransactionRequestConverter(cswRecordConverter));
        xStream.alias(CswConstants.CSW_TRANSACTION, CswTransactionRequest.class);
    }

    @Test
    public void testValidInsertMarshal() throws SAXException, IOException, XpathException {

        CswTransactionRequest transactionRequest = new CswTransactionRequest();

        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("metacard1");
        InsertAction insertAction = new InsertAction("csw:Record", null, Arrays.asList(metacard));
        transactionRequest.getInsertActions()
                .add(insertAction);
        transactionRequest.setService(CswConstants.CSW);
        transactionRequest.setVerbose(true);
        transactionRequest.setVersion(CswConstants.VERSION_2_0_2);

        String xml = xStream.toXML(transactionRequest);
        Diff diff = XMLUnit.compareXML(xml, expectedInsertXML);
        assertTrue(diff.similar());
    }

    @Test
    public void testValidUpdateMarshal() throws SAXException, IOException, XpathException {

        CswTransactionRequest transactionRequest = new CswTransactionRequest();

        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("metacard1");
        UpdateAction updateAction = new UpdateAction(metacard, "csw:Record", null);
        transactionRequest.getUpdateActions()
                .add(updateAction);
        transactionRequest.setService(CswConstants.CSW);
        transactionRequest.setVerbose(true);
        transactionRequest.setVersion(CswConstants.VERSION_2_0_2);

        String xml = xStream.toXML(transactionRequest);
        Diff diff = XMLUnit.compareXML(xml, expectedUpdateXML);
        assertTrue(diff.similar());
    }

    @Test
    public void testValidDeleteMarshal() throws SAXException, IOException, XpathException {

        CswTransactionRequest transactionRequest = new CswTransactionRequest();

        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("metacard1");
        DeleteType deleteType = new DeleteType();
        QueryConstraintType queryConstraintType = new QueryConstraintType();
        queryConstraintType.setCqlText("identifier = metacard1");
        deleteType.setConstraint(queryConstraintType);
        DeleteAction deleteAction = new DeleteAction(deleteType, null);
        transactionRequest.getDeleteActions()
                .add(deleteAction);
        transactionRequest.setService(CswConstants.CSW);
        transactionRequest.setVerbose(true);
        transactionRequest.setVersion(CswConstants.VERSION_2_0_2);

        String xml = xStream.toXML(transactionRequest);
        Diff diff = XMLUnit.compareXML(xml, expectedDeleteXML);
        assertTrue(diff.similar());
    }

    @Test
    public void testMultipleOperations() throws Exception {
        CswTransactionRequest transactionRequest = new CswTransactionRequest();

        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("metacard1");

        transactionRequest.setService(CswConstants.CSW);
        transactionRequest.setVerbose(true);
        transactionRequest.setVersion(CswConstants.VERSION_2_0_2);

        InsertAction insertAction = new InsertAction("csw:Record", null, Arrays.asList(metacard));
        transactionRequest.getInsertActions()
                .add(insertAction);
        UpdateAction updateAction = new UpdateAction(metacard, "csw:Record", null);
        transactionRequest.getUpdateActions()
                .add(updateAction);
        DeleteType deleteType = new DeleteType();
        QueryConstraintType queryConstraintType = new QueryConstraintType();
        queryConstraintType.setCqlText("identifier = metacard1");
        deleteType.setConstraint(queryConstraintType);
        DeleteAction deleteAction = new DeleteAction(deleteType, null);
        transactionRequest.getDeleteActions()
                .add(deleteAction);

        String xml = xStream.toXML(transactionRequest);
        Diff diff = XMLUnit.compareXML(xml, expectedMultiOpXml);
        assertTrue(diff.similar());
    }
}
