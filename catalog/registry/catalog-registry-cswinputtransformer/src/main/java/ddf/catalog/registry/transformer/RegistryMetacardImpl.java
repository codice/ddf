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
package ddf.catalog.registry.transformer;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class RegistryMetacardImpl extends MetacardImpl {

    private static final RegistryObjectMetacardType ROMT = new RegistryObjectMetacardType();

    public RegistryMetacardImpl() {
        super(ROMT);
    }

    public RegistryMetacardImpl(MetacardTypeImpl type) {
        super(type);
    }

    private String getValue(String key) {
        return (String) getAttribute(key).getValue();
    }

    public String getOrgName() {
        return getValue(RegistryServiceMetacardType.ORGANIZATION_NAME);
    }

    public String getOrgAddress() {
        return getValue(RegistryServiceMetacardType.ORGANIZATION_ADDRESS);
    }

    public String getOrgPhoneNumber() {
        return getValue(RegistryServiceMetacardType.ORGANIZATION_PHONE_NUMBER);
    }

    public String getOrgEmail() {
        return getValue(RegistryServiceMetacardType.ORGANIZATION_EMAIL);
    }

}
