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
import ddf.catalog.data.impl.AttributeDescriptorImpl
import ddf.catalog.data.impl.BasicTypes
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference
import spock.lang.Specification

class PdfInputTransformerSpecTest extends Specification {
    Bundle bundleMock
    PdfInputTransformer pdfInputTransformer;

    def getEncryptedInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/encrypted.pdf")
    }

    def getSampleInputStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/sample.pdf")
    }

    def getSampleInputStreamWithCatalogMetadata = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/sample_with_catalog_metadata.pdf")
    }

    def getFontExceptionStream = {
        PdfInputTransformerSpecTest.class.getResourceAsStream("/font_exception.pdf")
    }

    void setup() {
        bundleMock = Mock(Bundle)

        pdfInputTransformer = new PdfInputTransformer() {
            @Override
            def Bundle getBundle() {
                return bundleMock
            }
        };
    }

    void verifySamplePdfMetacard(Metacard metacard) {
        verifyThumbnailSize(metacard.thumbnail)
        assert metacard.contentTypeName == 'pdf'
        assert metacard.title == 'Microsoft Word - Document1'
        assert metacard.createdDate.getTime() == 1456150156000
        assert metacard.modifiedDate.getTime() == 1456150156000

        assert metacard.metadata.contains('<title>Microsoft Word - Document1</title>')
        assert metacard.metadata.contains('<producer>Mac OS X 10.11.3 Quartz PDFContext</producer>')
        assert metacard.metadata.contains('<creationDate>2016-02-22T14:09:16+00:00</creationDate>')
        assert metacard.metadata.contains('<modificationDate>2016-02-22T14:09:16+00:00</modificationDate>')
        assert metacard.metadata.contains('<pageCount>1</pageCount>')
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

    def "Transform with catalog metadata"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStreamWithCatalogMetadata())

        then:
        verifyThumbnailSize(metacard.thumbnail)
        metacard.contentTypeName == 'pdf'
        metacard.title == 'Untitled'
        metacard.createdDate.getTime() == 1253116122000
        metacard.modifiedDate.getTime() == 1253116509000

        metacard.metadata.contains('<xap:MetadataDate>2009-09-16T10:55:09-05:00</xap:MetadataDate>')
        metacard.metadata.contains('<title>Untitled</title>')
        metacard.metadata.contains('<creator>Acrobat Editor 8.0</creator>')
        metacard.metadata.contains('<producer>Adobe Acrobat 8.1.6</producer>')
        metacard.metadata.contains('<creationDate>2009-09-16T10:48:42-05:00</creationDate>')
        metacard.metadata.contains('<modificationDate>2009-09-16T10:55:09-05:00</modificationDate>')
        metacard.metadata.contains('<pageCount>12</pageCount>')
    }

    def "Attempt transform encrypted pdf"() {
        when:
        Metacard metacard = pdfInputTransformer.transform(getEncryptedInputStream())

        then:
        metacard.thumbnail == null
        metacard.title == null
        metacard.metadata == null
        metacard.contentTypeName == 'pdf'
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

    def 'Transform with content extractor'() {
        setup:
        def bundleCtx = Mock(BundleContext)
        def cme = Mock(ContentMetadataExtractor)
        def serviceRef = Mock(ServiceReference)

        when:
        pdfInputTransformer.addContentMetadataExtractors(serviceRef)
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStreamWithCatalogMetadata())

        then:
        1 * bundleMock.getBundleContext() >> bundleCtx
        1 * bundleCtx.getService(_) >> cme
        cme.metacardAttributes >>
                [new AttributeDescriptorImpl('attr1', false, false, false, false, BasicTypes.OBJECT_TYPE),
                 new AttributeDescriptorImpl('attr2', false, false, false, false, BasicTypes.OBJECT_TYPE)]

        metacard.metacardType.name != BasicTypes.BASIC_METACARD.name
        def attrNames = metacard.metacardType.attributeDescriptors*.name
        attrNames.containsAll(['attr1', 'attr2'])
    }

    def 'Fail transform with content extractor on encrypted pdf'() {
        setup:
        def bundleCtx = Mock(BundleContext)
        def cme = Mock(ContentMetadataExtractor)
        def serviceRef = Mock(ServiceReference)

        when:
        pdfInputTransformer.addContentMetadataExtractors(serviceRef)
        Metacard metacard = pdfInputTransformer.transform(getEncryptedInputStream())

        then:
        1 * bundleMock.getBundleContext() >> bundleCtx
        1 * bundleCtx.getService(_) >> cme
        cme.metacardAttributes >>
                [new AttributeDescriptorImpl('attr1', false, false, false, false, BasicTypes.OBJECT_TYPE),
                 new AttributeDescriptorImpl('attr2', false, false, false, false, BasicTypes.OBJECT_TYPE)]

        metacard.metacardType == BasicTypes.BASIC_METACARD
        def attrNames = metacard.metacardType.attributeDescriptors*.name
        !attrNames.containsAll(['attr1', 'attr2'])
    }

    def 'Ensure content extractors are removed'() {
        setup:
        def bundleCtx = Mock(BundleContext)
        def cme = Mock(ContentMetadataExtractor)
        def serviceRef = Mock(ServiceReference)

        when:
        pdfInputTransformer.addContentMetadataExtractors(serviceRef)
        pdfInputTransformer.removeContentMetadataExtractor(serviceRef)
        Metacard metacard = pdfInputTransformer.transform(getSampleInputStreamWithCatalogMetadata())

        then:
        1 * bundleMock.getBundleContext() >> bundleCtx
        1 * bundleCtx.getService(_) >> cme
        cme.metacardAttributes >>
                [new AttributeDescriptorImpl('attr1', false, false, false, false, BasicTypes.OBJECT_TYPE),
                 new AttributeDescriptorImpl('attr2', false, false, false, false, BasicTypes.OBJECT_TYPE)]

        def attrNames = metacard.metacardType.attributeDescriptors*.name
        !attrNames.containsAll(['attr1', 'attr2'])
    }
}
