/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.util.impl;

import ddf.catalog.util.Maskable;

/**
 * Implementation of the Maskable interface that provides methods to set the masked ID of the item
 * and the ID of the item if it has not yet been masked. Once the item's ID is masked it cannot be
 * updated again. At that point, you can only change its value indirectly by changing the site name
 * of DDF.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public class MaskableImpl extends DescribableImpl implements Maskable {

    private boolean masked = false;

    /**
     * Sets the masked ID of this maskable item to the specified ID, and then sets an internal flag
     * indicating that the masked ID cannot be changed.
     */
    @Override
    public void maskId(String id) {
        synchronized (this) {
            super.setId(id);
            masked = true;
        }
    }

    /**
     * Sets the ID of the maskable item if it is not currently masked. If this item is already
     * masked, then its ID is not updated to the specified input ID.
     */
    @Override
    public void setId(String id) {
        synchronized (this) {
            if (!masked) {
                super.setId(id);
            }
        }
    }

}
