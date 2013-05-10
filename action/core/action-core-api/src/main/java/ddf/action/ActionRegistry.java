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
package ddf.action;

import java.util.List;

/**
 * This class is used to find all {@link Action} objects that correspond to a
 * certain input object.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public interface ActionRegistry {


	/**
	 * Used to retrieve all actions that can be applied to a given input.
	 * 
	 * @param subject
	 *            object in which an {@link Action} can be applied
	 * @return all {@link Action} objects that can be applied from the given
	 *         input, otherwise an empty list if no actions can be applied.
	 * 
	 */
	public <T> List<Action> list(T subject);
}
