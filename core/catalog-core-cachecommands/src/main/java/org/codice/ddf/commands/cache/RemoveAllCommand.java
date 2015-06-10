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
 */
package org.codice.ddf.commands.cache;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

/**
 *
 * Command to remove all or subset of records in the Metacard Cache.
 *
 */

@Command(scope = CacheCommands.NAMESPACE, name = "removeall", description = "Attempts to delete all Metacards from the cache.")
public class RemoveAllCommand extends CacheCommands {

    static final String WARNING_MESSAGE = "WARNING: This will remove all records from the cache.  Do you want to proceed? (yes/no): ";

    @Option(name = "-f", required = false, aliases = {"--force"}, multiValued = false, description = "Force the removal without a confirmation message.")
    boolean force = false;

    @Override
    protected Object doExecute() throws Exception {

        if (isAccidentalRemoval(console)) {
            return null;
        }

        long start = System.currentTimeMillis();

        getCacheProxy().removeAll();

        long end = System.currentTimeMillis();

        console.println();
        console.printf("Cache cleared in %3.3f seconds%n", (end - start) / MILLISECONDS_PER_SECOND);

        return null;

    }

    boolean isAccidentalRemoval(PrintStream console) throws IOException {
        if (!force) {
            StringBuffer buffer = new StringBuffer();
            System.err.println(String.format(WARNING_MESSAGE));
            System.err.flush();
            for (;;) {
                int byteOfData = session.getKeyboard().read();

                if (byteOfData < 0) {
                    // end of stream
                    return true;
                }
                System.err.print((char) byteOfData);
                if (byteOfData == '\r' || byteOfData == '\n') {
                    break;
                }
                buffer.append((char) byteOfData);
            }
            String str = buffer.toString();
            if (!str.equals("yes")) {
                console.println("No action taken.");
                return true;
            }
        }

        return false;
    }
}
