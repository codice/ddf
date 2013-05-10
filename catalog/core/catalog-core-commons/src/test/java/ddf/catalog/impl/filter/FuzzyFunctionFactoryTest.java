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

import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

public class FuzzyFunctionFactoryTest {
	FuzzyFunctionFactory toTest;
	
	@Before
	public void setup(){
		toTest = new FuzzyFunctionFactory();
	}
	
	@Test
	public void testGetFunctionNames() {
		List<FunctionName> funcs = toTest.getFunctionNames();
		assertEquals(1, funcs.size());
		assertEquals("fuzzy", funcs.get(0).getName());
	}
	
	@Test
	public void testFunctionForUnimplementedName(){
		assertNull(toTest.function("", null, null));
	}
	
	@Test(expected=NullPointerException.class)
	public void testFunctionForOnlyValidNameWithNullExpressionList(){
		toTest.function("fuzzy", null, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFunctionForOnlyValidNameWithTooManyExpressions(){
		List<Expression> expr = new ArrayList<Expression>();
		expr.add(Expression.NIL);
		expr.add(Expression.NIL);
		Function result = toTest.function("fuzzy", expr, null);
		assertEquals("fuzzy", result.getName());
	}
	
	@Test
	public void testFunctionForOnlyValidName(){
		List<Expression> expr = new ArrayList<Expression>();
		expr.add(Expression.NIL);
		Function result = toTest.function("fuzzy", expr, null);
		assertEquals("fuzzy", result.getName());
	}

}
