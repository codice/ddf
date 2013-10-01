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

package org.codice.ddf.ui.searchui.search.properties;

/**
 * Stores external configuration properties.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */

public class ConfigurationStore {

    private static ConfigurationStore uniqueInstance;

    private String header = "";

    private String footer = "";

    private String color = "";

    private String background = "";

    private ConfigurationStore() {
        header = "";
        footer = "";
        color = "";
        background = "";
    }

    /**
     * @return a unique instance of {@link ConfigurationStore}
     */
    public static synchronized ConfigurationStore getInstance() {

        if (uniqueInstance == null) {
            uniqueInstance = new ConfigurationStore();
        }

        return uniqueInstance;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
