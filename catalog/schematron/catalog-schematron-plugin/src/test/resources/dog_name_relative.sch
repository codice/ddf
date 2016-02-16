<?xml version="1.0" encoding="UTF-8"?>
<!--
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

 -->

<schema xmlns="http://purl.oclc.org/dsdl/schematron">
    <sch:pattern id="Dog Name" xmlns:sch="http://purl.oclc.org/dsdl/schematron">
        <sch:let name="valid_names" value="document('valid_names.xml')"/>
        <sch:rule context="//Dog/name">
            <sch:assert
                    test="$valid_names//name[normalize-space(.) = normalize-space(current()/.)]"
                    flag="warning">
                Not a valid name for a dog
            </sch:assert>
        </sch:rule>
    </sch:pattern>
</schema>