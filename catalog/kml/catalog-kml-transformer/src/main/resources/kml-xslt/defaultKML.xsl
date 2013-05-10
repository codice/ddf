<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns="http://www.opengis.net/kml/2.2" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/"
	xmlns:gml="http://www.opengis.net/gml" xmlns:xs="http://www.w3.org/2001/XMLSchema" exclude-result-prefixes="xsl ddms gml"
>
	<xsl:output method="xml" />
	<xsl:include href="defaultLibrary.xsl" />
	<xsl:template match="/">
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template match="ddms:Resource">
<!-- Name and Open are now being set in the java code -->
<!-- 		<name> -->
<!-- 			<xsl:value-of select="./ddms:title" /> -->
<!-- 		</name> -->
<!-- 		<open>1</open> -->
		<Placemark >
			<xsl:attribute name="id"><xsl:value-of select="$id" /></xsl:attribute>
			<name>
				<xsl:value-of select="./ddms:title" />
			</name>
			<styleUrl>#defaultStyle</styleUrl>
			<description>
				<xsl:value-of select=".//ddms:description" />
              <![CDATA[
		<a href="]]><xsl:value-of select="$resturl" /><![CDATA[">]]>
				<xsl:value-of select="//ddms:title" />
		<![CDATA[</a>]]>
			</description>
			<!--<xsl:apply-templates select=".//ddms:dates" />-->  <!-- this takes precendence.  the last one in the kml is the most important. -->
			<xsl:apply-templates select=".//ddms:boundingGeometry/node()" />
			<xsl:apply-templates select=".//ddms:boundingBox" />

		</Placemark>
	</xsl:template>

</xsl:stylesheet>