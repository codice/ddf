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
package org.codice.ddf.catalog.ui.metacard.workspace;

import java.util.Collections;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class SharingMetacardImpl extends MetacardImpl {

    private static final SharingMetacardTypeImpl TYPE = new SharingMetacardTypeImpl();

    public SharingMetacardImpl(Metacard metacard) {
        super(metacard);
    }

    public SharingMetacardImpl() {
        super(TYPE);
        setTags(Collections.singleton(SharingMetacardTypeImpl.SHARING_TAG));
    }

    /**
     * Check if a given metacard is a sharing metacard by checking the tags metacard attribute.
     *
     * @param metacard
     * @return
     */
    public static boolean isSharingMetacard(Metacard metacard) {
        if (metacard != null) {
            return metacard.getTags()
                    .stream()
                    .filter(SharingMetacardTypeImpl.SHARING_TAG::equals)
                    .findFirst()
                    .isPresent();
        }

        return false;
    }

    /**
     * Wrap any metacard as a SharingMetacardImpl.
     *
     * @param metacard
     * @return
     */
    public static SharingMetacardImpl from(Metacard metacard) {
        return new SharingMetacardImpl(metacard);
    }

    public String getSharingAttribute() {
        return (String) getAttribute(SharingMetacardTypeImpl.SHARING_ATTRIBUTE).getValue();
    }

    public SharingMetacardImpl setSharingAttribute(String attribute) {
        setAttribute(SharingMetacardTypeImpl.SHARING_ATTRIBUTE, attribute);
        return this;
    }

    public String getAction() {
        return (String) getAttribute(SharingMetacardTypeImpl.SHARING_ACTION).getValue();
    }

    public SharingMetacardImpl setAction(String action) {
        setAttribute(SharingMetacardTypeImpl.SHARING_ACTION, action);
        return this;
    }

    public boolean canView() {
        return SharingMetacardTypeImpl.SHARING_ACTION_EDIT.equals(getAction())
                || SharingMetacardTypeImpl.SHARING_ACTION_VIEW.equals(getAction());
    }

    public boolean canEdit() {
        return SharingMetacardTypeImpl.SHARING_ACTION_EDIT.equals(getAction());
    }

    public String getValue() {
        return (String) getAttribute(SharingMetacardTypeImpl.SHARING_VALUE).getValue();
    }

    public SharingMetacardImpl setValue(String value) {
        setAttribute(SharingMetacardTypeImpl.SHARING_VALUE, value);
        return this;
    }
}
