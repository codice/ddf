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

package org.codice.ddf.spatial.ogc.csw.catalog.source;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.ows.v_1_0_0.DCP;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.RequestMethodType;

import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.filter.FilterDelegate;

/**
 * CswDelegate is an abstract implementation of a {@link FilterDelegate}. It extends the
 * FilterDelegate with support for understanding the getRecords operation of the CSW 2.0.2
 * specification. CswDelegate captures the capabilities and supported formats for the particular
 * server, to be leveraged by supported CSW query languages.
 * 
 * @param <T>
 *            Generic type that the FilterDelegate will return as a final result
 */

public abstract class CswAbstractFilterDelegate<T> extends FilterDelegate<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswAbstractFilterDelegate.class);

    private String postUri;

    private String getUri;

    // HITS/RESULTS/VALIDATE. Hits can be used to get total count, results for
    // actual cards
    private Set<ResultType> resultTypes;

    private Set<String> formats;

    /**
     * Invoked by concrete implementations of CswDelegate
     * 
     * @param getRecordsOp
     *            An {@link Operation} for the getRecords feature of the Csw service
     * @param outputFormatValues
     *            An {@link DomainType} containing a list of valid Output Formats supported
     * @param resultTypesValues
     *            An {@link DomainType} containing a list of Result Types supported
     */
    public CswAbstractFilterDelegate(Operation getRecordsOp, DomainType outputFormatValues,
            DomainType resultTypesValues) {
        formats = new HashSet<String>();
        resultTypes = EnumSet.noneOf(ResultType.class);

        readUrls(getRecordsOp);

        if (null != outputFormatValues) {
            formats.addAll(outputFormatValues.getValue());
        }

        if (null != resultTypesValues) {
            for (String rt : resultTypesValues.getValue()) {
                try {
                    resultTypes.add(ResultType.fromValue(rt.toLowerCase()));
                } catch (IllegalArgumentException iae) {
                    LOGGER.warn("\"{}\" is not a ResultType.  Exception: {}", rt, iae);
                }
            }
        }

        formats = Collections.unmodifiableSet(formats);
        resultTypes = Collections.unmodifiableSet(resultTypes);
    }

    public String getPostUri() {
        return postUri;
    }

    public String getGetUri() {
        return getUri;
    }

    private void readUrls(Operation getRecordsOp) {
        List<DCP> dcp = getRecordsOp.getDCP();
        // only supports 1 DCP, and that is HTTP
        if (!CollectionUtils.isEmpty(dcp)) {
            List<JAXBElement<RequestMethodType>> methods = dcp.get(0).getHTTP().getGetOrPost();
            for (JAXBElement<RequestMethodType> method : methods) {
                if (CswConstants.POST.equals(method.getName())) {
                    postUri = method.getValue().getHref();
                } else if (CswConstants.GET.equals(method.getName())) {
                    getUri = method.getValue().getHref();
                }
            }
        }
    }

    protected Set<String> getFormats() {
        return formats;
    }

    protected Set<ResultType> getResultTypes() {
        return resultTypes;
    }
}
