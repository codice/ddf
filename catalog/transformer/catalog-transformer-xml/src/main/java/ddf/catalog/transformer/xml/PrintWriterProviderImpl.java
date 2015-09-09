/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.transformer.xml;

import java.io.Writer;

import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;

public class PrintWriterProviderImpl implements PrintWriterProvider {

    @Override
    public PrintWriter build(Writer writer, Class klass) {
        PrintWriter printWriter = null;

        if (Metacard.class.equals(klass)) {
            printWriter = new EscapingPrintWriter(writer);
        }

        // todo: throw exception instead of return null ?

        return printWriter;
    }

}
