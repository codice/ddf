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
package ddf.catalog.transformer.metacard.propertyjson

import ddf.catalog.data.BinaryContent
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.data.impl.MetacardTypeImpl
import ddf.catalog.data.impl.types.MediaAttributes
import ddf.catalog.transform.MetacardTransformer
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.text.SimpleDateFormat
import java.time.Instant

class PropertyJsonMetacardTransformerSpec extends Specification {

    MetacardTransformer jsonTransformer = new PropertyJsonMetacardTransformer();

    JsonSlurper slurper = new JsonSlurper();

    static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);

    def setup() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    def cleanup() {

    }

    def "test empty metacard"() {
        setup:
        MetacardImpl metacard = new MetacardImpl();

        when:
        BinaryContent result = jsonTransformer.transform(metacard, [:])
        def json = slurper.parse(result.byteArray)

        then:
        assert json.properties != null
        assert json.properties."metacard-type" == metacard.metacardType.name
    }

    def "test metacard"() {
        setup:
        MetacardImpl metacard = new MetacardImpl(
                new MetacardTypeImpl("test-type", [new MediaAttributes()]))
        def title = metacard.title = "Metacard title"
        def created = metacard.createdDate = Date.from(Instant.now())
        def geo = metacard.location = "POLYGON((40 40,34 44,20 45,13 31,38 32,40 40))"
        def metadata = metacard.metadata = '<?xml version="1.0" encoding="UTF-8"?><root><child>hello im childnode</child></root>'
        def bitspersample = 42
        metacard.setAttribute(MediaAttributes.BITS_PER_SAMPLE, bitspersample)
        def bitspersecond = 3.14159265358979323d
        metacard.setAttribute(MediaAttributes.BITS_PER_SECOND, bitspersecond)
        metacard.setAttribute(Metacard.MODIFIED, dateFormat.format(created)) //date field with string value


        when:
        BinaryContent result = jsonTransformer.transform(metacard, [:])
        def json = slurper.parse(result.byteArray)

        then:
        assert json.properties != null
        assert json.properties."metacard-type" == metacard.metacardType.name

        assert json.properties.location == geo

        assert json.properties.title == title
        assert json.properties.created == dateFormat.format(created)
        assert json.properties."${MediaAttributes.BITS_PER_SAMPLE}" == bitspersample
        assert json.properties."${MediaAttributes.BITS_PER_SECOND}" == bitspersecond
        assert json.properties.metadata == metadata
        assert json.properties.modifiedDate == null

    }

}