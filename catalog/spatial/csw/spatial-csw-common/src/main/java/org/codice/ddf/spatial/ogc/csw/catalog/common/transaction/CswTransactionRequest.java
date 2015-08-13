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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single CSW transaction request that can contain multiple inserts, updates, and
 * deletes.
 */
public class CswTransactionRequest {

    private String version;

    private String service;

    private boolean verbose;

    private final List<InsertTransaction> insertTransactions = new ArrayList<>();

    private final List<DeleteTransaction> deleteTransactions = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public List<InsertTransaction> getInsertTransactions() {
        return insertTransactions;
    }

    public void addInsertTransaction(InsertTransaction insertTransaction) {
        insertTransactions.add(insertTransaction);
    }

    public List<DeleteTransaction> getDeleteTransactions() {
        return deleteTransactions;
    }

    public void addDeleteTransaction(DeleteTransaction deleteTransaction) {
        deleteTransactions.add(deleteTransaction);
    }
}
