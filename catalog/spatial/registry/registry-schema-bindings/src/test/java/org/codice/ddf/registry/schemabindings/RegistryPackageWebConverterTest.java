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
package org.codice.ddf.registry.schemabindings;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.GML_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.WRS_FACTORY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.schemabindings.converter.type.RegistryPackageTypeConverter;
import org.codice.ddf.registry.schemabindings.converter.web.RegistryPackageWebConverter;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.junit.Before;
import org.junit.Test;

import net.opengis.cat.wrs.v_1_0_2.AnyValueType;
import net.opengis.cat.wrs.v_1_0_2.ValueListType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.EnvelopeType;
import net.opengis.gml.v_3_1_1.PointType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ClassificationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.EmailAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.OrganizationType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonNameType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PersonType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.PostalAddressType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectListType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceBindingType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ServiceType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SpecificationLinkType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.TelephoneNumberType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class RegistryPackageWebConverterTest {
    private Parser parser;

    private ParserConfigurator configurator;

    private InternationalStringTypeHelper istHelper = new InternationalStringTypeHelper();

    private SlotTypeHelper stHelper = new SlotTypeHelper();

    @Before
    public void setUp() {
        parser = new XmlParser();

        configurator = parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                EbrimConstants.OGC_FACTORY.getClass()
                        .getPackage()
                        .getName(),
                EbrimConstants.GML_FACTORY.getClass()
                        .getPackage()
                        .getName()),
                this.getClass()
                        .getClassLoader());
    }

    @Test
    public void testRoundTrip() throws Exception {
        RegistryPackageWebConverter rpwConverter = new RegistryPackageWebConverter();

        RegistryPackageType testRegistryPackage = getTestRegistryPackage();
        Map<String, Object> testRegistryPackageMap = rpwConverter.convert(testRegistryPackage);

        RegistryPackageTypeConverter rptConverter = new RegistryPackageTypeConverter();

        Optional<RegistryPackageType> optionalRegistryPackage = rptConverter.convert(
                testRegistryPackageMap);
        RegistryPackageType convertedTestRegistryPackage = optionalRegistryPackage.get();

        assertThat(testRegistryPackage.getObjectType(),
                is(equalTo(convertedTestRegistryPackage.getObjectType())));
        assertThat(testRegistryPackage.getId(), is(equalTo(convertedTestRegistryPackage.getId())));
        assertThat(testRegistryPackage.getHome(),
                is(equalTo(convertedTestRegistryPackage.getHome())));
        assertThat(testRegistryPackage.getExternalIdentifier(),
                is(equalTo(convertedTestRegistryPackage.getExternalIdentifier())));
        assertThat((testRegistryPackage).getRegistryObjectList()
                        .getIdentifiable()
                        .size(),
                is(equalTo((convertedTestRegistryPackage).getRegistryObjectList()
                        .getIdentifiable()
                        .size())));

        assertThat(testRegistryPackage, is(equalTo(convertedTestRegistryPackage)));
    }

    @Test
    public void testEmptyRegistryPackage() throws Exception {
        Map<String, Object> emptyRegistryMap = new HashMap<>();

        RegistryPackageTypeConverter rptConverter = new RegistryPackageTypeConverter();
        Optional<RegistryPackageType> optionalRegistryPackage = rptConverter.convert(
                emptyRegistryMap);
        RegistryPackageType registryPackage = optionalRegistryPackage.orElse(null);

        assertThat(registryPackage, nullValue());
    }

    private RegistryPackageType getTestRegistryPackage() {
        RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();
        registryPackage.setId("urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        registryPackage.setHome("https://somehost:someport");
        registryPackage.setObjectType("urn:registry:federation:node");

        registryPackage.getExternalIdentifier()
                .add(getFirstExternalIdentifier());
        registryPackage.getExternalIdentifier()
                .add(getSecondExternalIdentifier());

        RegistryObjectListType registryObjectList = RIM_FACTORY.createRegistryObjectListType();
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createExtrinsicObject(getFirstExtrinsicObject()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createExtrinsicObject(getSecondExtrinsicObject()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createExtrinsicObject(getThirdExtrinsicObject()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createExtrinsicObject(getFourthExtrinsicObject()));

        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createService(getService()));

        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createOrganization(getFirstOrganization()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createOrganization(getSecondOrganization()));

        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createPerson(getFirstPerson()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createPerson(getSecondPerson()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createPerson(getThirdPerson()));

        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getFirstAssociation()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getSecondAssociation()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getThirdAssociation()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getFourthAssociation()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getFifthAssociation()));
        registryObjectList.getIdentifiable()
                .add(RIM_FACTORY.createAssociation(getSixthAssociation()));

        registryPackage.setRegistryObjectList(registryObjectList);

        return registryPackage;
    }

    private ExternalIdentifierType getFirstExternalIdentifier() {
        ExternalIdentifierType externalIdentifier = RIM_FACTORY.createExternalIdentifierType();
        externalIdentifier.setId(RegistryConstants.REGISTRY_MCARD_ID_LOCAL);
        externalIdentifier.setRegistryObject("urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        externalIdentifier.setIdentificationScheme("MetacardId");
        externalIdentifier.setValue("someUUID");

        return externalIdentifier;
    }

    private ExternalIdentifierType getSecondExternalIdentifier() {
        ExternalIdentifierType externalIdentifier = RIM_FACTORY.createExternalIdentifierType();
        externalIdentifier.setId(RegistryConstants.REGISTRY_MCARD_ID_ORIGIN);
        externalIdentifier.setRegistryObject("urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        externalIdentifier.setIdentificationScheme("MetacardId");
        externalIdentifier.setValue("someUUID");

        return externalIdentifier;
    }

    private ExtrinsicObjectType getFirstExtrinsicObject() {
        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        // set default values
        extrinsicObject.setMimeType(extrinsicObject.getMimeType());
        extrinsicObject.setIsOpaque(extrinsicObject.isIsOpaque());
        extrinsicObject.setId("urn:registry:node");
        extrinsicObject.setObjectType("urn:registry:federation:node");
        extrinsicObject.getSlot()
                .add(stHelper.create("liveDate", "2015-11-01T06:15:30-07:00", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("dataStartDate", "2015-11-01T13:15:30Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("dataEndDate", "2015-12-01T23:01:40Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("lastUpdated", "2016-01-26T17:16:34.996Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("links", "https://some/link/to/my/repo", "xs:string"));

        SlotType1 locationSlot = stHelper.create("location",
                (String) null,
                "urn:ogc:def:dataType:ISO-19107:2003:GM_Point");

        PointType point = GML_FACTORY.createPointType();
        point.setSrsDimension(BigInteger.valueOf(2));
        point.setSrsName("urn:ogc:def:crs:EPSG::4326");
        DirectPositionType directPosition = GML_FACTORY.createDirectPositionType();
        directPosition.getValue()
                .add(112.267472);
        directPosition.getValue()
                .add(33.467944);
        point.setPos(directPosition);
        ValueListType valueList = WRS_FACTORY.createValueListType();
        AnyValueType anyValue = WRS_FACTORY.createAnyValueType();
        anyValue.getContent()
                .add(GML_FACTORY.createPoint(point));
        valueList.getAnyValue()
                .add(anyValue);
        locationSlot.setValueList(RIM_FACTORY.createValueList(valueList));

        extrinsicObject.getSlot()
                .add(locationSlot);

        SlotType1 boundsSlot = stHelper.create("bounds",
                (String) null,
                "urn:ogc:def:dataType:ISO-19107:2003:GM_Envelope");

        EnvelopeType bounds = GML_FACTORY.createEnvelopeType();

        bounds.setSrsName("urn:ogc:def:crs:EPSG::4326");
        directPosition = GML_FACTORY.createDirectPositionType();
        directPosition.getValue()
                .add(112.267472);
        directPosition.getValue()
                .add(33.467944);
        bounds.setUpperCorner(directPosition);
        directPosition = GML_FACTORY.createDirectPositionType();
        directPosition.getValue()
                .add(110.267472);
        directPosition.getValue()
                .add(30.467944);
        bounds.setLowerCorner(directPosition);
        valueList = WRS_FACTORY.createValueListType();
        anyValue = WRS_FACTORY.createAnyValueType();
        anyValue.getContent()
                .add(GML_FACTORY.createEnvelope(bounds));
        valueList.getAnyValue()
                .add(anyValue);
        boundsSlot.setValueList(RIM_FACTORY.createValueList(valueList));

        extrinsicObject.getSlot()
                .add(boundsSlot);

        extrinsicObject.getSlot()
                .add(stHelper.create("region",
                        "USA",
                        "urn:ogc:def:ebRIM-ClassificationScheme:UNSD:GlobalRegions"));

        List<String> values = new ArrayList<>();
        values.add("youtube");
        values.add("myCamera");
        extrinsicObject.getSlot()
                .add(stHelper.create("inputDataSources", values, "xs:string"));

        values = new ArrayList<>();
        values.add("video");
        values.add("sensor");
        extrinsicObject.getSlot()
                .add(stHelper.create("dataTypes", values, "xs:string"));
        extrinsicObject.getSlot()
                .add(stHelper.create("securityLevel", "role=guest", "xs:string"));

        extrinsicObject.setName(istHelper.create("Node Name"));
        extrinsicObject.setDescription(istHelper.create(
                "A little something describing this node in less than 1024 characters"));
        extrinsicObject.setVersionInfo(getVersionInfo("2.9.x"));

        ClassificationType classification = RIM_FACTORY.createClassificationType();
        classification.setId("urn:classification:id0");
        classification.setClassifiedObject("classifiedObjectId");
        extrinsicObject.getClassification()
                .add(classification);

        return extrinsicObject;
    }

    private ExtrinsicObjectType getSecondExtrinsicObject() {
        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        // set default values
        extrinsicObject.setMimeType(extrinsicObject.getMimeType());
        extrinsicObject.setIsOpaque(extrinsicObject.isIsOpaque());

        extrinsicObject.setId("urn:content:collection:id0");
        extrinsicObject.setObjectType("urn:registry:content:collection");
        extrinsicObject.getSlot()
                .add(stHelper.create("types", "sensor", "xs:string"));
        List<String> values = new ArrayList<>();
        values.add("application/pdf");
        values.add("application/msword");
        extrinsicObject.getSlot()
                .add(stHelper.create("mimeTypes", values, "xs:string"));
        extrinsicObject.getSlot()
                .add(stHelper.create("recordCount", "1234", "xs:long"));
        extrinsicObject.getSlot()
                .add(stHelper.create("startDate", "2015-11-01T13:15:30Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("endDate", "2015-12-01T23:01:40Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("lastUpdated", "2016-01-26T17:16:34.996Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("region",
                        "Arizona",
                        "urn:ogc:def:ebRIM-ClassificationScheme:UNSD:GlobalRegions"));
        extrinsicObject.setName(istHelper.create("Collection Name"));
        extrinsicObject.setDescription(istHelper.create(
                "A little something describing this collection in less than 1024 characters"));

        return extrinsicObject;
    }

    private ExtrinsicObjectType getThirdExtrinsicObject() {
        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        // set default values
        extrinsicObject.setMimeType(extrinsicObject.getMimeType());
        extrinsicObject.setIsOpaque(extrinsicObject.isIsOpaque());

        extrinsicObject.setId("urn:content:collection:id1");
        extrinsicObject.setObjectType("urn:registry:content:collection");

        extrinsicObject.getSlot()
                .add(stHelper.create("types", "video", "xs:string"));
        extrinsicObject.getSlot()
                .add(stHelper.create("mimeTypes", "video/mpeg4-generic", "xs:string"));
        extrinsicObject.getSlot()
                .add(stHelper.create("recordCount", "1234", "xs:long"));
        extrinsicObject.getSlot()
                .add(stHelper.create("startDate", "2015-11-01T13:15:30Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("endDate", "2015-12-01T22:01:40Z", "xs:dateTime"));
        extrinsicObject.getSlot()
                .add(stHelper.create("lastUpdated", "2016-01-26T17:16:34.996Z", "xs:dateTime"));

        extrinsicObject.setName(istHelper.create("Collection Name2"));
        extrinsicObject.setDescription(istHelper.create(
                "A little something describing this collection in less than 1024 characters"));

        return extrinsicObject;
    }

    private ExtrinsicObjectType getFourthExtrinsicObject() {
        ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
        extrinsicObject.setId("urn:service:params:id0");
        extrinsicObject.setMimeType("application/octet-stream");
        extrinsicObject.setIsOpaque(false);
        extrinsicObject.setContentVersionInfo(getVersionInfo("versionName"));

        extrinsicObject.getSlot()
                .add(stHelper.create("parameters", "param1", "xs:string"));

        extrinsicObject.setObjectType("urn:registry:content:collection");

        return extrinsicObject;
    }

    private ServiceType getService() {
        ServiceType service = RIM_FACTORY.createServiceType();
        service.setId("urn:service:id0");
        service.setObjectType("registry:federation:service");

        service.getServiceBinding()
                .add(getFirstServiceBinding());
        service.getServiceBinding()
                .add(getSecondServiceBinding());

        return service;
    }

    private ServiceBindingType getFirstServiceBinding() {
        ServiceBindingType binding = RIM_FACTORY.createServiceBindingType();
        binding.setId("urn:registry:federation:method:csw");
        binding.setService("urn:service:id0");

        binding.getSlot()
                .add(stHelper.create("cswUrl", "https://some/address/here", "xs:anyURI"));
        binding.getSlot()
                .add(stHelper.create("bindingType", "Csw_Federated_Source", "xs:string"));
        binding.getSlot()
                .add(stHelper.create("serviceType", "REST", "xs:string"));
        binding.getSlot()
                .add(stHelper.create("endpointDocumentation",
                        "https://some/path/to/docs.html",
                        "xs:anyURI"));

        binding.setName(istHelper.create("CSW Federation Method"));
        binding.setDescription(istHelper.create("This is the CSW federation method."));
        binding.setVersionInfo(getVersionInfo("2.0.2"));

        binding.getSpecificationLink()
                .add(getFirstSpecificationLink());

        return binding;
    }

    private SpecificationLinkType getFirstSpecificationLink() {
        SpecificationLinkType specificationLink = RIM_FACTORY.createSpecificationLinkType();

        specificationLink.setId("urn:request:parameters");
        specificationLink.setServiceBinding("urn:registry:federation:method:csw");
        specificationLink.setSpecificationObject("urn:service:params:id0");

        return specificationLink;
    }

    private ServiceBindingType getSecondServiceBinding() {
        ServiceBindingType binding = RIM_FACTORY.createServiceBindingType();
        binding.setId("urn:registry:federation:method:soap13");
        binding.setService("urn:service:id0");
        binding.setAccessURI("some:access:URI:any:URI");
        binding.setTargetBinding("some:target:binding:reference:URI");

        binding.getSlot()
                .add(stHelper.create("queryAddress", "https://some/address/here", "xs:anyURI"));
        binding.getSlot()
                .add(stHelper.create("ingestAddress", "https://some/address/here", "xs:anyURI"));
        binding.getSlot()
                .add(stHelper.create("eventAddress", "https://some/address/here", "xs:anyURI"));
        binding.getSlot()
                .add(stHelper.create("bindingType", "soap13", "xs:string"));
        binding.getSlot()
                .add(stHelper.create("serviceType", "SOAP", "xs:string"));
        binding.getSlot()
                .add(stHelper.create("endpointDocumentation",
                        "https://some/path/to/docs.html",
                        "xs:anyURI"));
        binding.setName(istHelper.create("Soap Federation Method"));
        binding.setDescription(istHelper.create("This is the Soap federation method."));
        binding.setVersionInfo(getVersionInfo("1.3"));

        binding.getSpecificationLink()
                .add(getSecondSpecificationLink());

        return binding;
    }

    private SpecificationLinkType getSecondSpecificationLink() {
        SpecificationLinkType specificationLink = RIM_FACTORY.createSpecificationLinkType();

        specificationLink.setId("notARealId");
        specificationLink.setServiceBinding("notARealServiceBinding");
        specificationLink.setSpecificationObject("notARealSpecificationObject");
        specificationLink.setUsageDescription(istHelper.create("This is some usage description"));

        List<String> usageParameters = new ArrayList<>();
        usageParameters.add("someUsageParameter");

        specificationLink.setUsageParameter(usageParameters);

        return specificationLink;
    }

    private OrganizationType getFirstOrganization() {
        OrganizationType organization = RIM_FACTORY.createOrganizationType();

        organization.setId("urn:organization:id0");
        organization.setParent("urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        organization.setPrimaryContact("somePrimaryContact");
        organization.setLid("someLid");
        organization.setStatus("someStatus");

        organization.setName(istHelper.create("Codice"));
        organization.getAddress()
                .add(getAddress("Phoenix", "USA", "85037", "AZ", "1234 Some Street", null));

        organization.getTelephoneNumber()
                .add(getPhoneNumber("555", null, "1234", "555-5555", null));
        organization.getEmailAddress()
                .add(getEmailAddress("emailaddress@something.com", null));

        ClassificationType classification = RIM_FACTORY.createClassificationType();
        classification.setId("urn:classification:id0");
        classification.setClassifiedObject("classifiedObjectId");
        classification.setClassificationScheme("classificationScheme");
        classification.setClassificationNode("classificationNode");
        classification.setNodeRepresentation("nodeRepresentation");

        organization.getClassification()
                .add(classification);

        return organization;
    }

    private OrganizationType getSecondOrganization() {
        OrganizationType organization = RIM_FACTORY.createOrganizationType();

        organization.setId("urn:organization:id1");
        organization.setParent("urn:uuid:2014ca7f59ac46f495e32b4a67a51276");

        organization.setName(istHelper.create("MyOrg"));
        organization.getAddress()
                .add(getAddress("Phoenix", "USA", "85037", "AZ", "1234 Some Street", "3914"));
        organization.getTelephoneNumber()
                .add(getPhoneNumber("555", null, "1234", "555-5555", null));
        organization.getEmailAddress()
                .add(getEmailAddress("emailaddress@something.com", "SomeEmailAddressType"));

        return organization;
    }

    private PersonType getFirstPerson() {
        PersonType person = RIM_FACTORY.createPersonType();

        person.setId("urn:contact:id0");
        person.setPersonName(getPersonName("john", "doe", "middleName"));
        person.getTelephoneNumber()
                .add(getPhoneNumber("111", "country", "1234", "111-1111", "cell phone"));
        person.getEmailAddress()
                .add(getEmailAddress("emailaddress@something.com", null));
        person.getAddress()
                .add(getAddress("Phoenix", "USA", "85037", "AZ", "1234 Some Street", "1234"));

        return person;
    }

    private PersonType getSecondPerson() {
        PersonType person = RIM_FACTORY.createPersonType();

        person.setId("urn:contact:id1");
        person.setPersonName(getPersonName("john1", "doe1", null));
        person.getTelephoneNumber()
                .add(getPhoneNumber("111", null, "1234", "111-1111", null));
        person.getEmailAddress()
                .add(getEmailAddress("emailaddress@something.com", null));

        return person;
    }

    private PersonType getThirdPerson() {
        PersonType person = RIM_FACTORY.createPersonType();

        person.setId("urn:contact:id2");
        person.setPersonName(getPersonName("john2", "doe2", null));
        person.getTelephoneNumber()
                .add(getPhoneNumber("111", null, "1234", "111-1111", null));
        person.getEmailAddress()
                .add(getEmailAddress("emailaddress@something.com", null));

        return person;
    }

    private AssociationType1 getFirstAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();
        association.setId("urn:association:1");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:registry:node");
        association.setTargetObject("urn:contact:id0");
        return association;
    }

    private AssociationType1 getSecondAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();

        association.setId("urn:association:2");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:registry:node");
        association.setTargetObject("urn:organization:id0");

        return association;
    }

    private AssociationType1 getThirdAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();
        association.setId("urn:association:3");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:contact:person1");
        association.setTargetObject("urn:content:collection:id0");
        return association;
    }

    private AssociationType1 getFourthAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();

        association.setId("urn:association:4");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:contact:person2");
        association.setTargetObject("urn:content:collection:id1");

        return association;
    }

    private AssociationType1 getFifthAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();
        association.setId("urn:association:5");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:organization:id1");
        association.setTargetObject("urn:service:id0");
        return association;
    }

    private AssociationType1 getSixthAssociation() {
        AssociationType1 association = RIM_FACTORY.createAssociationType1();
        association.setId("urn:association:6");
        association.setAssociationType("RelatedTo");
        association.setSourceObject("urn:organization:id0");
        association.setTargetObject("urn:content:collection:id0");

        return association;
    }

    private PostalAddressType getAddress(String city, String country, String zip, String state,
            String street, String streetNumber) {
        PostalAddressType address = RIM_FACTORY.createPostalAddressType();
        address.setCity(city);
        address.setCountry(country);
        address.setPostalCode(zip);
        address.setStateOrProvince(state);
        address.setStreet(street);
        address.setStreetNumber(streetNumber);

        return address;
    }

    private TelephoneNumberType getPhoneNumber(String areaCode, String countryCode,
            String extension, String number, String type) {
        TelephoneNumberType phoneNumber = RIM_FACTORY.createTelephoneNumberType();

        phoneNumber.setAreaCode(areaCode);
        phoneNumber.setCountryCode(countryCode);
        phoneNumber.setExtension(extension);
        phoneNumber.setNumber(number);
        phoneNumber.setPhoneType(type);

        return phoneNumber;
    }

    private EmailAddressType getEmailAddress(String address, String type) {
        EmailAddressType emailAddress = RIM_FACTORY.createEmailAddressType();
        emailAddress.setAddress(address);
        emailAddress.setType(type);

        return emailAddress;
    }

    private PersonNameType getPersonName(String firstName, String lastname, String middleName) {
        PersonNameType personName = RIM_FACTORY.createPersonNameType();
        personName.setFirstName(firstName);
        personName.setLastName(lastname);
        personName.setMiddleName(middleName);
        return personName;
    }

    private VersionInfoType getVersionInfo(String version) {
        VersionInfoType versionInfoType = RIM_FACTORY.createVersionInfoType();
        versionInfoType.setVersionName(version);

        return versionInfoType;
    }

    private RegistryObjectType getRegistryObjectFromResource(String path) throws ParserException {
        RegistryObjectType registryObject = null;
        JAXBElement<RegistryObjectType> jaxbRegistryObject = parser.unmarshal(configurator,
                JAXBElement.class,
                getClass().getResourceAsStream(path));

        if (jaxbRegistryObject != null) {
            registryObject = jaxbRegistryObject.getValue();
        }

        return registryObject;
    }

}
