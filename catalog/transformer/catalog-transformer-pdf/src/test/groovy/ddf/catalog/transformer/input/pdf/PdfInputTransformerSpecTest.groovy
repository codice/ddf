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

import ddf.catalog.data.Metacard
import spock.lang.Specification


class PdfInputTransformerSpecTest extends Specification {
    PdfInputTransformer pdfInputTransformer;

    def getEncryptedInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/encrypted.pdf")
    }

    def getSampleInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/sample.pdf")
    }

    void setup() {
        pdfInputTransformer = new PdfInputTransformer();
    }

    void cleanup() {
        // Function for post test cleanup
    }

    def "Transform"() {
        setup:
        def id = "123ab-4a23-1bde2f-82fdc01"

        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStream(), id)

        then:
        metacard.thumbnail.length < 128 * 1024
        metacard.contentTypeName == 'pdf'
        metacard.id == id
        metacard.title == 'Microsoft Word - Document1'
    }

    def "Transform without id"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStream())

        then:
        metacard.thumbnail.length < 128 * 1024
        metacard.contentTypeName == 'pdf'
        metacard.title == 'Microsoft Word - Document1'
    }

    def "Attempt transform encrypted pdf"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getEncryptedInputStream())

        then:
        metacard.thumbnail == null
        metacard.title == null
        metacard.contentTypeName == 'pdf'
    }

    def "Generate thumbnail only"() {
        when:
        byte[] thumbnail = pdfInputTransformer.generatePdfThumbnail(getSampleInputStream())

        then:
        thumbnail != null
        thumbnail.length > 0
    }

    def "Generate thumbnail only encrypted"() {
        when:
        byte[] thumbnail = pdfInputTransformer.generatePdfThumbnail(getEncryptedInputStream())

        then:
        thrown(IOException)
    }
}
