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
package org.codice.ddf.commands.catalog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Class is used to intercept System.out to verify what has been written to the PrintStream by
 * various Commands.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class ConsoleOutput {

    private ByteArrayOutputStream buffer;

    private PrintStream realSystemOut;

    public void interceptSystemOut() {
        this.realSystemOut = System.out;

        this.buffer = new ByteArrayOutputStream();

        System.setOut(new PrintStream(this.buffer));

    }

    public void closeBuffer() throws IOException {

        buffer.close();
    }

    public String getOutput() {
        return buffer.toString();
    }

    public void resetSystemOut() {
        System.setOut(realSystemOut);
    }
}