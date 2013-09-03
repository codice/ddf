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

import org.opengis.filter.Filter;

import ddf.catalog.filter.ContextualExpressionBuilder;

public final class GeotoolsContextualExpressionBuilder extends GeotoolsBuilder implements ContextualExpressionBuilder {

	GeotoolsContextualExpressionBuilder(GeotoolsBuilder builder) {
		super(builder);
	}

	/* (non-Javadoc)
	 * @see ddf.catalog.filter.proxy.builder.ContextualExpressionBuilder#text(java.lang.String)
	 */
	@Override
	public Filter text(String string) {
	    return build(string, false);
	}

	@Override
	public Filter fuzzyText(String string) {
	    setOperator(Operator.FUZZY);
	    return build(string, false);
	}

	@Override
	public Filter caseSensitiveText(String string) {
	    return build(string, true);
	}

}
