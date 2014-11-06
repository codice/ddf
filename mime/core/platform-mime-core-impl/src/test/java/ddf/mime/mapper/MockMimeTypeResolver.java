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
package ddf.mime.mapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ddf.mime.MimeTypeResolver;

// import org.apache.tika.mime.MimeTypeException;

public class MockMimeTypeResolver implements MimeTypeResolver {
    private HashMap<String, String> customFileExtensionsToMimeTypesMap;

    private HashMap<String, List<String>> customMimeTypesToFileExtensionsMap;

    private String name;
    
    private String schema;

    private int priority;

    public MockMimeTypeResolver(String name) {
        this(name, 10);
    }
    
    public MockMimeTypeResolver(String name, int priority) {
        this.name = name;
        this.priority = priority;
        
        String[] customMimeTypes = new String[] {
            "nitf=image/nitf",
            "ntf=image/nitf"
        };
        
        setCustomMimeTypes(customMimeTypes);
    }

    public MockMimeTypeResolver(String name, int priority, String[] customMimeTypes, String schema) {
        this.name = name;
        this.priority = priority;
        this.schema = schema;
        
        setCustomMimeTypes(customMimeTypes);
    }

    public void setCustomMimeTypes(String[] customMimeTypes) {
//        this.customMimeTypes = customMimeTypes.clone();
        this.customFileExtensionsToMimeTypesMap = new HashMap<String, String>();
        this.customMimeTypesToFileExtensionsMap = new HashMap<String, List<String>>();

        for (String mimeTypeMapping : customMimeTypes) {

            // mimeTypeMapping is of the form <file extension>=<mime type>
            // Examples:
            // nitf=image/nitf

            String fileExtension = StringUtils.substringBefore(mimeTypeMapping, "=");
            String mimeType = StringUtils.substringAfter(mimeTypeMapping, "=");

            customFileExtensionsToMimeTypesMap.put(fileExtension, mimeType);
            List<String> fileExtensions = (List<String>) customMimeTypesToFileExtensionsMap
                    .get(mimeType);
            if (fileExtensions == null) {
                fileExtensions = new ArrayList<String>();
            }
            fileExtensions.add(fileExtension);
            customMimeTypesToFileExtensionsMap.put(mimeType, fileExtensions);
        }
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getPriority() {
        return priority;
    }
    
    
    @Override
    public boolean hasSchema() {
        return StringUtils.isNotBlank(this.schema);
    }
    
    @Override
    public String getSchema() {
        return schema;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getFileExtensionForMimeType(String contentType)
    {
        String fileExtension = null;
        List<String> fileExtensions = customMimeTypesToFileExtensionsMap.get(contentType);
        if (fileExtensions != null && fileExtensions.size() > 0) {
            fileExtension = fileExtensions.get(0);

            if (StringUtils.isNotEmpty(fileExtension)) {
                return "." + fileExtension;
            }
        }
        return fileExtension;
    }

    @Override
    public String getMimeTypeForFileExtension(String fileExtension)
    {
        String mimeType = customFileExtensionsToMimeTypesMap.get(fileExtension);

        return mimeType;
    }

}
