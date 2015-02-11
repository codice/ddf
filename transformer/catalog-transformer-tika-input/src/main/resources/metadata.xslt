<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<!-- Copy all nodes that we dont want to handle specificly -->
<xsl:template match="/*">
<metadata>
<xsl:apply-templates select="@*|node()"/>
</metadata>
</xsl:template>

<xsl:template match="*">
<xsl:element name="{local-name()}">
<xsl:apply-templates select="@* | node()" />
</xsl:element>
</xsl:template>

<!-- template to copy attributes -->
<xsl:template match="@*">
<xsl:attribute name="{local-name()}">
<xsl:value-of select="." />
</xsl:attribute>
</xsl:template>

<!-- template to copy the rest of the nodes -->
<xsl:template match="comment() | text() | processing-instruction()">
<xsl:copy />
</xsl:template>
</xsl:stylesheet>