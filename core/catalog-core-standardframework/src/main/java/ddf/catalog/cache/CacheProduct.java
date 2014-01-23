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
package ddf.catalog.cache;

import java.io.InputStream;


public class CacheProduct {
    
    private int hashCode;
    private InputStream inputStream;
    
    public CacheProduct(int hashCode, InputStream inputStream) {
        this.hashCode = hashCode;
        this.inputStream = inputStream;
    }

    public int getHashCode() {
        return hashCode;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

}
