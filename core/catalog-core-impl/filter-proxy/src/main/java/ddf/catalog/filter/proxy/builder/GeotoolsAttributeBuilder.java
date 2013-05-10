/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.filter.proxy.builder;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ExpressionBuilder;

public final class GeotoolsAttributeBuilder extends GeotoolsExpressionBuilder
		implements AttributeBuilder {

	private static XLogger logger = new XLogger(
			LoggerFactory.getLogger(GeotoolsAttributeBuilder.class));

	GeotoolsAttributeBuilder(String attribute) {
		setAttribute(attribute);
		setOperator(Operator.EQ);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ddf.catalog.filter.AttributeBuilder#is()
	 */
	@Override
	public ExpressionBuilder is() {
		logger.debug("is: operator:" + getOperator() + ", attribute:"
				+ getAttribute());
		return this;
	}

}
