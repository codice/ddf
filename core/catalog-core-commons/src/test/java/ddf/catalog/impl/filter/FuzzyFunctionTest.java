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
package ddf.catalog.impl.filter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.opengis.filter.expression.Expression;

import org.junit.Test;

public class FuzzyFunctionTest {

	@Test(expected=NullPointerException.class)
	public void testVerifyFuzzyFunctionCannotBeCalledWithNull() {
		// When: I try to create a Fuzzy Function with null parameters
		FuzzyFunction func = new FuzzyFunction(null,null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testVerifyFuzzyFunctionCannotBeCalledWithMoreThanOneParameter() {
		List<Expression> exprs = new ArrayList<Expression>();
		exprs.add(Expression.NIL);
		exprs.add(Expression.NIL);
		// When: I try to create a Fuzzy Function with null parameters
		FuzzyFunction func = new FuzzyFunction(exprs,null);
	}
	
	@Test
	public void testVerifyFuzzyFunction() {
		List<Expression> exprs = new ArrayList<Expression>();
		exprs.add(Expression.NIL);
		// When: I try to create a Fuzzy Function with null parameters
		FuzzyFunction func = new FuzzyFunction(exprs,null);
		assertEquals("fuzzy", FuzzyFunction.NAME.getName());
	}

}
