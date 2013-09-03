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
package ddf.catalog.plugin;



/**
 * The PluginExecutionException is used to signal errors during service operations.
 */
public class PluginExecutionException extends Exception {

    /**
     * Instantiates a new PluginExecutionException from a given string.
     *
     * @param string the string to use for the exception.
     */
    public PluginExecutionException(String string) {
	super(string);
    }

    /**
     * Instantiates a new PluginExecutionException.
     */
    public PluginExecutionException() {
	super();
    }

    /**
     * Instantiates a new PluginExecutionException with a message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public PluginExecutionException(String message, Throwable throwable) {
	super(message, throwable);
    }

    /**
     * Instantiates a new PluginExecutionExceptionn.
     *
     * @param throwable the throwable
     */
    public PluginExecutionException(Throwable throwable) {
	super(throwable);
    }

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

}
