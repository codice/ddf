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

package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import ddf.catalog.data.Metacard;

public class Wfs20FeatureCollection {
    private BigInteger numberReturned;
    private String numberMatched;
    private List<Metacard> members = new ArrayList<Metacard>();
    
    public BigInteger getNumberReturned() {
        return numberReturned;
    }
    public void setNumberReturned(BigInteger numberReturned) {
        this.numberReturned = numberReturned;
    }
    public String getNumberMatched() {
        return numberMatched;
    }
    public void setNumberMatched(String numberMatched) {
        this.numberMatched = numberMatched;
    }
    public List<Metacard> getMembers() {
        return members;
    }
    public void setMembers(List<Metacard> members) {
        this.members = members;
    }
    
}
