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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.digester.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.data.dynamic.api.MetacardFactory;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

/**
 * Reads metacard type definitions from an XML file. Methods exist to take various types of
 * input (File, stream, etc.) and parse them in order to create a dynamic class definition
 * of the specified metacard class type.
 *
 * The XML input structure is as follows:
 *
 * <metacard>                               <!-- example values -->
 *   <name>...</name>                       <!-- nitf -->
 *   <type>...</type>                       <!-- ddf --> <!-- addes in attributes from the ddf type -->
 *   <attributes>
 *     <attribute multi-valued="true">
 *       <name>...</name>                   <!-- created -->
 *       <type>...</type>                   <!-- Date, String, Long, etc. -->
 *       <indexed>true|false</indexed>
 *       <stored>true|false</stored>
 *       <tokenized>true|false</tokenized>
 *       <multi-valued>true|false</multi-valued>
 *     </attribute>
 *   <attributes>
 * </metacard>
 *
 */
public class MetacardReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardReader.class);
    private static final DynaProperty[] EMPTY_DYNAPROPERTY_ARRAY = new DynaProperty[0];
    private MetacardFactory metacardFactory;

    /**
     * Reads and parses the input stream building the definition for a DynamicMetacardImpl type (a LazyDynaClass)
     * as it reads. It is expected that the stream is closed by the caller.
     * @param is the input stream containing the xml definition of a metacard class
     * @return an instance of a LazyDynaClass representing the provided metacard attributes
     */
    public boolean parseMetacardDefinition(InputStream is) {
        MetacardClassBuilder dmcb = null;
        boolean status = false;
        try {
            Digester digester = new Digester();
            digester.setValidating(false);

            digester.addObjectCreate("metacard", MetacardClassBuilder.class);
            digester.addBeanPropertySetter("metacard/name", "name");
            digester.addCallMethod("metacard/type", "addType", 1);
            digester.addCallParam("metacard/type", 0);
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

        if (dmcb != null) {
            List<MetacardPropertyDescriptor> descriptors = new ArrayList<>();
            // loop through all the base types and add all their attributes first
            List<String> types = dmcb.getTypes();
            if (types != null) {
                for (String name : types) {
                    MetacardPropertyDescriptor[] props = metacardFactory.getMetacardPropertyDescriptors(name);
                    if (props != null) {
                        descriptors.addAll(
                                Arrays.asList(metacardFactory.getMetacardPropertyDescriptors(name)));
                    }
                }
            }

            // now add in the descriptors from this definition file
            descriptors.addAll(dmcb.getDescriptors());

            metacardFactory.registerDynamicMetacardType(dmcb.getName(), descriptors);
            status = true;
        }

        return status;
    }

    /**
     * Reads and parses the given {@link File} building the definition for a DynamicMetacardImpl
     * type (a LazyDynaClass) as it reads.
     * @param metacardDefinitionFile the {@link File} object containing the xml definition
     *                               of a metacard class
     * @return an instance of a LazyDynaClass representing the provided metacard attributes
     */
    public boolean registerMetacard(File metacardDefinitionFile) {
        boolean success = false;
        if (metacardDefinitionFile != null) {
            try (BufferedInputStream inputStream =
                    new BufferedInputStream(new FileInputStream(metacardDefinitionFile))) {
                success = parseMetacardDefinition(inputStream);
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
//
//    public void registerMetacardDefinition(InputStream is) {
//        try {
//            LazyDynaClass dclass = parseMetacardDefinition(is);
//
//            if ((dclass != null) && (metacardFactory != null)) {
//                LOGGER.debug("Registering metacard - name: {}", dclass.getName());
//                metacardFactory.addDynaClass(dclass);
//            } else {
//                LOGGER.warn("Unable to register new metacard type.");
//            }
//        } catch (Exception e) {
//            LOGGER.warn("Unexpected error parsing metadata definition.", e);
//        }
//    }

    /**
     * Allows the metacard factory object to be injected into this reader.
     * @param mcf the {@link MetacardFactory} implementation for registering metacards
     */
    public void setMetacardFactory(MetacardFactory mcf) {
        this.metacardFactory = mcf;
    }
}