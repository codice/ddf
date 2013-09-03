<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:gml="http://www.opengis.net/gml">

	<xsl:output method="html" omit-xml-declaration="yes" indent="no" />
	<xsl:variable name="disk"
		select="'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABGdBTUEAAK/INwWK6QAAABl0RVh0
													U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAH+SURBVBgZBcE9i11VGAbQtc/sO0OCkqhg
													hEREAwpWAWUg8aMVf4KFaJEqQtAipTZWViKiCGOh2Ap2gmJhlSIWFsFOxUK0EsUM3pl79n4f12qH
													b3z3Fh7D83gC95GOJsDe0ixLk5Qq/+xv/Lw9Xd+78/HLX3Y8fXTr2nWapy4eCFKxG7Fby97SnDlY
													tMbxthyfzHO//nl85fNvfvnk8MbX5xa8IHx1518Vkrj54Q+qQms2vVmWZjdiu5ZR2rT01166/NCZ
													g/2PFjwSVMU6yjoC1oq+x6Y3VbHdlXWExPd379nf7Nmejv2Os6OC2O4KLK0RNn3RNCdr2Z5GJSpU
													4o+/TkhaJ30mEk5HwNuvX7Hpi76wzvjvtIwqVUSkyjqmpHS0mki8+9mPWmuWxqYvGkbFGCUAOH/+
													QevYI9GFSqmaHr5wkUYTAlGhqiRRiaqiNes6SOkwJwnQEqBRRRJEgkRLJGVdm6R0GLMQENE0Ekmk
													SkQSVVMqopyuIaUTs0J455VLAAAAAODW0U/GiKT0pTWziEj44PZ1AAAAcPPqkTmH3QiJrlEVDXDt
													0qsAAAAAapa5BqUnyaw0Am7//gUAAAB49tEXzTmtM5KkV/y2G/X4M5fPao03n/sUAAAAwIX7y5yB
													v9vhjW/fT/IkuSp5gJKElKRISYoUiSRIyD1tufs/IXxui20QsKIAAAAASUVORK5CYII='" />
	<xsl:template match="/">
		<html>
			<head>
				<style type="text/css">
					#soft-table
					{
					font-family: "Lucida Sans Unicode",
					"Lucida Grande",
					Sans-Serif;
					font-size: 12px;
					margin: 10px;
					text-align: center;
					border-collapse: collapse;
					border-top: 7px solid
					#9baff1;
					}
					#soft-table th
					{
					font-size: 15px;
					font-weight: thick;
					padding: 8px;
					background: #e8edff;
					border-right: 1px solid
					#9baff1;
					border-left: 1px solid #9baff1;
					color: #039;
					}
					#soft-table td
					{
					padding: 8px;
					border-right: 1px
					solid #aabcfe;
					border-left:
					1px
					solid
					#aabcfe;
					color: #669;
					}
					#soft-table tr:hover
					td
					{
					background:#d0dafd;
					color:#339;
					}
					#soft-table .odd
					{
					background: #f9f9f9;
					}
				</style>
				<title>DDF Query Response</title>
			</head>
			<body>
				<xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
	<xsl:template match="results">
		<font size="1">
			<!-- <xsl:value-of select="@totalNum" /> -->
			<xsl:value-of select="count(metacard/document)" />
			<xsl:text> results</xsl:text>
		</font>
		<table id="soft-table" border="1">
			<thead>
				<tr>
					<th>#</th>
					<th>Title</th>
					<th>Date</th>
					<th>Product</th>
					<th>Spatial</th>
					<th>Thumbnail</th>
				</tr>
			</thead>
			<tbody>
				<xsl:for-each select="metacard/document">
					<tr>
						<xsl:if test="(position() mod 2) = 0">
							<xsl:attribute name="class">odd</xsl:attribute>
						</xsl:if>
						<td>
							<center>
								<xsl:value-of select="position()" />
							</center>
						</td>
						<td>
							<a>
								<xsl:attribute name="href"><xsl:text>/services/catalog/</xsl:text>
                  <xsl:value-of select="../id" /><xsl:text>?transform=html</xsl:text>
                    </xsl:attribute>
								<xsl:value-of select="../title" />
							</a>
						</td>
						<td>
							<xsl:value-of select="../created" />
						</td>
						<td>
							<xsl:if test="../product">
								<a>
									<xsl:attribute name="href">
                    <xsl:value-of select="../product" />
                      </xsl:attribute>
									<img src="{$disk}" />
								</a>
							</xsl:if>
						</td>
						<td>
							<!-- sum() div count() <xsl:value-of select="tokenize(.//gml:exterior/gml:LinearRing/gml:pos,' 
								')[2]" /> <xsl:for-each select="gml:exterior/gml:LinearRing/gml:pos"> <xsl:value-of 
								select="tokenize(.,' ')[2]" /> <xsl:text>,</xsl:text> <xsl:value-of select="tokenize(.,' 
								')[1]" /> <xsl:text>,0 </xsl:text> </xsl:for-each> -->

							<xsl:value-of select="../location" />
						</td>
						<td>
							<xsl:if test="../thumbnail">
								<img>
									<xsl:attribute name="src">
                    							<xsl:text>data:</xsl:text>
												<xsl:value-of select="../t_mimetype" />
                    							<xsl:text>;base64,</xsl:text>
                    							<xsl:value-of select="../thumbnail" />
	                  						</xsl:attribute>
								</img>
							</xsl:if>
						</td>
					</tr>
				</xsl:for-each>
			</tbody>
		</table>
	</xsl:template>
</xsl:stylesheet>