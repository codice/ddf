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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class CswRecordMetacardType extends MetacardTypeImpl {

    private static final long serialVersionUID = 1L;

    /**
     * Used as prefix on attribute names that clash with basic Metacard attribute names e.g.,
     * "title" vs. "csw.title"
     */
    public static final String CSW_ATTRIBUTE_PREFIX = "csw.";

    public static final String CSW_NAMESPACE_URI = "http://www.opengis.net/cat/csw/2.0.2";

    public static final String CSW_METACARD_TYPE_NAME = "csw.record";

    public static final String CSW_IDENTIFIER = "identifier";

    /** Substitution name for "identifier" */
    public static final String CSW_BIBLIOGRAPHIC_CITATION = "bibliographicCitation";

    public static final String CSW_TITLE = CSW_ATTRIBUTE_PREFIX + "title";

    /** Substitution name for "title" */
    public static final String CSW_ALTERNATIVE = "alternative";

    public static final String CSW_TYPE = "type";

    public static final String CSW_SUBJECT = "subject";

    public static final String CSW_FORMAT = "format";

    /** Substitution name for "format" */
    public static final String CSW_EXTENT = "extent";

    /** Substitution name for "format" */
    public static final String CSW_MEDIUM = "medium";

    public static final String CSW_RELATION = "relation";

    /** Substitution name for "relation" */
    public static final String CSW_CONFORMS_TO = "conformsTo";

    /** Substitution name for "relation" */
    public static final String CSW_HAS_FORMAT = "hasFormat";

    /** Substitution name for "relation" */
    public static final String CSW_HAS_PART = "hasPart";

    /** Substitution name for "relation" */
    public static final String CSW_HAS_VERSION = "hasVersion";

    /** Substitution name for "relation" */
    public static final String CSW_IS_FORMAT_OF = "isFormatOf";

    /** Substitution name for "relation" */
    public static final String CSW_IS_PART_OF = "isPartOf";

    /** Substitution name for "relation" */
    public static final String CSW_IS_REFERENCED_BY = "isReferencedBy";

    /** Substitution name for "relation" */
    public static final String CSW_IS_REPLACED_BY = "isReplacedBy";

    /** Substitution name for "relation" */
    public static final String CSW_IS_REQUIRED_BY = "isRequiredBy";

    /** Substitution name for "relation" */
    public static final String CSW_IS_VERSION_OF = "isVersionOf";

    /** Substitution name for "relation" */
    public static final String CSW_REFERENCES = "references";

    /** Substitution name for "relation" */
    public static final String CSW_REPLACES = "replaces";

    /** Substitution name for "relation" */
    public static final String CSW_REQUIRES = "requires";

    public static final String CSW_DATE = "date";

    /** Substitution name for "date" */
    public static final String CSW_MODIFIED = CSW_ATTRIBUTE_PREFIX + "modified";

    /** Substitution name for "date" */
    public static final String CSW_CREATED = CSW_ATTRIBUTE_PREFIX + "created";

    /** Substitution name for "date" */
    public static final String CSW_DATE_ACCEPTED = "dateAccepted";

    /** Substitution name for "date" */
    public static final String CSW_DATE_COPYRIGHTED = "dateCopyrighted";

    /** Substitution name for "date" */
    public static final String CSW_DATE_SUBMITTED = "dateSubmitted";

    /** Substitution name for "date" */
    public static final String CSW_ISSUED = "issued";

    /** Substitution name for "date" */
    public static final String CSW_VALID = "valid";

    // Synonyms: abstract, tableOfContents
    public static final String CSW_DESCRIPTION = "description";

    /** Substitution name for "description" */
    public static final String CSW_ABSTRACT = "abstract";

    /** Substitution name for "description" */
    public static final String CSW_TABLE_OF_CONTENTS = "tableOfContents";

    // coverage: temporal and/or spatial info
    public static final String CSW_COVERAGE = "coverage";

    public static final String CSW_SPATIAL = "spatial";

    public static final String CSW_TEMPORAL = "temporal";

    public static final String OWS_BOUNDING_BOX = "BoundingBox";

    public static final String CSW_CREATOR = "creator";

    public static final String CSW_PUBLISHER = "publisher";

    public static final String CSW_CONTRIBUTOR = "contributor";

    public static final String CSW_LANGUAGE = "language";

    public static final String CSW_RIGHTS = "rights";

    /** Substitution name for "rights" */
    public static final String CSW_ACCESS_RIGHTS = "accessRights";

    /** Substitution name for "rights" */
    public static final String CSW_LICENSE = "license";

    public static final String CSW_SOURCE = "source";

    public static final String CSW_RESOURCE_URI = "resource-uri";

    
    public static final QName CSW_IDENTIFIER_QNAME;
    public static final QName CSW_BIBLIOGRAPHIC_CITATION_QNAME;
    public static final QName CSW_TITLE_QNAME;
    public static final QName CSW_ALTERNATIVE_QNAME;
    public static final QName CSW_TYPE_QNAME;
    public static final QName CSW_SUBJECT_QNAME;
    public static final QName CSW_FORMAT_QNAME;
    public static final QName CSW_EXTENT_QNAME;
    public static final QName CSW_MEDIUM_QNAME;
    public static final QName CSW_RELATION_QNAME;
    public static final QName CSW_CONFORMS_TO_QNAME;
    public static final QName CSW_HAS_FORMAT_QNAME;
    public static final QName CSW_HAS_PART_QNAME;
    public static final QName CSW_HAS_VERSION_QNAME;
    public static final QName CSW_IS_FORMAT_OF_QNAME;
    public static final QName CSW_IS_PART_OF_QNAME;
    public static final QName CSW_IS_REFERENCED_BY_QNAME;
    public static final QName CSW_IS_REPLACED_BY_QNAME;
    public static final QName CSW_IS_REQUIRED_BY_QNAME;
    public static final QName CSW_IS_VERSION_OF_QNAME;
    public static final QName CSW_REFERENCES_QNAME;
    public static final QName CSW_REPLACES_QNAME;
    public static final QName CSW_REQUIRES_QNAME;
    public static final QName CSW_DATE_QNAME;
    public static final QName CSW_MODIFIED_QNAME;
    public static final QName CSW_CREATED_QNAME;
    public static final QName CSW_DATE_ACCEPTED_QNAME;
    public static final QName CSW_DATE_COPYRIGHTED_QNAME;
    public static final QName CSW_DATE_SUBMITTED_QNAME;
    public static final QName CSW_ISSUED_QNAME;
    public static final QName CSW_VALID_QNAME;
    public static final QName CSW_DESCRIPTION_QNAME;
    public static final QName CSW_ABSTRACT_QNAME;
    public static final QName CSW_TABLE_OF_CONTENTS_QNAME;
    public static final QName CSW_COVERAGE_QNAME;
    public static final QName CSW_SPATIAL_QNAME;
    public static final QName CSW_TEMPORAL_QNAME;
    public static final QName OWS_BOUNDING_BOX_QNAME;
    public static final QName CSW_CREATOR_QNAME;
    public static final QName CSW_PUBLISHER_QNAME;
    public static final QName CSW_CONTRIBUTOR_QNAME;
    public static final QName CSW_LANGUAGE_QNAME;
    public static final QName CSW_RIGHTS_QNAME;
    public static final QName CSW_ACCESS_RIGHTS_QNAME;
    public static final QName CSW_LICENSE_QNAME;
    public static final QName CSW_SOURCE_QNAME;
    
    public static final List<QName> REQUIRED_FIELDS;
    public static final List<QName> BRIEF_CSW_RECORD_FIELDS;
    public static final List<QName> SUMMARY_CSW_RECORD_FIELDS;
    public static final List<QName> FULL_CSW_RECORD_FIELDS;

    /**
     * Indicates CSW Metacard Type's attribute is queryable, i.e., is indexed.
     */
    public static final boolean QUERYABLE = true;

    /**
     * Indicates CSW Metacard Type's attribute is not queryable, i.e., is not indexed.
     */
    public static final boolean NON_QUERYABLE = false;

    static {
        CSW_TITLE_QNAME = createDublinCoreQName(CswConstants.CSW_TITLE);
        CSW_MODIFIED_QNAME = createDublinCoreTermQName(CswConstants.CSW_MODIFIED);
        CSW_CREATED_QNAME = createDublinCoreTermQName(CswConstants.CSW_CREATED);

        CSW_IDENTIFIER_QNAME = createDublinCoreQName(CSW_IDENTIFIER);
        CSW_TYPE_QNAME = createDublinCoreQName(CSW_TYPE);
        CSW_SUBJECT_QNAME = createDublinCoreQName(CSW_SUBJECT);
        CSW_FORMAT_QNAME = createDublinCoreQName(CSW_FORMAT);
        CSW_RELATION_QNAME = createDublinCoreQName(CSW_RELATION);
        CSW_DATE_QNAME = createDublinCoreQName(CSW_DATE);
        CSW_DESCRIPTION_QNAME = createDublinCoreQName(CSW_DESCRIPTION);
        CSW_COVERAGE_QNAME = createDublinCoreQName(CSW_COVERAGE);
        CSW_CREATOR_QNAME = createDublinCoreQName(CSW_CREATOR);
        CSW_PUBLISHER_QNAME = createDublinCoreQName(CSW_PUBLISHER);
        CSW_CONTRIBUTOR_QNAME = createDublinCoreQName(CSW_CONTRIBUTOR);
        CSW_LANGUAGE_QNAME = createDublinCoreQName(CSW_LANGUAGE);
        CSW_RIGHTS_QNAME = createDublinCoreQName(CSW_RIGHTS);
        CSW_SOURCE_QNAME = createDublinCoreQName(CSW_SOURCE);

        CSW_ALTERNATIVE_QNAME = createDublinCoreTermQName(CSW_ALTERNATIVE);
        CSW_BIBLIOGRAPHIC_CITATION_QNAME = createDublinCoreTermQName(CSW_BIBLIOGRAPHIC_CITATION);
        CSW_EXTENT_QNAME = createDublinCoreTermQName(CSW_EXTENT);
        CSW_MEDIUM_QNAME = createDublinCoreTermQName(CSW_MEDIUM);
        CSW_CONFORMS_TO_QNAME = createDublinCoreTermQName(CSW_CONFORMS_TO);
        CSW_HAS_FORMAT_QNAME = createDublinCoreTermQName(CSW_HAS_FORMAT);
        CSW_HAS_PART_QNAME = createDublinCoreTermQName(CSW_HAS_PART);
        CSW_HAS_VERSION_QNAME = createDublinCoreTermQName(CSW_HAS_VERSION);
        CSW_IS_FORMAT_OF_QNAME = createDublinCoreTermQName(CSW_IS_FORMAT_OF);
        CSW_IS_PART_OF_QNAME = createDublinCoreTermQName(CSW_IS_PART_OF);
        CSW_IS_REFERENCED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REFERENCED_BY);
        CSW_IS_REPLACED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REPLACED_BY);
        CSW_IS_REQUIRED_BY_QNAME = createDublinCoreTermQName(CSW_IS_REQUIRED_BY);
        CSW_IS_VERSION_OF_QNAME = createDublinCoreTermQName(CSW_IS_VERSION_OF);
        CSW_REFERENCES_QNAME = createDublinCoreTermQName(CSW_REFERENCES);
        CSW_REPLACES_QNAME = createDublinCoreTermQName(CSW_REPLACES);
        CSW_REQUIRES_QNAME = createDublinCoreTermQName(CSW_REQUIRES);
        CSW_DATE_ACCEPTED_QNAME = createDublinCoreTermQName(CSW_DATE_ACCEPTED);
        CSW_DATE_COPYRIGHTED_QNAME = createDublinCoreTermQName(CSW_DATE_COPYRIGHTED);
        CSW_DATE_SUBMITTED_QNAME = createDublinCoreTermQName(CSW_DATE_SUBMITTED);
        CSW_ISSUED_QNAME = createDublinCoreTermQName(CSW_ISSUED);
        CSW_VALID_QNAME = createDublinCoreTermQName(CSW_VALID);
        CSW_ABSTRACT_QNAME = createDublinCoreTermQName(CSW_ABSTRACT);
        CSW_TABLE_OF_CONTENTS_QNAME = createDublinCoreTermQName(CSW_TABLE_OF_CONTENTS);
        CSW_SPATIAL_QNAME = createDublinCoreTermQName(CSW_SPATIAL);
        CSW_TEMPORAL_QNAME = createDublinCoreTermQName(CSW_TEMPORAL);
        CSW_ACCESS_RIGHTS_QNAME = createDublinCoreTermQName(CSW_ACCESS_RIGHTS);
        CSW_LICENSE_QNAME = createDublinCoreTermQName(CSW_LICENSE);
        
        OWS_BOUNDING_BOX_QNAME = new QName(CswConstants.OWS_NAMESPACE, 
                OWS_BOUNDING_BOX, CswConstants.OWS_NAMESPACE_PREFIX);

        REQUIRED_FIELDS = Arrays.asList(
                CSW_IDENTIFIER_QNAME,
                CSW_TITLE_QNAME
                );

        BRIEF_CSW_RECORD_FIELDS = Arrays.asList(
                CSW_IDENTIFIER_QNAME,
                CSW_TITLE_QNAME,
                CSW_TYPE_QNAME,
                OWS_BOUNDING_BOX_QNAME
                );

        SUMMARY_CSW_RECORD_FIELDS = Arrays.asList(
                CSW_IDENTIFIER_QNAME,
                CSW_TITLE_QNAME,
                CSW_TYPE_QNAME,
                CSW_SUBJECT_QNAME,
                CSW_FORMAT_QNAME,
                CSW_RELATION_QNAME,
                CSW_MODIFIED_QNAME,
                CSW_ABSTRACT_QNAME,
                CSW_SPATIAL_QNAME,
                OWS_BOUNDING_BOX_QNAME
                );

        FULL_CSW_RECORD_FIELDS = Arrays.asList(
                CSW_IDENTIFIER_QNAME,
                CSW_BIBLIOGRAPHIC_CITATION_QNAME,
                CSW_TITLE_QNAME,
                CSW_ALTERNATIVE_QNAME,
                CSW_TYPE_QNAME,
                CSW_SUBJECT_QNAME,
                CSW_FORMAT_QNAME,
                CSW_EXTENT_QNAME,
                CSW_MEDIUM_QNAME,
                CSW_RELATION_QNAME,
                CSW_CONFORMS_TO_QNAME,
                CSW_HAS_FORMAT_QNAME,
                CSW_HAS_PART_QNAME,
                CSW_HAS_VERSION_QNAME,
                CSW_IS_FORMAT_OF_QNAME,
                CSW_IS_PART_OF_QNAME,
                CSW_IS_REFERENCED_BY_QNAME,
                CSW_IS_REPLACED_BY_QNAME,
                CSW_IS_REQUIRED_BY_QNAME,
                CSW_IS_VERSION_OF_QNAME,
                CSW_REFERENCES_QNAME,
                CSW_REPLACES_QNAME,
                CSW_REQUIRES_QNAME,
                CSW_DATE_QNAME,
                CSW_MODIFIED_QNAME,
                CSW_CREATED_QNAME,
                CSW_DATE_ACCEPTED_QNAME,
                CSW_DATE_COPYRIGHTED_QNAME,
                CSW_DATE_SUBMITTED_QNAME,
                CSW_ISSUED_QNAME,
                CSW_VALID_QNAME,
                CSW_DESCRIPTION_QNAME,
                CSW_ABSTRACT_QNAME,
                CSW_TABLE_OF_CONTENTS_QNAME,
                CSW_COVERAGE_QNAME,
                CSW_SPATIAL_QNAME,
                CSW_TEMPORAL_QNAME,
                OWS_BOUNDING_BOX_QNAME,
                CSW_CREATOR_QNAME,
                CSW_PUBLISHER_QNAME,
                CSW_CONTRIBUTOR_QNAME,
                CSW_LANGUAGE_QNAME,
                CSW_RIGHTS_QNAME,
                CSW_ACCESS_RIGHTS_QNAME,
                CSW_LICENSE_QNAME,
                CSW_SOURCE_QNAME,
                OWS_BOUNDING_BOX_QNAME
                );

    }

    private static QName createDublinCoreQName(final String field) {
        return new QName(CswConstants.DUBLIN_CORE_SCHEMA, 
                field, CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX);
    }

    private static QName createDublinCoreTermQName(final String field) {
        return new QName(CswConstants.DUBLIN_CORE_TERMS_SCHEMA, 
                field, CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX);
    }
    
    public CswRecordMetacardType() {
        super(CSW_METACARD_TYPE_NAME, null);

        addDdfMetacardAttributes();
        addCswBriefRecordAttributes();
        addCswSummaryRecordSpecificAttributes();
        addCswFullRecordSpecificAttributes();
    }

    public CswRecordMetacardType(String sourceId) {
        super(sourceId + "." + CSW_METACARD_TYPE_NAME, null);

        addDdfMetacardAttributes();
        addCswBriefRecordAttributes();
        addCswSummaryRecordSpecificAttributes();
        addCswFullRecordSpecificAttributes();
    }

    private void addDdfMetacardAttributes() {
        // Single unique ID required by DDF Metacard
        descriptors.add(new AttributeDescriptorImpl(Metacard.ID, NON_QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Single/primary metacard title required by DDF Metacard
        descriptors.add(new AttributeDescriptorImpl(Metacard.TITLE, QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Original metadata
        descriptors.add(new AttributeDescriptorImpl(Metacard.METADATA, NON_QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Single/primary effective date
        descriptors.add(new AttributeDescriptorImpl(Metacard.EFFECTIVE,
                NON_QUERYABLE /* indexed */, false /* stored */, false /* tokenized */,
                false /* multivalued */, BasicTypes.DATE_TYPE));

        // Single/primary modified date
        descriptors.add(new AttributeDescriptorImpl(Metacard.MODIFIED, QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.DATE_TYPE));

        // Single/primary created date
        descriptors.add(new AttributeDescriptorImpl(Metacard.CREATED, QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.DATE_TYPE));

        // URI where Metacard's resource is located
        descriptors.add(new AttributeDescriptorImpl(Metacard.RESOURCE_URI,
                NON_QUERYABLE /* indexed */, false /* stored */, false /* tokenized */,
                false /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(Metacard.CONTENT_TYPE, QUERYABLE /* indexed */,
                false /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    private void addCswBriefRecordAttributes() {
        descriptors.add(new AttributeDescriptorImpl(CSW_IDENTIFIER, QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_BIBLIOGRAPHIC_CITATION,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_TITLE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_ALTERNATIVE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_TYPE, QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, false /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(OWS_BOUNDING_BOX, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.GEO_TYPE));
    }

    private void addCswSummaryRecordSpecificAttributes() {
        descriptors.add(new AttributeDescriptorImpl(CSW_SUBJECT, QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_FORMAT, QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));
        // Substitution name for "format"
        descriptors.add(new AttributeDescriptorImpl(CSW_EXTENT, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Substitution name for "format"
        descriptors.add(new AttributeDescriptorImpl(CSW_MEDIUM, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_RELATION, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        addRelationSubstitutionNames();

        descriptors.add(new AttributeDescriptorImpl(CSW_DATE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.DATE_TYPE));

        addDateSubstitutionNames();

        descriptors.add(new AttributeDescriptorImpl(CSW_DESCRIPTION, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Substitution name for "description"
        descriptors.add(new AttributeDescriptorImpl(CSW_ABSTRACT, QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_SPATIAL, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_COVERAGE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_TEMPORAL, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    private void addCswFullRecordSpecificAttributes() {
        descriptors.add(new AttributeDescriptorImpl(CSW_RIGHTS, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Substitution name for "rights"
        descriptors.add(new AttributeDescriptorImpl(CSW_ACCESS_RIGHTS, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        // Substitution name for "rights"
        descriptors.add(new AttributeDescriptorImpl(CSW_LICENSE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_LANGUAGE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_CREATOR, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_PUBLISHER, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_CONTRIBUTOR, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_SOURCE, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    private void addRelationSubstitutionNames() {
        descriptors.add(new AttributeDescriptorImpl(CSW_CONFORMS_TO, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_HAS_FORMAT, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_HAS_PART, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_HAS_VERSION, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_FORMAT_OF, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_PART_OF, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_REFERENCED_BY,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_REPLACED_BY,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_REQUIRED_BY,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_IS_VERSION_OF, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_REFERENCES, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_REPLACES, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_REQUIRES, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    private void addDateSubstitutionNames() {
        // Since CSW schema specifies date elements as SimpleLiteral (which is
        // anyText)
        // and not xsd:date, all date attributes are treated as a STRING_TYPE
        descriptors.add(new AttributeDescriptorImpl(CSW_MODIFIED, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_CREATED, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_DATE_ACCEPTED, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_DATE_COPYRIGHTED,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_DATE_SUBMITTED,
                NON_QUERYABLE /* indexed */, true /* stored */, false /* tokenized */,
                true /* multivalued */, BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_ISSUED, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));

        descriptors.add(new AttributeDescriptorImpl(CSW_VALID, NON_QUERYABLE /* indexed */,
                true /* stored */, false /* tokenized */, true /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    public String getNamespaceURI() {
        return CSW_NAMESPACE_URI;
    }
}
