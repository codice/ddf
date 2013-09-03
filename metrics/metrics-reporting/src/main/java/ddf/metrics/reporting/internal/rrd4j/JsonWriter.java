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
package ddf.metrics.reporting.internal.rrd4j;

import java.io.StringWriter;


/**
 * Writer to pretty print JSON.
 * 
 * This writer pretty prints JSON-formatted text to the a format where each JSON attribute 
 * and its value is on its own line, and JSON arrays are printed such that each array element
 * has each of its own attributes on separate lines.
 * 
 * Example of pretty-printed output:
 * <pre>
 * {@code
 * {
 *    "title":"Query Count for Jul 9 1998 09:00:00 to Jul 9 1998 09:50:00",
 *    "totalCount":322,
 *    "data":[
 *       {
 *          "timestamp":"Jul 9 1998 09:20:00",
 *          "value":54
 *       },
 *       {
 *          "timestamp":"Jul 9 1998 09:45:00",
 *          "value":51
 *       }
 *    ]
 *   }
 * }
 * </pre>
 * 
 * 
 * @author rodgersh
 *
 */
public class JsonWriter extends StringWriter 
{
    private int indent = 0;
    

    @Override
    public void write(int c) 
    {
        if (((char)c) == '[' || ((char)c) == '{') 
        {
            super.write(c);
            super.write('\n');
            indent++;
            writeIndentation();
        } 
        else if (((char)c) == ',') 
        {
            super.write(c);
            super.write('\n');
            writeIndentation();
        } 
        else if (((char)c) == ']' || ((char)c) == '}') 
        {
            super.write('\n');
            indent--;
            writeIndentation();
            super.write(c);
        } 
        else 
        {
            super.write(c);
        }
    }

    
    private void writeIndentation() 
    {
        for (int i = 0; i < indent; i++) 
        {
            super.write("   ");
        }
    }
}
