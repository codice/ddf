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
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.fusesource.jansi.Ansi;

import java.util.List;
import java.util.Map;


@Command( scope = "security", name = "expansions", description = "Dumps the current expansion tables." )
public class ExpansionsCommand extends OsgiCommandSupport
{
    private List<Expansion> expansionList = null;

    /**
     * Called to execute the security:encrypt console command.
     */
    @Override
    protected Object doExecute() throws Exception
    {           
        if ((expansionList != null) && (!expansionList.isEmpty()))
        {
            for (Expansion expansion : expansionList)
            {
                Map<String, List<String[]>> map = expansion.getExpansionMap();
                System.out.print( Ansi.ansi().fg( Ansi.Color.YELLOW ).toString() );
                if ((map != null) && (!map.isEmpty()))
                {
                    for (String key : map.keySet())
                    {
                        for (String[] mapping : map.get(key))
                        {
                            System.out.println(key + " : " + mapping[0] + " : " + mapping[1]);
                        }
                    }
                }
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
