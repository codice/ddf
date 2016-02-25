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
 */
package ddf.catalog.nato.stanag4559.server.impl.managers;

import ddf.catalog.nato.stanag4559.common.GIAS.Association;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeInformation;
import ddf.catalog.nato.stanag4559.common.GIAS.ConceptualAttributeType;
import ddf.catalog.nato.stanag4559.common.GIAS.DataModelMgrPOA;
import ddf.catalog.nato.stanag4559.common.GIAS.Library;
import ddf.catalog.nato.stanag4559.common.GIAS.View;
import ddf.catalog.nato.stanag4559.common.UCO.AbsTime;
import ddf.catalog.nato.stanag4559.common.UCO.Date;
import ddf.catalog.nato.stanag4559.common.UCO.EntityGraph;
import ddf.catalog.nato.stanag4559.common.UCO.InvalidInputParameter;
import ddf.catalog.nato.stanag4559.common.UCO.NameName;
import ddf.catalog.nato.stanag4559.common.UCO.NameValue;
import ddf.catalog.nato.stanag4559.common.UCO.ProcessingFault;
import ddf.catalog.nato.stanag4559.common.UCO.SystemFault;
import ddf.catalog.nato.stanag4559.common.UCO.Time;
import ddf.catalog.nato.stanag4559.server.data.AttributeInformationGenerator;

import org.omg.CORBA.NO_IMPLEMENT;

public class DataModelMgrImpl extends DataModelMgrPOA {

    private static final AbsTime LAST_UPDATED = new AbsTime(new Date((short) 2,
            (short) 9,
            (short) 16), new Time((short) 2, (short) 0, (short) 0));

    private static final short MAX_VERTICES = 10;

    @Override
    public AbsTime get_data_model_date(NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return LAST_UPDATED;
    }

    @Override
    public String[] get_alias_categories(NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return new String[0];
    }

    @Override
    public NameName[] get_logical_aliases(String category, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return new NameName[0];
    }

    @Override
    public String get_logical_attribute_name(String view_name,
            ConceptualAttributeType attribute_type, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return "";
    }

    @Override
    public View[] get_view_names(NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return AttributeInformationGenerator.generateViewNames();
    }

    @Override
    public AttributeInformation[] get_attributes(String view_name, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return AttributeInformationGenerator.getAttributesForView(view_name);
    }

    @Override
    public AttributeInformation[] get_queryable_attributes(String view_name, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return AttributeInformationGenerator.getAttributesForView(view_name);
    }

    @Override
    public EntityGraph get_entities(String view_name, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return new EntityGraph();
    }

    @Override
    public AttributeInformation[] get_entity_attributes(String aEntity, NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return new AttributeInformation[0];
    }

    @Override
    public Association[] get_associations(NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return new Association[0];
    }

    @Override
    public short get_max_vertices(NameValue[] properties)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return MAX_VERTICES;
    }

    // LibraryMgr
    @Override
    public String[] get_property_names() throws ProcessingFault, SystemFault {
        throw new NO_IMPLEMENT();
    }

    @Override
    public NameValue[] get_property_values(String[] desired_properties)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Library[] get_libraries() throws ProcessingFault, SystemFault {
        throw new NO_IMPLEMENT();
    }
}