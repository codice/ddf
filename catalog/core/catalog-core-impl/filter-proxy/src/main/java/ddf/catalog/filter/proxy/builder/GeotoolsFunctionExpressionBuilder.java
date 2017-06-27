/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.filter.proxy.builder;

import java.util.ArrayList;
import java.util.Date;

import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPosition;

import ddf.catalog.filter.ArgumentBuilder;
import ddf.catalog.filter.EqualityExpressionBuilder;

public class GeotoolsFunctionExpressionBuilder extends GeotoolsBuilder implements ArgumentBuilder {

    public GeotoolsFunctionExpressionBuilder(String name) {
        setFunctionName(name);
        setArguments(new ArrayList<>());
    }

    @Override
    public ArgumentBuilder numberArg(float arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder numberArg(double arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder numberArg(int arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder numberArg(short arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder numberArg(long arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder wktArg(String wkt) {
        addLiteralWktArg(wkt);
        return this;
    }

    @Override
    public ArgumentBuilder dateArg(Date date) {
        addLiteralArg(date);
        return this;
    }

    @Override
    public ArgumentBuilder dateRangeArg(Date begin, Date end) {
        addLiteralArg(begin);
        addLiteralArg(end);
        return this;
    }

    @Override
    public ArgumentBuilder boolArg(boolean arg) {
        addLiteralArg(arg);
        return this;
    }

    @Override
    public ArgumentBuilder bytesArg(byte[] bytes) {
        addLiteralArg(bytes);
        return this;
    }

    @Override
    public ArgumentBuilder textArg(String text) {
        addLiteralArg(text);
        return this;
    }

    @Override
    public ArgumentBuilder objArg(Object obj) {
        addLiteralArg(obj);
        return this;
    }

    @Override
    public ArgumentBuilder attributeArg(String name) {
        addAttribute(name);
        return this;
    }

    protected void addLiteralWktArg(String wkt) {
        addLiteralArg(toGeometry(wkt));
    }

    protected void addLiteralArg(Date date) {
        getArguments().add(getFactory().literal(new DefaultInstant(new DefaultPosition(date))));
    }

    protected void addLiteralArg(Object value) {
        getArguments().add(getFactory().literal(value));
    }

    protected void addAttribute(String value) {
        getArguments().add(getFactory().property(value));

    }

    @Override
    public EqualityExpressionBuilder equalTo() {
        setOperator(Operator.EQ);
        return new GeotoolsEqualityExpressionBuilder(this);
    }

}
