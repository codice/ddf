<!DOCTYPE html>
<!-- 
/**
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

<html>
<head>
<meta charset="utf-8">




<title>DDF Search</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="">
<meta name="author" content="">

<link href="lib/bootstrap-2.3.1/css/bootstrap.min.css" rel="stylesheet">
<link href="lib/font-awesome/css/font-awesome.min.css" rel="stylesheet">

<link href="lib/jquery/css/smoothness/jquery-ui-1.9.1.custom.min.css" rel="stylesheet">
<link href="lib/jquery/css/plugin/jquery-ui-timepicker-addon.css" rel="stylesheet">

<!-- These CSS files have been compressed and aggregated into Search-min.css.  The list is here for easy modification for
     the sake of debugging.  
     TODO: Leverage something like http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/  for a better solution
 -->
<!--  
<link href="css/searchPage.css" rel="stylesheet">
<link href="css/recordView.css" rel="stylesheet">
-->
<link href="css/Search-min.css" rel="stylesheet">

<style media=screen type="text/css">
	.banner {
		color: <%=org.codice.ddf.ui.searchui.search.properties.ConfigurationStore.getInstance().getColor()%>;
		background: <%=org.codice.ddf.ui.searchui.search.properties.ConfigurationStore.getInstance().getBackground()%>;
	}
</style>


<!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
		<script src="lib/shim/html5.js"></script>
	<![endif]-->
<!--[if IE 7]>
  <link rel="stylesheet" href="lib/font-awesome/css/font-awesome-ie7.min.css">
<![endif]-->

</head>
<body>

	<div class="navbar navbar-inverse navbar-fixed-top">
		<% String h = org.codice.ddf.ui.searchui.search.properties.ConfigurationStore.getInstance().getHeader();
			if(h != null && h.trim().length() > 0)
			    out.println("<div class=\"banner\">" + h + "</div>");
		 %>
	
		<div class="navbar-inner">
			<div class="container">
				<a class="brand" href="#"><i class="icon-globe icon-white"></i>DDF</a>
				<div class="nav-collapse collapse">
					<ul class="nav">
						<li class="active"><a href="#">Search</a></li>
					</ul>
					<ul class="nav pull-right">
						<li><a
							href="SearchHelp.html?title=DDF">Help</a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>

	<!-- Metacard Modal -->
	<div id="metacardModal" class="modal hide" tabindex="-1" role="dialog"
		aria-labelledby="myModalLabel" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
			<h3>title</h3>
		</div>
		<div class="modal-body"></div>
	</div>

	<div class="main" class="container-fluid clear-top">
		<div class="row-fluid">


			<div class="span3">
				<form id="searchForm" class="partialaffix span3 search-controls"
					action="/services/catalog/query" method="get">
					<ul class="nav nav-list well well-small">
						<input type="hidden" name="format" value="geojson"></input>
						<input type="hidden" name="start" value="1"></input>


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
								<button type="button" class="btn btn-mini active"
									name="noTemporalButton" data-target="#notemporal"
									data-toggle="tab">Any</button>
								<button type="button" class="btn btn-mini"
									name="relativeTimeButton" data-target="#time_relative"
									data-toggle="tab">Relative</button>
								<button type="button" class="btn btn-mini"
									name="absoluteTimeButton" data-target="#time_absolute"
									data-toggle="tab">Absolute</button>
							</div>
							<div class="tab-content">
								<div id="notemporal" name="notemporal" class="tab-pane"></div>
								<div id="time_absolute" name="time_absolute" class="tab-pane">
									<input type="hidden" name="dtstart" value=""> <input
										type="hidden" name="dtend" value="">

									<div class="input-prepend">
										<span class="add-on add-on-label">Begin <i
											class="icon-time"></i></span> <input id="absoluteStartTime"
											name="absoluteStartTime" type="text" class="span8" />
									</div>
									<div class="input-prepend">
										<span class="add-on add-on-label">End <i
											class="icon-time"></i></span> <input id="absoluteEndTime"
											name="absoluteEndTime" type="text" class="span8" />
									</div>
									<div class="alert alert-block span11" id="timeAbsoluteWarning">
										Warning! If either value is unpopulated, the search will not
										use any temporal filters</div>

								</div>
								<div id="time_relative" name="time_relative" class="tab-pane">
									<div class="row-fluid">
										<input type="hidden" name="dtoffset" value="">
										<div class="span11 input-prepend input-append">
											<div class="span7">
												<span class="add-on add-on-label">Last</span> <input
													id="offsetTime" class="span5" name="offsetTime"
													type="number" onchange="updateOffset()" />
											</div>
											<select id="offsetTimeUnits" class="add-on span3"
												name="offsetTimeUnits" onchange="updateOffset()">
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
											Warning! If the 'Last' duration is unpopulated, the search
											will not use any temporal filters</div>
									</div>
								</div>
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Location</li>
						<li>
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" name="noLocationButton"
									class="btn btn-mini active" data-target="#nogeo"
									data-toggle="tab">Any</button>
								<button type="button" name="pointRadiusButton"
									class="btn btn-mini" data-target="#pointradius"
									data-toggle="tab">Point-Radius</button>
								<button type="button" name="bboxButton" class="btn btn-mini"
									data-target="#boundingbox" data-toggle="tab">Bounding
									Box</button>
								<!--
								<button type="button" name="wktButton" class="btn btn-mini" data-target="#wkt" data-toggle="tab">WKT</button>
-->
							</div>
							<div class="tab-content">
								<div id="nogeo" class="tab-pane"></div>
								<div id="pointradius" class="tab-pane">
									<input type="hidden" name="radius" value=""> <input
										type="hidden" name="lat" value=""> <input
										type="hidden" name="lon" value="">

									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">Latitude<i
											class="icon-globe"></i></span> <input class="span7" id="latitude"
											name="latitude" type="number" min="-90" max="90" step="any"
											onchange="updatePointRadius()" placeholder="" /> <label
											class="add-on">&deg;</label>
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">Longitude<i
											class="icon-globe"></i></span> <input class="span7" id="longitude"
											name="longitude" type="number" min="-180" max="180"
											step="any" onchange="updatePointRadius()" placeholder="" />
										<label class="add-on">&deg;</label>
									</div>

									<div class="span11 input-prepend input-append">
										<div class="span7">
											<span class="add-on add-on-label">Radius<i
												class="icon-plus"></i></span> <input class="span5" id="radiusValue"
												name="radiusValue" type="number" placeholder=""
												onchange="updatePointRadius()" />
										</div>
										<select id="radiusUnits" class="add-on span4"
											name="radiusUnits" onchange="updatePointRadius()">
											<option value="meters" selected="selected">meters</option>
											<option value="kilometers">kilometers</option>
											<option value="feet">feet</option>
											<option value="yards">yards</option>
											<option value="miles">miles</option>
										</select>
									</div>
									<div class="alert alert-block span11" id="pointRadiusWarning">
										Warning! If any Point-Radius value is unpopulated, the search
										will not use any location filters</div>
								</div>

								<div id="boundingbox" class="tab-pane">
									<input type="hidden" name="bbox" value="">
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">West <i
											class="icon-globe"></i></span> <input class="span7" id="west"
											name="west" type="number" min="-180" max="180" step="any"
											onchange="updateBoundingBox()" placeholder="" /> <label
											class="add-on">&deg;</label>
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">South <i
											class="icon-globe"></i></span> <input class="span7" id="south"
											name="south" type="number" min="-90" max="90" step="any"
											onchange="updateBoundingBox()" placeholder="" /> <label
											class="add-on">&deg;</label>
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">East <i
											class="icon-globe"></i></span> <input class="span7" id="east"
											name="east" type="number" min="-180" max="180" step="any"
											onchange="updateBoundingBox()" placeholder="" /> <label
											class="add-on">&deg;</label>
									</div>
									<div class="span11 input-prepend input-append">
										<span class="add-on add-on-label">North <i
											class="icon-globe"></i></span> <input class="span7" id="north"
											name="north" type="number" min="-90" max="90" step="any"
											onchange="updateBoundingBox()" placeholder="" /> <label
											class="add-on">&deg;</label>
									</div>
									<div class="alert alert-block span11" id="boundingBoxWarning">
										Warning! If any field is unpopulated, the search will not use
										any location filters</div>
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
								<button type="button" name="noTypeButton"
									class="btn btn-mini active" data-target="#noTypeTab"
									data-toggle="tab">Any</button>
								<button type="button" name="typeButton" class="btn btn-mini"
									data-target="#typeTab" data-toggle="tab">Specific
									Types</button>
							</div>
							<div class="tab-content">
								<div id="noTypeTab" class="tab-pane"></div>
								<div id="typeTab" class="tab-pane">
									<input type="hidden" name="type" value=""> <select
										id="typeList" name="typeList" class="span12"
										onchange="updateType()">
									</select>
								</div>
							</div>
						</li>

						<li class="divider"></li>

						<li class="nav-header">Additional Sources</li>
						<li><input type="hidden" name="src" value="">
							<div class="btn-group" data-toggle="buttons-radio">
								<button type="button" class="btn btn-mini active"
									name="noFederationButton" data-target="#nofed"
									data-toggle="tab">None</button>
								<button type="button" class="btn btn-mini"
									name="enterpriseFederationButton" data-target="#nofed"
									data-toggle="tab">All Sources</button>
								<button type="button" class="btn btn-mini"
									name="selectedFederationButton" data-target="#sources"
									data-toggle="tab">Specific Sources</button>
							</div>
							<div class="tab-content">
								<div id="nofed" class="tab-pane"></div>
								<div id="sources" class="tab-pane">
									<div id="scrollableSources" class="scrollable">
										<select id="federationSources" multiple="multiple"
											onchange="updateFederation()">
										</select>
									</div>
									<div class="alert alert-block" id="federationListWarning">
										Warning! If no selections are made, the search will use 'All
										Sources'</div>
								</div>
							</div></li>

						<li class="divider"></li>

						<li class="nav-header">Page Size</li>
						<li>
							<div class="input-prepend">
								<span class="add-on">Results per Page</span> <select id="count"
									class="span4" type="number" name="count">
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
			<div id="contentView" class="span9">
				<div id="recordView" class="record-view" style="display: none;">
					<div id="recordViewHeader" class='affix results-header row-fluid'>
						<div class='pagination pull-left span6'>
							<ul>
								<li id="backToResultsBtn"><a>Back to Results</a></li>
							</ul>
						</div>
						<div class='pagination pull-right span6'>
							<ul>
								<li id="previousRecordLi"><a>Previous</a></li>
								<li id="nextRecordLi"><a>Next</a></li>
							</ul>
						</div>
					</div>
					
					<!--  this div contains the 'record view.'  Eventually we want to
							make this re-usable somehow. -->
					<div id="recordContentDiv">
						<div class="panel-primary">
							<div class="panel-heading"><h2></h2></div>
							<div id=tablePanel class="panel-info">
								<table class="table-striped record-table">
									<thead class="custom-thead">
										<th class="table-header">Property</th>
										<th class="table-header">Value</th>
									</thead>
									<tbody>
									</tbody>
								</table>
							</div>
						</div>
					</div>
					<!-- End 'record view' area -->
	
				</div>
				<div id="loadingView" class="msgView">
					<div class="msgViewContainer">
						<div class="msgViewContents">
							<p>
								<img src="images/ajax-loader.gif" /> Loading ...
							</p>
						</div>
					</div>
				</div>
				<div id="errorView" class="msgView">
					<div class="msgViewContainer">
						<div class="msgViewContents">
							<p>
							<span class="icon-exclamation icon-2x"></span> <strong>Error!</strong>
							<button type="btn" onClick="hideError()">&times; Close</button>
							<div id="errorText"></div>
						</div>
					</div>
				</div>
					<div id="resultsView">
						<div class="affix results-header row-fluid">

							<div class="resultsCount pull-left span6">
								<p id="countTotal" class="lead">Total Results: 0</p>
							</div>
							<div class="pagination pull-right span6">
								<ul id="pages">
								</ul>
							</div>
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
							<tbody id="resultsList">
							</tbody>
						</table>
					</div>
					<div class="row-fluid">
						<p class="pull-right">
							<a href="#">Back to top</a>
						</p>
						<br>
					</div>
				</div>
			</div>
		</div>
	</div>

	<% String f = org.codice.ddf.ui.searchui.search.properties.ConfigurationStore.getInstance().getFooter();
		if(f != null && f.trim().length() > 0)
		    out.println("<div class=\"navbar-fixed-bottom banner\">" + f + "</div>");
	 %>
		
	<!-- Placed at the end of the document so the pages load faster -->

	<script type="text/javascript" src="lib/jquery/js/jquery-1.8.2.min.js"></script>
	<script type="text/javascript" src="lib/jquery/js/jquery-ui-1.9.1.custom.min.js"></script>

	
	<script type="text/javascript" src="lib/bootstrap-2.3.1/js/bootstrap.min.js"></script>
	<script type="text/javascript" src="lib/bootstrap-extensions/js/partial-affix.js"></script>
	<script type="text/javascript" src="lib/jquery/js/plugin/purl.js"></script>
	<script type="text/javascript" src="lib/jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon.js"></script>
	<script type="text/javascript" src="lib/jquery/js/plugin/jquery-ui-timepicker-addon.js"></script>

<!-- These scripts have been compressed and aggregated into Search-min.js.  The list is here for easy modification for
     the sake of debugging.  
     TODO: Leverage something like http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/  for a better solution
 -->
<!--
	<script type="text/javascript" src="js/searchMessagingDirect.js"></script>
	<script type="text/javascript" src="js/recordView.js"></script>
	<script type="text/javascript" src="js/metadataHelper.js"></script>
	<script type="text/javascript" src="js/viewSwitcher.js"></script>
	<script type="text/javascript" src="js/searchPage.js"></script>
 -->

 	<script type="text/javascript" src="js/Search-min.js"></script>

</body>
</html>
