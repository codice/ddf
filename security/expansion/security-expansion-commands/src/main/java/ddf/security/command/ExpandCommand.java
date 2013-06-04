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
package ddf.security.command;


import ddf.security.expansion.Expansion;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;

import java.util.List;
import java.util.Set;

/**
 * Implements the "expand" command - taking an attribute name and the current value, and prints out what the
 * expansion of that value is using the currently-configured expansion service.
 */
@Command( scope = "security", name = "expand", description = "Expands a given key and set of values." )
public class ExpandCommand extends OsgiCommandSupport
{
    @Argument( name = "key", description = "The of the value to be encrypted.", index = 0, multiValued = false, required = true )
    private String key = null;

    @Argument( name = "values", description = "The set of values to be expanded.", index = 1, multiValued = true, required = true )
    private Set<String> values = null;

    // live list of expansion services
    private List<Expansion> expansionList = null;


    /**
     * Called to execute the security:encrypt console command.
     */
    @Override
    protected Object doExecute() throws Exception
    {           
        if ((key == null) || (values == null))
        {
            return null;
        }

        if ((expansionList != null) && (!expansionList.isEmpty()))
        {
            for (Expansion expansion : expansionList)
            {
                Set<String> expandedValues = expansion.expand(key, values);
                System.out.print( Ansi.ansi().fg( Ansi.Color.YELLOW ).toString() );
                System.out.println( expandedValues );
                System.out.print( Ansi.ansi().fg( Ansi.Color.DEFAULT ).toString() );
            }
        } else
        {
            System.out.println("No expansion services currently available.");
        }
        return null;
    }
    
    public void setExpansionList(List<Expansion> list)
    {
       this.expansionList = list;
    }
}
