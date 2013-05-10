<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml" >

	<xsl:output method="html" encoding="utf-8" omit-xml-declaration="yes" />

	<xsl:template match="/">
		<xsl:call-template name="prettyprint" />
	</xsl:template>


	<xsl:template name="prettyprint">
		<HTML>
			<HEAD>
				<TITLE>
					<xsl:value-of select="title" />
				</TITLE>
				<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
				<style type="text/css">
					span {
					display:inline ;
					}
					body {
					background: #FFF; ;
					font:14px Cambria,Georgia,Myriad Pro,Arial,Helvetica;

					valign: top;
					}

					span.elementName {
					/*background: #36393D;*/
					color: #000000;
					}
					span.elementValue{
					display:inline-block;
					}
					span.elementTextValue{
					font-weight: bold;
					color:
					#F;
					}
					span.attributes {

					}
					span.attrName{
					color: #000;
					background:#EEEEEE;
					border-right: solid 1px silver;
					border-bottom:solid 1px #999;
					text-align: left;

					}
					span.attrVal {
					color: #1D7D65;

					}
					ul {
					list-style: none;
					margin: 0;
					padding: 0;
					text-indent: 0;
					display:inline;
					margin-top: 0px;
					margin-bottom: 0px;

					}
					li {

					}
					td {
					vertical-align: top;
					padding:0;
					margin:0;


					}
					td.tdelementName {
					width:150px;
					background: #E2E2E2;
					border-bottom:1px solid #888888;
					border-right:1px
					solid #888888;
					}
					table,tr,th,td,div,span,ul,li {
					padding:0;
					margin:0;
					}
				</style>
				<script type="text/javascript">
					function toggle(obj)
					{
						var tds = obj.getElementsByTagName('td');
						
						if (tds.length == 2)
						{
							var secondTd = tds[1];
							
							if(secondTd.style.display == 'none')
							{
								secondTd.style.display = 'block';
							}
							else
							{
								secondTd.style.display = 'none';
							}
						}
					}
				</script>
			</HEAD>

			<BODY>
				<div id="root">
					<xsl:apply-templates select="node()" />
				</div>
			</BODY>
		</HTML>
	</xsl:template>

	<!-- Handle elements -->
	<xsl:template match="*">
		<ul>
			<li>
				<table onDblClick="toggle(this);" border="0">
					<tr>
						<td class="tdelementName">
							<span class="elementName">
								<xsl:value-of select="local-name()" />
							</span>
						</td>
						<td>
						<xsl:attribute name="id"><xsl:text>col</xsl:text><xsl:value-of select="position()" /></xsl:attribute>
							<span class="elementValue" id="elementValue">
								<!-- <xsl:apply-templates select="." mode="process-ns-declarations"/> -->
								<ul>

									<xsl:if test="not(count(@*) = 0 )">
										<xsl:apply-templates select="@*" />
									</xsl:if>

									<xsl:if test="text()">
										<span class="elementTextValue">
											<li>
												<xsl:apply-templates select="text()" />
											</li>
										</span>
									</xsl:if>
									<xsl:if test="*">
										<xsl:apply-templates select="*" />
									</xsl:if>

								</ul>
							</span>
						</td>
					</tr>
				</table>
			</li>
		</ul>
	</xsl:template>

	<!-- Processing of attributes -->

	<xsl:template match="@*">

		<li>
			<span class="attrName">
				<xsl:value-of select="local-name()" />
			</span>
			<span class="attrVal">
				<xsl:value-of select="." />
			</span>
		</li>

	</xsl:template>


</xsl:stylesheet>
