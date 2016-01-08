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
package ddf.catalog.data.dynamic.registry;

import java.io.BufferedInputStream;
//import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.beanutils.LazyDynaClass;
import org.apache.commons.digester.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.dynamic.api.MetacardFactory;

/**
 * Reads metacard type definitions from an XML file. Methods exist to take various types of
 * input (File, stream, etc.) and parse them in order to create a dynamic class definition
 * of the specified metacard class type.
 *
 * The XML input structure is as follows:
 *
 * <metacard>
 *   <name>...</name>
 *   <attributes>
 *     <attribute multi-valued="true">
 *       <name>createdDate</name>
 *       <type>Date</type>
 *       <indexed>true</indexed>
 *       <stored>true</stored>
 *       <tokenized>false</tokenized>
 *       <multi-valued>true</multi-valued>
 *     </attribute>
 *   <attributes>
 * </metacard>
 *
 */
public class MetacardReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardReader.class);

    private MetacardFactory metacardFactory;

    public LazyDynaClass parseMetacardDefinition(InputStream is) {
        MetacardClassBuilder dmcb = null;
        LazyDynaClass dynaClass = null;
        try {
            Digester digester = new Digester();
            digester.setValidating(false);

            digester.addObjectCreate("metacard", MetacardClassBuilder.class);
            digester.addBeanPropertySetter("metacard/name", "name");
            digester.addObjectCreate("metacard/attributes/attribute", MetacardAttribute.class);
            digester.addBeanPropertySetter("metacard/attributes/attribute/name", "name");
            digester.addBeanPropertySetter("metacard/attributes/attribute/type", "type");
            digester.addBeanPropertySetter("metacard/attributes/attribute/indexed", "indexed");
            digester.addBeanPropertySetter("metacard/attributes/attribute/stored", "stored");
            digester.addBeanPropertySetter("metacard/attributes/attribute/tokenized", "tokenized");
            digester.addBeanPropertySetter("metacard/attributes/attribute/multi-valued",
                    "multiValued");
            digester.addSetNext("metacard/attributes/attribute", "addAttribute");

            dmcb = (MetacardClassBuilder) digester.parse(is);
        } catch (IOException e) {
            LOGGER.warn("Error reading input stream - no metacard definition generated.", e);
        } catch (SAXException e) {
            LOGGER.warn("Error parsing metacard definition - no metacard definition generated.", e);
        }
        return dmcb.getDynamicMetacardClass();
    }

    public boolean registerMetacard(File metacardDefinitionFile) {
        boolean success = false;
        if (metacardDefinitionFile != null) {
            try (BufferedInputStream inputStream =
                    new BufferedInputStream(new FileInputStream(metacardDefinitionFile))) {
                LazyDynaClass dynaClass = parseMetacardDefinition(inputStream);
                metacardFactory.addDynaClass(dynaClass);
                success = true;
            } catch (IOException e) {
                LOGGER.warn("Unable to read file {}", metacardDefinitionFile.getName(), e);
            }
        }
        return success;
    }

//    public void registerMetacardDefinition(File f) {
//        if (f == null) {
//            LOGGER.debug("Unable to read definition from a null file.");
//            return;
//        }
//
//        LOGGER.debug("Reading metacard definition from file {}", f.getName());
//        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(f))) {
//            registerMetacardDefinition(is);
//        } catch (IOException e) {
//            LOGGER.warn("Error opening file {}", f.getName());
//        }
//    }
//
//    public void registerMetacardDefinition(String s) {
//        if (s == null) {
//            LOGGER.warn("Unable to read metacard definition from null string.");
//            return;
//        }
//
//        InputStream is = new ByteArrayInputStream(s.getBytes());
//        registerMetacardDefinition(is);
//    }

    public void registerMetacardDefinition(InputStream is) {
        try {
            LazyDynaClass dclass = parseMetacardDefinition(is);

            if ((dclass != null) && (metacardFactory != null)) {
                LOGGER.debug("Registering metacard - name: {}", dclass.getName());
                metacardFactory.addDynaClass(dclass);
            } else {
                LOGGER.warn("Unable to register new metacard type.");
            }
        } catch (Exception e) {
            LOGGER.warn("Unexpected error parsing metadata definition.", e);
        }
    }

    public void setMetacardFactory(MetacardFactory mcf) {
        this.metacardFactory = mcf;
    }
}