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
package ddf.catalog.filter;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;

import ddf.catalog.source.UnsupportedQueryException;

/**
 * {@code FilterAdapter} visits a {@link Filter} and invokes
 * {@link FilterDelegate} methods with normalized, typed values. A large part of
 * implementing a {@link FilterVisitor} is handling expressions, type casting
 * literals, and supporting custom filter logic like {@code FuzzyFunction}s. The
 * {@code FilterAdapter} handles all of this common boilerplate functionality.
 * Furthermore, sources using the {@code FilterAdapter} will receive future
 * enhancements and ensure basic compatibility with Endpoints that use the
 * {@code FilterBuilder}.
 * <p>
 * To use the {@code FilterAdapter}, a {@code FilterDelegate} for the source is
 * required. Here is an example using a text base filter delegate.
 * 
 * <pre>
 * FilterDelegate&lt;String&gt; delegate = new FilterToTextDelegate();
 * FilterAdapter adapter = new FilterAdapterImpl();
 * 
 * String result = adapter.adapt(filter, delegate);
 * </pre>
 * 
 * A reference implementation is provided with the DDF Core in the Filter Proxy
 * bundle.
 * 
 * @author ddf.isgs@lmco.com
 * 
 * @see FilterDelegate
 */
public interface FilterAdapter {

	/**
	 * Visit {@code Filter} nodes and invoke {@code FilterDelegate} methods with
	 * normalized input.
	 * 
	 * @param filter
	 *            OGC Filter to visit
	 * @param filterDelegate
	 *            delegate to invoke will visiting
	 * @return result of adapted filter output
	 * @throws UnsupportedQueryException
	 */
	public <T> T adapt(Filter filter, FilterDelegate<T> filterDelegate)
			throws UnsupportedQueryException;

}
