<!DOCTYPE html>
<!-- 
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 -->

<html>
<head>
<meta charset="utf-8">
${response.setHeader("Content-Type", "text/html")}

<#-- building 'sets' of sites and content types... doing it in variables and hashes to filter duplicates -->
<#assign statics=exchange.getProperty("beansWrapper").getStaticModels() />
<#assign typeList = {} />
<#assign siteList = {} />
<#list exchange.getProperty("catalog").getSourceInfo(exchange.getProperty("sourceInfoReqEnterprise")).getSourceInfo() as srcDesc>
	<#assign siteList = siteList + {srcDesc.getSourceId():srcDesc.isAvailable()} />

	<#if srcDesc.getContentTypes()??>
		<#list srcDesc.getContentTypes() as contentType>
			<#assign typeList = typeList + {contentType.getName():""} />
		</#list>
	</#if>
</#list>

<title><#if exchange.getProperty("branding")??>${exchange.getProperty("branding").getProductName()}</#if> Search</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="">
<meta name="author" content="">

<link href="/search/bootstrap/css/bootstrap.min.css" rel="stylesheet">
<link href="/search/font-awesome/css/font-awesome.min.css" rel="stylesheet">
<link href="/search/jquery/css/smoothness/jquery-ui-1.9.1.custom.css" rel="stylesheet">
<link href="/search/jquery/css/plugin/jquery-ui-timepicker-addon.css" rel="stylesheet">

<link href="/search/style.css" rel="stylesheet">


<style type="text/css">
	div.banner { 
		<#if exchange.getProperty("color")??  && exchange.getProperty("color")?length &gt; 0>color: ${exchange.getProperty("color")}; </#if>
		<#if exchange.getProperty("background")??  && exchange.getProperty("background")?length &gt; 0>background: ${exchange.getProperty("background")}; </#if>
	}
</style>


<!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
		<script src="/search/shim/html5.js"></script>
	<![endif]-->
<!--[if IE 7]>
  <link rel="stylesheet" href=/search/font-awesome/css/font-awesome-ie7.min.css">
<![endif]-->

</head>
<body>
	<div class="navbar navbar-inverse navbar-fixed-top">
		<#if exchange.getProperty("header")??  && exchange.getProperty("header")?length &gt; 0><div class="banner">${exchange.getProperty("header")}</div></#if>
		<div class="navbar-inner">
			<div class="container">
				<a class="brand" href="#"><i class="icon-globe icon-white"></i> <#if exchange.getProperty("branding")??>${exchange.getProperty("branding").getProductName()}</#if></a>
				<div class="nav-collapse collapse">
					<ul class="nav">
						<li class="active"><a href="#">Search</a></li>
					</ul>
					<ul class="nav pull-right">
						<li><a href="/search/SearchHelp.html<#if exchange.getProperty("branding")??>?title=${exchange.getProperty("branding").getProductName()}</#if>">Help</a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>

	<!-- Metacard Modal -->
	<div id="metacardModal" class="modal hide" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3>Record Metadata</h3>
		</div>
		<div class="modal-body">
		</div>
		<div class="modal-footer">
		</div>
	</div>
	
	<div class="main" class="container-fluid clear-top">
		<div class="row-fluid">

			<div class="span3">
				<!-- leave "action" off so that it gets posted to the URL it came from. -->
				<form id="searchForm" class="partialaffix span3 search-controls" method="get">
					<ul class="nav nav-list well well-small">
						<input type="hidden" name="format" value="querypage">
						<input type="hidden" name="start" value="1">

						<li class="nav-header">Keywords</li>
						<li>
							<div class="input-append">
								<input name="q" type="text" class="span12">
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Time</li>
						<li>
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" class="btn btn-mini active" name="noTemporalButton" data-target="#notemporal" data-toggle="tab">Any</button>
								<button type="button" class="btn btn-mini" name="relativeTimeButton" data-target="#time_relative" data-toggle="tab">Relative</button>
								<button type="button" class="btn btn-mini"  name="absoluteTimeButton" data-target="#time_absolute" data-toggle="tab">Absolute</button>
							</div>
							<div class="tab-content">
								<div id="notemporal" name="notemporal" class="tab-pane"></div>
								<div id="time_absolute" name="time_absolute" class="tab-pane">
									<input type="hidden" name="dtstart" value="">
									<input type="hidden" name="dtend" value="">
									
									<div class="input-prepend">
										<span class="add-on add-on-label">Begin <i class="icon-time"></i></span>
										<input id="absoluteStartTime" name="absoluteStartTime" type="text" class="span8"/>
									</div>
									<div class="input-prepend">
										<span class="add-on add-on-label">End <i class="icon-time"></i></span>
										<input id="absoluteEndTime" name="absoluteEndTime" type="text" class="span8"/>
									</div>
                                    <div class="alert alert-block span11" id="timeAbsoluteWarning">
                                    	Warning! If either value is unpopulated, the search will not use any temporal filters
                                    </div>

								</div>
								<div id="time_relative" name="time_relative" class="tab-pane">
									<div class="row-fluid">
										<input type="hidden" name="dtoffset" value="">
										<div class="span11 input-prepend input-append">
											<div class="span7">
												<span class="add-on add-on-label">Last</span>
												<input id="offsetTime" class="span7" name="offsetTime" type="number" onchange="updateOffset()"/>
											</div>
											<select id="offsetTimeUnits" class="add-on span3" name="offsetTimeUnits" onchange="updateOffset()">
												<option value="seconds">seconds</option>
												<option value="minutes" selected="selected">minutes</option>
												<option value="hours">hours</option>
												<option value="days">days</option>
												<option value="weeks">weeks</option>
												<option value="months">months</option>
												<option value="years">years</option>
											</select>
										</div>
	                                    <div class="alert alert-block span11" id="timeRelativeWarning">
	                                    	Warning! If the 'Last' duration is unpopulated, the search will not use any temporal filters
	                                    </div>
									</div>
								</div>
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Location</li>
						<li>
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" name="noLocationButton" class="btn btn-mini active" data-target="#nogeo" data-toggle="tab">Any</button>
								<button type="button" name="pointRadiusButton" class="btn btn-mini" data-target="#pointradius" data-toggle="tab">Point-Radius</button>
								<button type="button" name="bboxButton" class="btn btn-mini" data-target="#boundingbox" data-toggle="tab">Bounding Box</button>
<!--
								<button type="button" name="wktButton" class="btn btn-mini" data-target="#wkt" data-toggle="tab">WKT</button>
-->
							</div>
							<div class="tab-content">
								<div id="nogeo" class="tab-pane"></div>
								<div id="pointradius" class="tab-pane">
									<input type="hidden" name="radius" value="">
									<input type="hidden" name="lat" value="">
									<input type="hidden" name="lon" value="">

									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">Latitude<i class="icon-globe"></i></span>
										<input class="span7" id="latitude" name="latitude" type="number" min="-90" max="90" step="any" onchange="updatePointRadius()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">Longitude<i class="icon-globe"></i></span>
										<input class="span7" id="longitude" name="longitude" type="number" min="-180" max="180" step="any" onchange="updatePointRadius()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>

                                    <div class="span11 input-prepend input-append">
                                    	<div class="span7">
	                                        <span class="add-on add-on-label">Radius<i class="icon-plus"></i></span>
											<input class="span7" id="radiusValue" name="radiusValue" type="number" placeholder="" onchange="updatePointRadius()"/>
										</div>
										<select id="radiusUnits" class="add-on span4" name="radiusUnits" onchange="updatePointRadius()">
											<option value="meters" selected="selected">meters</option>
											<option value="kilometers">kilometers</option>
											<option value="feet">feet</option>
											<option value="yards">yards</option>
											<option value="miles">miles</option>
										</select>										
                                    </div>
                                    <div class="alert alert-block span11" id="pointRadiusWarning">
										Warning! If any Point-Radius value is unpopulated, the search will not use any location filters
									</div>                                    
								</div>

								<div id="boundingbox" class="tab-pane">
									<input type="hidden" name="bbox" value="">
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">West <i class="icon-globe"></i></span>
										<input class="span7" id="west" name="west" type="number" min="-180" max="180" step="any" onchange="updateBoundingBox()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">South <i class="icon-globe"></i></span>
										<input class="span7" id="south" name="south" type="number" min="-90" max="90" step="any" onchange="updateBoundingBox()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">East <i class="icon-globe"></i></span>
										<input class="span7" id="east" name="east" type="number" min="-180" max="180" step="any" onchange="updateBoundingBox()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">North <i class="icon-globe"></i></span>
										<input class="span7" id="north" name="north" type="number" min="-90" max="90" step="any" onchange="updateBoundingBox()" placeholder=""/>
										<label class="add-on">&deg;</label>		
									</div>
                                    <div class="alert alert-block span11" id="boundingBoxWarning">
										Warning! If any field is unpopulated, the search will not use any location filters
									</div>
								</div>

								<div id="wkt" class="tab-pane">
									<textarea rows="3" class="span12"></textarea>
								</div>
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Type</li>
						<li>
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" name="noTypeButton" class="btn btn-mini active" data-target="#noTypeTab" data-toggle="tab">Any</button>
								<button type="button" name="typeButton" class="btn btn-mini" data-target="#typeTab" data-toggle="tab">Specific Types</button>
							</div>
							<div class="tab-content">
								<div id="noTypeTab" class="tab-pane"></div>
								<div id="typeTab" class="tab-pane">
									<input type="hidden" name="type" value="">
									<select id="typeList" name="typeList" class="span12" onchange="updateType()">
										<#list typeList?keys as types>
											<option>${types}</option>
										</#list>
									</select>
								</div>
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Additional Sources</li>
						<li>
							<input type="hidden" name="src" value="">
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" class="btn btn-mini active" name="noFederationButton" data-target="#nofed" data-toggle="tab">None</button>
								<button type="button" class="btn btn-mini" name="enterpriseFederationButton" data-target="#nofed" data-toggle="tab">All Sources</button>
								<button type="button" class="btn btn-mini" name="selectedFederationButton" data-target="#sources" data-toggle="tab">Specific Sources</button>
							</div>
							<div class="tab-content">
								<div id="nofed" class="tab-pane"></div>
								<div id="sources" class="tab-pane">
									<select name="federationSources" multiple="multiple" onchange="updateFederation()" class="span12">
										<#list siteList?keys as site>
											<#if siteList[site] >
												<option title="${site}">${site}</option>
											<#else>
												<option disabled="disabled" class="disabled_option" title="${site}">${site}</option>
											</#if>
										</#list>
									</select>
									<div class="alert alert-block" id="federationListWarning">
										Warning! If no selections are made, the search will use 'All Sources'
									</div>
								</div>
							</div>
						</li>
						
						<li class="divider"></li>

						<li class="nav-header">Page Size</li>
						<li>
							<div class="input-prepend">
								<span class="add-on">Results per Page</span>
								<select id="count" class="span4" type="number" name="count">
									<option value="5">5</option>
									<option value="10" selected="selected">10</option>
									<option value="25">25</option>
									<option value="100">100</option>
									<option value="500">500</option>
									<option value="1000">1000</option>
								</select>
							</div>
						</li>
						
						<li>
							<div class="form-actions">
								<button class="btn btn-primary " type="submit">
									<i class="icon-search icon-white"></i> Search
								</button>
								<button class="btn " type="reset" onClick="resetForm()">Clear</button>
							</div>
						</li>
					</ul>
				</form>
			</div>
			<div class="span9">
				<div class="affix results-header row-fluid">
					<#assign url = headers.url>
					<#if (! url?contains("start=")) >
						<#assign url = headers.url + "&start=1">
					</#if>

					<#assign start = 1>  
					<#assign count = 10>
					<#list url?matches("[&?]([^=]+)=([^&]*)") as m>
						<#if m?groups[1]?matches("start")>
							<#attempt>
								<#assign start = m?groups[2]?number>
							<#recover>
								<#assign start = 1>  
							</#attempt>
						</#if>
						<#if m?groups[1]?matches("count")>
							<#attempt>
								<#assign count = m?groups[2]?number>
							<#recover>
								<#assign count = 10>
							</#attempt>
						</#if>
					</#list>
					<#assign hits = request.body.hits>
					<#assign currentPage = (start / count)?ceiling>
					<#assign maxPage = (hits / count)?ceiling>
					<#assign startString = "start=" + start?c>

					<div class="resultsCount pull-left span6" ><p class="lead">Total Results: ${hits} </p></div>

					<#assign startingPage = 1>
					<#assign endingPage = 1>
					
					<#if (maxPage > 1)>
						<#if (maxPage <= 4)>
							<#assign startingPage = 1>
							<#assign endingPage = maxPage>
						<#elseif (currentPage == 1)>
							<#assign startingPage = 1>
							<#assign endingPage = 4>
						<#elseif (currentPage > maxPage - 2)>
							<#assign startingPage = maxPage - 3>
							<#assign endingPage = maxPage>
						<#else>
							<#assign startingPage = currentPage - 1>
							<#assign endingPage = currentPage + 2>
						</#if>

						<div class="pagination pull-right span6">
							<ul>
								<#if (currentPage <= 1)>
									<li class="disabled"><a href="${url?replace(startString, "start=" + (1 + (count * (currentPage - 1)))?c)}">Prev</a></li>
								<#else>
									<li><a href="${url?replace(startString, "start=" + (1 + (count * (currentPage - 2)))?c)}"><abbr title="Previous">Prev</abbr></a></li>
								</#if>
								<#list startingPage..endingPage as pageNum>
									<li <#if pageNum==currentPage>class="active"</#if>><a href="${url?replace(startString, "start=" + (1 + (count * (pageNum - 1)))?c)}">${pageNum}</a></li>
								</#list>								
								<#if (currentPage >= maxPage)>
									<li class="disabled"><a href="${url?replace(startString, "start=" + (1 + (count * (currentPage - 1)))?c)}">Next</a></li>
								<#else>
									<li><a href="${url?replace(startString, "start=" + (1 + (count * currentPage))?c)}">Next</a></li>
								</#if>
							</ul>
						</div>
					</#if>
				</div>
				<div class="row-fluid results">
					<table class="table table-striped">
						<thead>
							<tr>
								<th>Title</th>
								<th>Source</th>
								<th>Location</th>
								<th>Time</th>
							</tr>
						</thead>
						<tbody>
							<#list request.body.results as result>
								<tr>
									<td>
										<#if (exchange.getProperty("htmlActionProviderList")?size > 0) &&
											 (exchange.getProperty("htmlActionProviderList")[0].getAction(result.metacard)??) &&
											 (exchange.getProperty("htmlActionProviderList")[0].getAction(result.metacard).getUrl()??) >
											<a class="metacard-modal" href="${exchange.getProperty("htmlActionProviderList")[0].getAction(result.metacard).getUrl()?string}">${result.metacard.title!"No Title"}</a>
										<#else>
											${result.metacard.title!"No Title"}
										</#if>
									</td>
									<td><#if result.metacard.sourceId??>${result.metacard.sourceId}</#if></td>
									<td>
										<#if result.metacard.location??>${result.metacard.location}</#if>
									</td>
									<td>
										<#if result.metacard.effectiveDate??>
											Effective: ${result.metacard.effectiveDate?string("yyyy-MM-dd HH:mm:ss zzz")}<br>
										</#if>
										<#if result.metacard.createdDate??>
										    Received: ${result.metacard.createdDate?string("yyyy-MM-dd HH:mm:ss zzz")}</td>
										</#if>
									<td>
										<#if result.metacard.thumbnail?? && result.metacard.thumbnail[0]??>												
											<img class="thumbnail" src="data:image/jpeg;charset=utf-8;base64, ${statics["javax.xml.bind.DatatypeConverter"].printBase64Binary(result.metacard.thumbnail)}" alt=""/>
										</#if>
									</td>
									<td>
										<#if result.metacard.resourceURI??>
											<#if (exchange.getProperty("resourceActionProviderList")?size > 0) &&
												(exchange.getProperty("resourceActionProviderList")[0].getAction(result.metacard)??) &&
												(exchange.getProperty("resourceActionProviderList")[0].getAction(result.metacard).getUrl()??) >
												<a href="${exchange.getProperty("resourceActionProviderList")[0].getAction(result.metacard).getUrl()?string}" target="_blank">
													<i class="icon-download-alt icon-2x"></i>
												</a><br>
												<#if result.metacard.resourceSize??>
													<div style="visibility: hidden;" class="resourceSize">${result.metacard.resourceSize}</div>
												</#if>
											</#if>
										</#if>
									</td>
								</tr>
							</#list>
							
						</tbody>
					</table>
				</div>
				<div class="row-fluid">
					<p class="pull-right">
						<a href="#">Back to top</a>
					</p><br>
				</div>
			</div>
		</div>
	</div>
	<#if exchange.getProperty("footer")?? && exchange.getProperty("footer")?length &gt; 0><div class="navbar-fixed-bottom banner">${exchange.getProperty("footer")}</div></#if>
	
	<!-- Placed at the end of the document so the pages load faster -->
	<script src="/search/jquery/js/jquery-1.8.2.min.js"></script>
	<script src="/search/jquery/js/jquery-ui-1.9.1.custom.min.js"></script>
	<script src="/search/jquery/js/plugin/purl.js"></script>
	<!-- need to override datePicker to fix 4digit year bug before timepicker extends it -->
	<script src="/search/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon.js"></script>
	<script src="/search/jquery/js/plugin/jquery-ui-timepicker-addon.js"></script>
	

	<script src="/search/bootstrap/js/bootstrap.min.js"></script>
	<script src="/search/bootstrap-extensions/js/partial-affix.min.js"></script>
	
	<script src="/search/searchPage.js"></script>
	

</body>
</html>
