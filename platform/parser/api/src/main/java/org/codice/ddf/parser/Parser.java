/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.parser;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Defines a service interface for converting between Objects and external representations of them.
 * Objects can be represented in myriad ways for transport and interoperability with other systems;
 * this service is intended to isolate some of the more particular needs of various converision
 * libraries.
 */
public interface Parser {

    /**
     * Creates an initial configurator object to be used to facilitate the marshaling and
     * unmarshaling processes.
     *
     * @param contextPath a list of paths that implementations can search in order to find binding
     *                    information
     * @param loader the classloader for the parser to use
     * @return a configuration object with the specified {@code contextPath} and {@code loader}
     */
    ParserConfigurator configureParser(List<String> contextPath, ClassLoader loader);

    /**
     * Converts an object graph into the appropriate output format, writing it to the given stream.
     *
     * @param configurator object containing the relevant configuration information needed to
     *                     perform the conversion
     * @param obj the root of the object graph to convert
     * @param os the output stream on which the converted object is written
     * @throws ParserException
     */
    void marshal(ParserConfigurator configurator, Object obj, OutputStream os)
            throws ParserException;

    /**
     * Converts a representation of an object graph into an instance of type {@code T}.
     *
     * @param configurator object containing the relevant configuration information needed to
     *                     perform the conversion
     * @param cls {@code Class} for the conversion
     * @param stream input stream which is read for the object data
     * @param <T> expected return object
     * @return an object of type {@code T} as read and converted from the stream
     * @throws ParserException
     */
    <T> T unmarshal(ParserConfigurator configurator, Class<? extends T> cls, InputStream stream)
            throws ParserException;
}
