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
package ddf.catalog.transformer.xml.adapter;

import java.util.LinkedList;
import java.util.List;

public class AdaptedList<T, T2 extends T> extends LinkedList<T2> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7192930446368854263L;

	@SuppressWarnings("unchecked")
	public AdaptedList(List<T> source, Class<T2> tClass) {
		super();
		for (T entry : source) {
			if (entry == null) {
				this.add(null);
			} else if (entry.getClass().isAssignableFrom(tClass)) {
				this.add((T2) entry);
			}
		}
	}

}
