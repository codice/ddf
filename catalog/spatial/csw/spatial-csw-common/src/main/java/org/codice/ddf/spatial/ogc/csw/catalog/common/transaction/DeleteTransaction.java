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

package org.codice.ddf.spatial.ogc.csw.catalog.common.transaction;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;

import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

public class DeleteTransaction extends CswTransaction {
    private QueryConstraintType queryConstraintType;

    private Map<String, String> prefixToUriMappings;

    public DeleteTransaction(DeleteType deleteType, Map<String, String> prefixToUriMappings) {
        typeName = StringUtils.defaultIfEmpty(deleteType.getTypeName(), CswConstants.CSW_RECORD);
        handle = StringUtils.defaultIfEmpty(deleteType.getHandle(), "");
        queryConstraintType = deleteType.getConstraint();
        this.prefixToUriMappings = prefixToUriMappings;
    }

    public QueryConstraintType getConstraint() {
        return queryConstraintType;
    }

    public Map<String, String> getPrefixToUriMappings() {
        return prefixToUriMappings;
    }
}
