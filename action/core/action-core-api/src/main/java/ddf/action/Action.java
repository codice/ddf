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
package ddf.action;

import java.net.URL;

/**
 * An {@link Action} has a {@link URL} meant to be used in a browser or be able to be invoked to
 * provide some resource or business logic. An example would be providing a link to a product or a
 * link to calculate information about a specific resource.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public interface Action {

    /**
     * 
     * @return the unique identifier of the {@link ActionProvider} that supplied the {@link Action}
     * 
     * @see ActionProvider
     */
    public String getId();

    /**
     * 
     * @return {@link URL} object that provides business logic when invoked. This could be used as
     *         the href of a html hyperlink.
     */
    public URL getUrl();

    /**
     * 
     * @return a title that provides a brief name or label for this {@link Action}. Title can be
     *         used as the hyperlink text for a HTML hyperlink.
     */
    public String getTitle();

    /**
     * 
     * @return a description or concise statement of what this {@link Action} does and the expected
     *         result when the {@link Action} is invoked. The description, for example, could be
     *         used to provide more information when a link is hovered upon.
     */
    public String getDescription();

}
