<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns="http://www.opengis.net/kml/2.2"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:gml="http://www.opengis.net/gml"
	xmlns:ddms="http://metadata.dod.mil/mdr/ns/DDMS/2.0/">
	<xsl:param name="id" select="''"/>
	<xsl:param name="site" select="''" />
	<xsl:param name="resturl" select="''"/>
	<xsl:param name="services" select="''"/>
	
	<xsl:template match="ddms:boundingBox">
		<Polygon>
			<outerBoundaryIs>
				<LinearRing>
					<coordinates>
						<xsl:value-of select="ddms:WestBL/text()"/><xsl:text>,</xsl:text><xsl:value-of select="ddms:NorthBL/text()"/><xsl:text>&#xa;</xsl:text>
						<xsl:value-of select="ddms:EastBL/text()"/><xsl:text>,</xsl:text><xsl:value-of select="ddms:NorthBL/text()"/><xsl:text>&#xa;</xsl:text>
						<xsl:value-of select="ddms:EastBL/text()"/><xsl:text>,</xsl:text><xsl:value-of select="ddms:SouthBL/text()"/><xsl:text>&#xa;</xsl:text>
						<xsl:value-of select="ddms:WestBL/text()"/><xsl:text>,</xsl:text><xsl:value-of select="ddms:SouthBL/text()"/><xsl:text>&#xa;</xsl:text>
						<xsl:value-of select="ddms:WestBL/text()"/><xsl:text>,</xsl:text><xsl:value-of select="ddms:NorthBL/text()"/><xsl:text>&#xa;</xsl:text>
					</coordinates>
				</LinearRing>
			</outerBoundaryIs>
		</Polygon>
	</xsl:template>
	
	<xsl:template match="gml:Point">
		<Point>
			<coordinates>
				<xsl:apply-templates select="./*" />
			</coordinates>
		</Point>
	</xsl:template>

	<xsl:template match="gml:Polygon">
		<Polygon>
			<xsl:apply-templates select="gml:exterior" />
		</Polygon>
	</xsl:template>

	<xsl:template match="gml:exterior">
		<outerBoundaryIs>
			<xsl:if test="gml:LinearRing">
				<LinearRing>
					<coordinates>
						<xsl:apply-templates select="gml:LinearRing/*" />
					</coordinates>
				</LinearRing>
			</xsl:if>
		</outerBoundaryIs>
	</xsl:template>

	<xsl:template match="gml:pos">
      <xsl:variable name="aPoint"
                    select="tokenize(normalize-space(.),' ')" />

      <xsl:call-template name="handleLonLat">
         <xsl:with-param name="currentNode" select="current()" />
         <xsl:with-param name="splitPointString" select="$aPoint" />
      </xsl:call-template>
   </xsl:template>

   <!--
      Template: handleLonLat
      Pseudocode: (where aPoint is a tokenized gml point)

      if outSide gmlNamespace
         call insertLonLat(token1, token2)       
      else if srsName exists
         if WGS84E then
            call insertLonLat(token2, token1)
         else
            call insertLonLat(token1, token2)
         end if
      else 
         call findSrsName(parentNode)
      end if
   -->
   <xsl:template name="handleLonLat">
      <xsl:param name="currentNode" />
      <xsl:param name="splitPointString" />

      <xsl:choose>
         <xsl:when test="not(namespace-uri($currentNode) = 'http://www.opengis.net/gml')">
            <xsl:call-template name="insertLonLat">
               <xsl:with-param name="lon" select="$splitPointString[1]" />
               <xsl:with-param name="lat" select="$splitPointString[2]" />
            </xsl:call-template>
         </xsl:when>

         <xsl:when test="$currentNode/@srsName">
            <xsl:choose>
               <xsl:when test="($currentNode/@srsName = 'http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84E_2D') or ($currentNode/@srsName = 'http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84C_3D') or ($currentNode/@srsName = 'http://metadata.dod.mil/mdr/ns/GSIP/crs/WGS84E_3D')">
                  <xsl:call-template name="insertLonLat">
                     <xsl:with-param name="lon" select="$splitPointString[2]" />
                     <xsl:with-param name="lat" select="$splitPointString[1]" />
                  </xsl:call-template>                 
               </xsl:when>

               <xsl:otherwise>
                  <xsl:call-template name="insertLonLat">
                     <xsl:with-param name="lon" select="$splitPointString[1]" />
                     <xsl:with-param name="lat" select="$splitPointString[2]" />
                  </xsl:call-template>
               </xsl:otherwise>
            </xsl:choose>            
         </xsl:when>

         <xsl:otherwise>
            <xsl:call-template name="handleLonLat">
               <xsl:with-param name="currentNode" select="$currentNode/.." />
               <xsl:with-param name="splitPointString" select="$splitPointString" />
            </xsl:call-template>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <xsl:template name="insertLonLat">
      <xsl:param name="lon" />
      <xsl:param name="lat" />

      <xsl:value-of select="$lon" />
      <xsl:text>,</xsl:text>
      <xsl:value-of select="$lat" />
      <xsl:text>&#xa;</xsl:text>
   </xsl:template>
   	
	<xsl:template name="convertToMeters">
		<xsl:param name="distance" select="0"/>
		<xsl:param name="units" select="m"/>
		<xsl:choose>
			<xsl:when test="$units = 'cm' or $units = 'CM'">
				<!-- centimeter -->
				<xsl:value-of select="$distance * 0.0000054"/>
			</xsl:when>
			<xsl:when test="$units = 'ft' or $units = 'FT'">
				<!-- foot -->
				<xsl:value-of select="$distance * .3048006"/>
			</xsl:when>
			<xsl:when test="$units = 'inch' or $units = 'INCH'">
				<!-- inch -->
				<xsl:value-of select="$distance * 0.0254"/>
			</xsl:when>
			<xsl:when test="$units = 'km' or $units = 'KM'">
				<!-- kilometer -->
				<xsl:value-of select="$distance * 1000"/>
			</xsl:when>
			<xsl:when test="$units = 'mm' or $units = 'MM'">
				<!-- millimeter -->
				<xsl:value-of select="$distance * 0.001"/>
			</xsl:when>
			<xsl:when test="$units = 'yard' or $units = 'YARD'">
				<!-- yard -->
				<xsl:value-of select="$distance * .9144"/>
			</xsl:when>
			<xsl:when test="$units = 'mile' or $units = 'MILE'">
				<!-- mile -->
				<xsl:value-of select="$distance * 1609.344"/>
			</xsl:when>
			<xsl:when test="$units = 'nauticMile' or $units = 'NauticMile'">
				<!-- mile -->
				<xsl:value-of select="$distance * 1852.0"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$distance"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="ddms:temporalCoverage/ddms:TimePeriod">
		<TimeSpan>
			<begin>
				<xsl:value-of select="ddms:start/text()" />
			</begin>
			<end>
				<xsl:value-of select="ddms:end/text()" />
			</end>
		</TimeSpan>		
	</xsl:template>
	
	<xsl:template match="ddms:dates">
		<xsl:for-each select="@*">
			
			<TimeStamp>
				<xsl:attribute name="id">
					<xsl:value-of select="local-name()" />
				</xsl:attribute> 
				<when>
					<xsl:value-of select="."/>
				</when>
			</TimeStamp>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
