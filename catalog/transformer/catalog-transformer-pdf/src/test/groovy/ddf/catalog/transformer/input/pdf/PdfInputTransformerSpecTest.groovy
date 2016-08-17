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
package ddf.catalog.transformer.input.pdf

import ddf.catalog.content.operation.ContentMetadataExtractor
import ddf.catalog.data.Metacard
import ddf.catalog.data.MetacardType
import ddf.catalog.data.impl.AttributeDescriptorImpl
import ddf.catalog.data.impl.BasicTypes

import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

class PdfInputTransformerSpecTest extends Specification {

    PdfInputTransformer pdfInputTransformer;
    MetacardType metacardType;

    def getEncryptedInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/encrypted.pdf")
    }

    def getSampleInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/sample.pdf")
    }

    def getFontExceptionStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/font_exception.pdf")
    }

    void setup() {
        metacardType = new MetacardTypeImpl("pdf", [
            new AssociationsAttributes(),
            new ContactAttributes(),
            new MediaAttributes(),
            new DateTimeAttributes(),
            new LocationAttributes(),
            new ValidationAttributes()
        ])

        pdfInputTransformer = new PdfInputTransformer(metacardType, false);
    }

    void verifySamplePdfMetacard(Metacard metacard) {
        verifyThumbnailSize(metacard.thumbnail)
        assert metacard.contentTypeName == 'application/pdf'
        assert metacard.createdDate.getTime() == 1456150156000
        assert metacard.modifiedDate.getTime() == 1456150156000
    }

    void verifyThumbnailSize(byte[] thumbnail) {
        assert thumbnail.length > 0
        assert thumbnail.length < 128 * 1024
    }

    def "Generate Thumbail with Font Exception"() {

        /*
          The only thing this test does is reproduce a problem that can only be detected by visual
          inspection of the output log. It may be possible to test the thumbnail to see if every pixel is
          white.
         */

        when:
        Metacard metacard = pdfInputTransformer.transform(getFontExceptionStream())

        then:
        metacard != null
    }

    def "Transform"() {
        setup:
        def id = "123ab-4a23-1bde2f-82fdc01"

        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStream(), id)

        then:
        metacard.id == id
        verifySamplePdfMetacard(metacard)
    }

    def "Transform without id"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStream())

        then:
        verifySamplePdfMetacard(metacard)
    }

    def "Attempt transform encrypted pdf"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getEncryptedInputStream())

        then:
        metacard.thumbnail == null
        metacard.title == null
        metacard.metadata == null
        metacard.contentTypeName == 'application/pdf'
    }

    def "Generate thumbnail only"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStream())

        then:
        metacard != null
        metacard.getThumbnail() != null
        verifyThumbnailSize(metacard.getThumbnail())
    }

    def "Generate thumbnail only encrypted"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getEncryptedInputStream())

        then:
        metacard.getThumbnail() == null
    }
    
}
