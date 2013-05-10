<?xml version="1.0" encoding="UTF-8"?>
<!--
/*
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

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:atom="http://www.w3.org/2005/Atom"
  xmlns:fsa="http://www.intelink.gov/fsa/elements/1.0/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/" xmlns:media="http://search.yahoo.com/mrss/"
  xmlns:ices="http://www.intelink.gov/ices/elements/1.0/" xmlns:gsa="http://base.google.com/ns/1.0"
  xmlns:gml="http://www.opengis.net/gml" xmlns:georss="http://www.georss.org/georss"
  xmlns:fs="http://a9.com/-/opensearch/extensions/federation/1.0/"
  xmlns:document="http://www.intelink.gov/entercat2/document"
  xmlns:ddms20="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:relevance="http://a9.com/-/opensearch/extensions/relevance/1.0/" 
  xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/" xmlns:ICISM="urn:us:gov:ic:ism:v2">
  <xsl:output method="xml" omit-xml-declaration="yes" indent="yes" />

  <xsl:param name="applyDefaultSecurity">false</xsl:param>
  
  <xsl:param name="classificationTitle">U</xsl:param>
  <xsl:param name="ownerProducerTitle">USA</xsl:param>
  <xsl:param name="SCIcontrolsTitle">UNKNOWN</xsl:param>
  <xsl:param name="SARIdentifierTitle">UNKNOWN</xsl:param>
  <xsl:param name="disseminationControlsTitle">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceOpenTitle">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceProtectedTitle">UNKNOWN</xsl:param>
  <xsl:param name="releasableToTitle">UNKNOWN</xsl:param>
  <xsl:param name="nonICmarkingsTitle">UNKNOWN</xsl:param>
  <xsl:param name="classifiedByTitle">UNKNOWN</xsl:param>
  <xsl:param name="derivativelyClassifiedByTitle">UNKNOWN</xsl:param>
  <xsl:param name="classificationReasonTitle">UNKNOWN</xsl:param>
  <xsl:param name="derivedFromTitle">UNKNOWN</xsl:param>
  <xsl:param name="declassDateTitle">9999-01-01</xsl:param>
  <xsl:param name="declassEventTitle">UNKNOWN</xsl:param>
  <xsl:param name="declassExceptionTitle">UNKNOWN</xsl:param>
  <xsl:param name="typeOfExemptedSourceTitle">UNKNOWN</xsl:param>
  <xsl:param name="dateOfExemptedSourceTitle">UNKNOWN</xsl:param>
  <xsl:param name="declassManualReviewTitle">UNKNOWN</xsl:param>

  <xsl:param name="classificationDescription">U</xsl:param>
  <xsl:param name="ownerProducerDescription">USA</xsl:param>
  <xsl:param name="SCIcontrolsDescription">UNKNOWN</xsl:param>
  <xsl:param name="SARIdentifierDescription">UNKNOWN</xsl:param>
  <xsl:param name="disseminationControlsDescription">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceOpenDescription">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceProtectedDescription">UNKNOWN</xsl:param>
  <xsl:param name="releasableToDescription">UNKNOWN</xsl:param>
  <xsl:param name="nonICmarkingsDescription">UNKNOWN</xsl:param>
  <xsl:param name="classifiedByDescription">UNKNOWN</xsl:param>
  <xsl:param name="derivativelyClassifiedByDescription">UNKNOWN</xsl:param>
  <xsl:param name="classificationReasonDescription">UNKNOWN</xsl:param>
  <xsl:param name="derivedFromDescription">UNKNOWN</xsl:param>
  <xsl:param name="declassDateDescription">9999-01-01</xsl:param>
  <xsl:param name="declassEventDescription">UNKNOWN</xsl:param>
  <xsl:param name="declassExceptionDescription">UNKNOWN</xsl:param>
  <xsl:param name="typeOfExemptedSourceDescription">UNKNOWN</xsl:param>
  <xsl:param name="dateOfExemptedSourceDescription">UNKNOWN</xsl:param>
  <xsl:param name="declassManualReviewDescription">UNKNOWN</xsl:param>
  
  <xsl:param name="classificationSecurity">U</xsl:param>
  <xsl:param name="ownerProducerSecurity">USA</xsl:param>
  <xsl:param name="SCIcontrolsSecurity">UNKNOWN</xsl:param>
  <xsl:param name="SARIdentifierSecurity">UNKNOWN</xsl:param>
  <xsl:param name="disseminationControlsSecurity">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceOpenSecurity">UNKNOWN</xsl:param>
  <xsl:param name="FGIsourceProtectedSecurity">UNKNOWN</xsl:param>
  <xsl:param name="releasableToSecurity">UNKNOWN</xsl:param>
  <xsl:param name="nonICmarkingsSecurity">UNKNOWN</xsl:param>
  <xsl:param name="classifiedBySecurity">UNKNOWN</xsl:param>
  <xsl:param name="derivativelyClassifiedBySecurity">UNKNOWN</xsl:param>
  <xsl:param name="classificationReasonSecurity">UNKNOWN</xsl:param>
  <xsl:param name="derivedFromSecurity">UNKNOWN</xsl:param>
  <xsl:param name="declassDateSecurity">9999-01-01</xsl:param>
  <xsl:param name="declassEventSecurity">UNKNOWN</xsl:param>
  <xsl:param name="declassExceptionSecurity">UNKNOWN</xsl:param>
  <xsl:param name="typeOfExemptedSourceSecurity">UNKNOWN</xsl:param>
  <xsl:param name="dateOfExemptedSourceSecurity">UNKNOWN</xsl:param>
  <xsl:param name="declassManualReviewSecurity">UNKNOWN</xsl:param>  

  <xsl:template match="/">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/atom:feed">
    <resultSet>
      <xsl:for-each select="atom:entry">
      	<xsl:if test="ddms:Resource or $applyDefaultSecurity = 'true'">
        <ddms:Resource xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/"
          xmlns:ICISM="urn:us:gov:ic:ism:v2" xmlns:gml="http://www.opengis.net/gml">
          <xsl:attribute name="score">
            <xsl:value-of select="relevance:score" />
          </xsl:attribute>
            <!-- DDF 2.0.0 uses atom:id, CDDA does not include that element, so we use the link -->
          <xsl:attribute name="id">
              <xsl:choose>
                  <xsl:when test="atom:id">
                      <xsl:value-of select="atom:id" />
                  </xsl:when>
                  <xsl:otherwise>
                      <xsl:value-of select="atom:link/@href" />
                  </xsl:otherwise>
              </xsl:choose>
          </xsl:attribute>
          <xsl:attribute name="date">
            <xsl:value-of select="atom:updated" />
          </xsl:attribute>
          <xsl:choose>
            <xsl:when test="ddms:Resource">
            <!-- <xsl:apply-templates select="ddms:Resource" /> -->
              <xsl:copy-of select="ddms:Resource/*" />
            </xsl:when>
            <xsl:otherwise>
          <!--No DDMS in ATOM content -->
              <ddms:identifier>
                <xsl:attribute name="ddms:qualifier">
                  <xsl:text>http://metadata.dod.mil/mdr/ns/MDR/0.1/MDR.owl#URI</xsl:text>
                </xsl:attribute>
                <xsl:attribute name="ddms:value">
                <!-- DDF 2.0.0 uses atom:id, CDDA does not include that element, so we use the link -->
                <xsl:choose>
                    <xsl:when test="atom:id">
                        <xsl:value-of select="atom:id" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="atom:link/@href" />
                    </xsl:otherwise>
                </xsl:choose>
                </xsl:attribute>
              </ddms:identifier>
              <ddms:title>
                <xsl:attribute name="ICISM:classification">
                    <xsl:value-of select="$classificationTitle" />
                  </xsl:attribute>
                <xsl:attribute name="ICISM:ownerProducer">
                    <xsl:value-of select="$ownerProducerTitle" />
                  </xsl:attribute>
                <xsl:if test="$classificationTitle != 'U'">
	        	    <xsl:if test="$SCIcontrolsTitle != ''">
		              <xsl:attribute name="ICISM:SCIcontrols">
	                    <xsl:value-of select="$SCIcontrolsTitle" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$SARIdentifierTitle != ''">
		              <xsl:attribute name="ICISM:SARIdentifier">
	                    <xsl:value-of select="$SARIdentifierTitle" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$disseminationControlsTitle != ''">
		              <xsl:attribute name="ICISM:disseminationControls">
	                    <xsl:value-of select="$disseminationControlsTitle" />
	                  </xsl:attribute>
		            </xsl:if>	            
	        	    <xsl:if test="$FGIsourceOpenTitle != ''">
		              <xsl:attribute name="ICISM:FGIsourceOpen">
	                    <xsl:value-of select="$FGIsourceOpenTitle" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$FGIsourceProtectedTitle != ''">
		              <xsl:attribute name="ICISM:FGIsourceProtected">
	                    <xsl:value-of select="$FGIsourceProtectedTitle" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$releasableToTitle != ''">
		              <xsl:attribute name="ICISM:releasableTo">
	                    <xsl:value-of select="$releasableToTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$nonICmarkingsTitle != ''">
		              <xsl:attribute name="ICISM:nonICmarkings">
	                    <xsl:value-of select="$nonICmarkingsTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		            <xsl:if test="$classifiedByTitle != ''">
		              <xsl:attribute name="ICISM:classifiedBy">
	                    <xsl:value-of select="$classifiedByTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivativelyClassifiedByTitle != ''">
		              <xsl:attribute name="ICISM:derivativelyClassifiedBy">
	                    <xsl:value-of select="$derivativelyClassifiedByTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$classificationReasonTitle != ''">
		              <xsl:attribute name="ICISM:classificationReason">
	                    <xsl:value-of select="$classificationReasonTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivedFromTitle != ''">
		              <xsl:attribute name="ICISM:derivedFrom">
	                    <xsl:value-of select="$derivedFromTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassDateTitle != ''">
		              <xsl:attribute name="ICISM:declassDate">
	                    <xsl:value-of select="$declassDateTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassEventTitle != ''">
		              <xsl:attribute name="ICISM:declassEvent">
	                    <xsl:value-of select="$declassEventTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassExceptionTitle != ''">
		              <xsl:attribute name="ICISM:declassException">
	                    <xsl:value-of select="$declassExceptionTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$typeOfExemptedSourceTitle != ''">
		              <xsl:attribute name="ICISM:typeOfExemptedSource">
	                    <xsl:value-of select="$typeOfExemptedSourceTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$dateOfExemptedSourceTitle != ''">
		              <xsl:attribute name="ICISM:dateOfExemptedSource">
	                    <xsl:value-of select="$dateOfExemptedSourceTitle" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassManualReviewTitle != ''">
		              <xsl:attribute name="ICISM:declassManualReview">
	                    <xsl:value-of select="$declassManualReviewTitle" />
	                  </xsl:attribute>
		            </xsl:if>
	            </xsl:if>
                <xsl:value-of select="atom:title" />
              </ddms:title>
              <xsl:if test="atom:summary">
                <ddms:description>
                  <xsl:attribute name="ICISM:classification">
                    <xsl:value-of select="$classificationDescription" />
                  </xsl:attribute>
                  <xsl:attribute name="ICISM:ownerProducer">
                    <xsl:value-of select="$ownerProducerDescription" />
                  </xsl:attribute>
                <xsl:if test="$classificationDescription != 'U'">
	        	    <xsl:if test="$SCIcontrolsDescription != ''">
		              <xsl:attribute name="ICISM:SCIcontrols">
	                    <xsl:value-of select="$SCIcontrolsDescription" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$SARIdentifierDescription != ''">
		              <xsl:attribute name="ICISM:SARIdentifier">
	                    <xsl:value-of select="$SARIdentifierDescription" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$disseminationControlsDescription != ''">
		              <xsl:attribute name="ICISM:disseminationControls">
	                    <xsl:value-of select="$disseminationControlsDescription" />
	                  </xsl:attribute>
		            </xsl:if>	            
	        	    <xsl:if test="$FGIsourceOpenDescription != ''">
		              <xsl:attribute name="ICISM:FGIsourceOpen">
	                    <xsl:value-of select="$FGIsourceOpenDescription" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$FGIsourceProtectedDescription != ''">
		              <xsl:attribute name="ICISM:FGIsourceProtected">
	                    <xsl:value-of select="$FGIsourceProtectedDescription" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$releasableToDescription != ''">
		              <xsl:attribute name="ICISM:releasableTo">
	                    <xsl:value-of select="$releasableToDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$nonICmarkingsDescription != ''">
		              <xsl:attribute name="ICISM:nonICmarkings">
	                    <xsl:value-of select="$nonICmarkingsDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		            <xsl:if test="$classifiedByDescription != ''">
		              <xsl:attribute name="ICISM:classifiedBy">
	                    <xsl:value-of select="$classifiedByDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivativelyClassifiedByDescription != ''">
		              <xsl:attribute name="ICISM:derivativelyClassifiedBy">
	                    <xsl:value-of select="$derivativelyClassifiedByDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$classificationReasonDescription != ''">
		              <xsl:attribute name="ICISM:classificationReason">
	                    <xsl:value-of select="$classificationReasonDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivedFromDescription != ''">
		              <xsl:attribute name="ICISM:derivedFrom">
	                    <xsl:value-of select="$derivedFromDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassDateDescription != ''">
		              <xsl:attribute name="ICISM:declassDate">
	                    <xsl:value-of select="$declassDateDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassEventDescription != ''">
		              <xsl:attribute name="ICISM:declassEvent">
	                    <xsl:value-of select="$declassEventDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassExceptionDescription != ''">
		              <xsl:attribute name="ICISM:declassException">
	                    <xsl:value-of select="$declassExceptionDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$typeOfExemptedSourceDescription != ''">
		              <xsl:attribute name="ICISM:typeOfExemptedSource">
	                    <xsl:value-of select="$typeOfExemptedSourceDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$dateOfExemptedSourceDescription != ''">
		              <xsl:attribute name="ICISM:dateOfExemptedSource">
	                    <xsl:value-of select="$dateOfExemptedSourceDescription" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassManualReviewDescription != ''">
		              <xsl:attribute name="ICISM:declassManualReview">
	                    <xsl:value-of select="$declassManualReviewDescription" />
	                  </xsl:attribute>
		            </xsl:if>
	            </xsl:if>
                  <xsl:value-of select="atom:summary" />
                </ddms:description>
              </xsl:if>
              <xsl:if test="atom:updated">
                <ddms:dates>
                  <xsl:attribute name="ddms:posted">
                    <xsl:value-of select="atom:updated" />
                  </xsl:attribute>
                </ddms:dates>
              </xsl:if>
              <ddms:publisher>
                <ddms:Organization>
                  <ddms:name>
                    <xsl:choose>
                      <xsl:when test="fs:resultSource">
                        <xsl:value-of select="fs:resultSource" />
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:text>UNKNOWN</xsl:text>
                      </xsl:otherwise>
                    </xsl:choose>
                  </ddms:name>
                </ddms:Organization>
              </ddms:publisher>
              <ddms:subjectCoverage>
                <ddms:Subject>
                  <ddms:category>
                    <xsl:attribute name="ddms:label">
                      <xsl:choose>
                        <xsl:when
                      test="media:content/@type and media:content/@type != ''">
                          <xsl:value-of select="media:content/@type" />
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:text>UNKNOWN</xsl:text>
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:attribute>
                  </ddms:category>
                </ddms:Subject>
              </ddms:subjectCoverage>
              <ddms:security>
                <xsl:attribute name="ICISM:classification">
                  <xsl:value-of select="$classificationSecurity" />
                </xsl:attribute>
                <xsl:attribute name="ICISM:ownerProducer">
                  <xsl:value-of select="$ownerProducerSecurity" />
                </xsl:attribute>
                <xsl:if test="$classificationSecurity != 'U'">
	        	    <xsl:if test="$SCIcontrolsSecurity != ''">
		              <xsl:attribute name="ICISM:SCIcontrols">
	                    <xsl:value-of select="$SCIcontrolsSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$SARIdentifierSecurity != ''">
		              <xsl:attribute name="ICISM:SARIdentifier">
	                    <xsl:value-of select="$SARIdentifierSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$disseminationControlsSecurity != ''">
		              <xsl:attribute name="ICISM:disseminationControls">
	                    <xsl:value-of select="$disseminationControlsSecurity" />
	                  </xsl:attribute>
		            </xsl:if>	            
	        	    <xsl:if test="$FGIsourceOpenSecurity != ''">
		              <xsl:attribute name="ICISM:FGIsourceOpen">
	                    <xsl:value-of select="$FGIsourceOpenSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$FGIsourceProtectedSecurity != ''">
		              <xsl:attribute name="ICISM:FGIsourceProtected">
	                    <xsl:value-of select="$FGIsourceProtectedSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
	        	    <xsl:if test="$releasableToSecurity != ''">
		              <xsl:attribute name="ICISM:releasableTo">
	                    <xsl:value-of select="$releasableToSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$nonICmarkingsSecurity != ''">
		              <xsl:attribute name="ICISM:nonICmarkings">
	                    <xsl:value-of select="$nonICmarkingsSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		            <xsl:if test="$classifiedBySecurity != ''">
		              <xsl:attribute name="ICISM:classifiedBy">
	                    <xsl:value-of select="$classifiedBySecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivativelyClassifiedBySecurity != ''">
		              <xsl:attribute name="ICISM:derivativelyClassifiedBy">
	                    <xsl:value-of select="$derivativelyClassifiedBySecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$classificationReasonSecurity != ''">
		              <xsl:attribute name="ICISM:classificationReason">
	                    <xsl:value-of select="$classificationReasonSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$derivedFromSecurity != ''">
		              <xsl:attribute name="ICISM:derivedFrom">
	                    <xsl:value-of select="$derivedFromSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassDateSecurity != ''">
		              <xsl:attribute name="ICISM:declassDate">
	                    <xsl:value-of select="$declassDateSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassEventSecurity != ''">
		              <xsl:attribute name="ICISM:declassEvent">
	                    <xsl:value-of select="$declassEventSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassExceptionSecurity != ''">
		              <xsl:attribute name="ICISM:declassException">
	                    <xsl:value-of select="$declassExceptionSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$typeOfExemptedSourceSecurity != ''">
		              <xsl:attribute name="ICISM:typeOfExemptedSource">
	                    <xsl:value-of select="$typeOfExemptedSourceSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$dateOfExemptedSourceSecurity != ''">
		              <xsl:attribute name="ICISM:dateOfExemptedSource">
	                    <xsl:value-of select="$dateOfExemptedSourceSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
		        	<xsl:if test="$declassManualReviewSecurity != ''">
		              <xsl:attribute name="ICISM:declassManualReview">
	                    <xsl:value-of select="$declassManualReviewSecurity" />
	                  </xsl:attribute>
		            </xsl:if>
	            </xsl:if> 
              </ddms:security>
              <xsl:copy-of select="." />
            </xsl:otherwise>
          </xsl:choose>
        </ddms:Resource>
        </xsl:if>
      </xsl:for-each>
    </resultSet>
  </xsl:template>

<!-- <xsl:template match="ddms:Resource">
    <ddms:Resource xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/"
      xmlns:ICISM="urn:us:gov:ic:ism:v2" xmlns:gml="http://www.opengis.net/gml">
      <xsl:copy-of select="ddms:identifier" />
      <xsl:copy-of select="ddms:title" />
      <xsl:copy-of select="ddms:description" />
      <xsl:copy-of select="ddms:creator" />
      <xsl:copy-of select="ddms:publisher" />
      <xsl:copy-of select="ddms:contributor" />
      <xsl:copy-of select="ddms:pointOfContact" />
      <ddms:subjectCoverage>
        <ddms:Subject>
          <ddms:category>
            <xsl:attribute name="ddms:label">
              <xsl:choose>
                <xsl:when
              test="media:content/@type and media:content/@type != ''">
                  <xsl:value-of select="media:content/@type" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>UNKNOWN</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
          </ddms:category>
        </ddms:Subject>
      </ddms:subjectCoverage>
      <ddms:security>
        <xsl:attribute name="ICISM:classification">
          <xsl:value-of select="$classification" />
        </xsl:attribute>
        <xsl:attribute name="ICISM:ownerProducer">
          <xsl:value-of select="$ownerProducer" />
        </xsl:attribute>
      </ddms:security>
    </ddms:Resource>
  </xsl:template> -->

  <xsl:template match="*">
    <xsl:element name="{name()}">
      <xsl:apply-templates select="@*|node()" />
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*">
    <xsl:attribute name="{name()}">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="processing-instruction()|comment()">
    <xsl:copy>
      <xsl:apply-templates select="node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>