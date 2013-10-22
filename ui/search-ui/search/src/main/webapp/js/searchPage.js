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

var resultsMapping = null;
var maxResults = 0;

$('.search-controls').partialaffix({
	offset: {
		top: 63,
		bottom: 30
	}
});

$("#contentView").on("click", ".tree-toggle", function () {
	$(this).parent().children('ul.tree').toggle(200);
});

$("#errorView").hide();
$("#loadingView").hide();

function getServicesUrl() {
	return "";
}

$.ajax(
	{
		url: getServicesUrl() + "/services/catalog/sources",
		dataType: "jsonp"
	}).done(function(data){
		var sources, types, type, to, i, j, id, o;
		sources = data;
		types = {};
		
		for(i = 0; i < sources.length; i++) {
			id = sources[i].id;
			o = new Option(id, id);
			$(o).html(id);
			if(! sources[i].available) {
				$(o).attr("disabled", "disabled");
				$(o).attr("class", "disabled_option");
			}
			$("#federationSources").append(o);
			
			for(j = 0; j < sources[i].contentTypes.length; j++) {
				types[sources[i].contentTypes[j].name]=true;
			}
		}

		for(type in types) {
			to = new Option(type, type);
			$(to).html(type);
			$("#typeList").append(to);
		}
	});
	
	
var pendingRequest = null;
function cancelRequest() {
	if(pendingRequest) {
		pendingRequest.abort();
		pendingRequest = null;
	}
}

var viewSwitcher = null;

function getViewSwitcher() {
	if(! viewSwitcher) {
		viewSwitcher = new ViewSwitcher("resultsView","recordView");
	}
	return viewSwitcher;
}

function showLoading() {
	$("#resultsView").hide();
    $("#loadingView").show();
}

function showError(msg) {
    $("#loadingView").hide();
	$("#resultsView").hide();
    $("#errorText").html(msg);
    $("#errorView").show();
}

function hideError(msg) {
    $("#errorView").hide();
	getViewSwitcher().showResultsView();
}

function getMaxResults() {
	return maxResults;
}

function getItemsPerPage() {
	return parseInt($('select[name=count]').val(), 10);
}

function getPageStartIndex(index) {
	return 1 + (getItemsPerPage() * Math.floor((index - 1)/ getItemsPerPage()));
}

function getPageEndIndex(index) {
	var endIndex = getPageStartIndex(index) + getItemsPerPage() - 1;
	if(endIndex > getMaxResults()) {
		endIndex = getMaxResults();
	}
	return endIndex;
}

function haveResultsPageForItem(index) {
	var startIndex, endIndex, i;
	if(! resultsMapping[index]) {
		return false;
	}
	startIndex = getPageStartIndex(index);
	endIndex = getPageEndIndex(index);
	for(i = startIndex; i <= endIndex; i++) {
		if(! resultsMapping[i]) {
			return false;
		}
	}
	return true;
}

function setCountTotal(hits) {
	maxResults = hits;
	$("#countTotal").text("Total Results: " + hits);
}

function setUpPaging(hits, start, pageSize) {
	var pages, numOfPages, currentPage, startPage, endPage, p, navTo, javascript;
	javascript = "javascript";
	pages = $("#pages");
	pages.empty();
	if(hits > pageSize) {
		numOfPages = Math.ceil(hits / pageSize);
		currentPage = Math.ceil((start - 1) / pageSize) + 1;
		
		startPage = 1;
		endPage = 1;

		if(numOfPages <= 4) {
			endPage = numOfPages;
		} else if(currentPage === 1) {
			endPage = 4;
		} else if(currentPage > numOfPages - 2) {
			startPage = numOfPages - 3;
			endPage = numOfPages;
		} else {
			startPage = currentPage - 1;
			endPage = currentPage + 2;
		}
		
		if(startPage === currentPage) {
			$("<li class='disabled'><a>Prev</a></li>").appendTo(pages);
		} else {
			$("<li><a href='javascript:getResults(" + ((start - pageSize) > 1 ? (start - pageSize) : 1) + ")'>Prev</a></li>").appendTo(pages);
		}
		for(p = startPage; p <= endPage; p++) {
			navTo = start + (pageSize * (p - currentPage));
			if(navTo < 1) {
				navTo = 1;
			}
			$("<li" + (p === currentPage ? " class='active'" : "") + "><a href='" + javascript + ":getResults(" + navTo + ")'>" + p + "</a></li>").appendTo(pages);
		}
		if(endPage === currentPage) {
			$("<li class='disabled'><a>Next</a></li>").appendTo(pages);
		} else {
		$("<li><a href='javascript:getResults(" + (start + pageSize) + ")'>Next</a></li>").appendTo(pages);
		}
	}
}

function assignResults(results, startIndex) {
	var i, index;
	for(i = 0; i < results.length; i++) {
		index = startIndex + i;
		resultsMapping[index] = results[i].metacard;
	}
}

function getCoordinates(coordinates) {
	var val, i;
	if($.isArray(coordinates[0])) {
		val = "(" + getCoordinates(coordinates[0]);
		
		for(i = 1; i < coordinates.length; i++) {
			val += ", " + getCoordinates(coordinates[i]);
		}
		val += ")";
		return val;
	} else {
		return coordinates.join(" ");
	}
}

function getGeometry(geometry) {
	try {
		if(geometry.type.toUpperCase() === "GEOMETRYCOLLECTION") {
			return getGeometry(geometry.geometries[0]);
		}
		return geometry.type + "(" + getCoordinates(geometry.coordinates) + ")";
	} catch(err) {
		return "";
	}	
}

function getDate(props) {
	var date, obj;
	date = $("<td>");
	obj = date;
	try {
		if(props.effective) {
			obj.text("Effective: " + props.effective);
			obj.append("<br>");
		}
		if(props.created) {
			obj.append("Received: " + props.created);
		}
	} catch(err) {
	}
	return date;
}

function formatDate(dateStr) {
	var date = $.datepicker.parseDate($.datepicker.ATOM, dateStr);
	return date.toString();
}

function createRow(index) {
	var metacard, props, row, title, link, source,location, date, thumbnail,
			product, productLink, productIcon, thumbnailLink, javascript;
	
	javascript = "javascript";
	metacard = resultsMapping[index];
	props = metacard.properties;
	row = $("<tr>");
	title = $("<td>");
	link = $("<a>");
	title.append(link);
	link.attr("class", "metacard-modal");
	link.attr("href", javascript + ":sendMetacard(" + index + ")");
	link.text(props.title);

	source = $("<td>");
	source.text(props["source-id"]);

	location = $("<td>");
	location.text(getGeometry(metacard.geometry));

	date = getDate(props);
	
	thumbnail = $("<td>");
	if(props.thumbnail) {
		thumbnailLink = $("<img>");
		thumbnail.append(thumbnailLink);
		thumbnailLink.attr("class", "thumbnail");
		thumbnailLink.attr("alt", "");
		thumbnailLink.attr("src", "data:image/jpeg;charset=utf-8;base64, " + props.thumbnail);
	}

	product = $("<td>");
	productLink = $("<a>");
	product.append(productLink);
	productLink.attr("target", "_blank"); 
	productLink.attr("href", getServicesUrl() + "/services/catalog/sources/" + props["source-id"] + "/" + props.id + "?transform=resource");
	productIcon = $("<i>");
	productIcon.attr("class", "icon-download-alt icon-2x");
	productLink.append(productIcon);
	product.append(productLink);
	if(props["resource-size"]){
		product.append("<div style=\"visibility: hidden;\" class=\"resourceSize\">" + props["resource-size"] + "</div>");
	}
	row.append(title);
	row.append(source);
	row.append(location);
	row.append(date);
	row.append(thumbnail);
	row.append(product);
	
	return row;
}

//return a formatted (human readable) string for the resource size in bytes
function formatResourceSize(resourceSize) {
      SizePrefixes = ' KMGTPEZYXWVU';

      if (resourceSize.toLowerCase().indexOf("n/a") >= 0) {
              return '';
      }

      // if the size is not a number, and it isn't 'n/a', assume it is
      // already formatted, ie "10 MB"
      if (isNaN(resourceSize)) {
              return resourceSize;
      }

      if (resourceSize <= 0) {
              return '0';
      }
      var t2 = Math.min(Math.round(Math.log(resourceSize) / Math.log(1024)), 12);
      return (Math.round(resourceSize * 100 / Math.pow(1024, t2)) / 100)
                      + ' ' + SizePrefixes.charAt(t2).replace(' ', '') + 'B';
}

function showResourceSizes(){
	//format the resource size for display and make it visible
	divs = $('.resourceSize');
	var i;
	for ( i = 0; i < divs.length; i++) {
	        formattedSize = formatResourceSize(divs[i].innerHTML);
	        divs[i].innerHTML = formattedSize;
	        divs[i].style.visibility = "visible";
	}
}


function buildRows(startAt, finishAt) {
	var rows, i;
	rows = $("#resultsList");
	rows.empty();
	for(i = startAt; i <= finishAt; i++) {
		createRow(i).appendTo(rows);
	}
	showResourceSizes();
}

function loadPageForItem(index) {
	if(resultsMapping[index]) {
		setCountTotal(getMaxResults());
		setUpPaging(getMaxResults(), getPageStartIndex(index), getItemsPerPage());
		buildRows(getPageStartIndex(index), getPageEndIndex(index));
	}
}

function getResults(startVal, showList) {
	if(haveResultsPageForItem(startVal)) {
		loadPageForItem(startVal);
		return;
	}	
	$('input[name=start]').val(getPageStartIndex(startVal));

	showLoading();

	//Do the AJAX post
	pendingRequest = $.ajax({
		url:  getServicesUrl() + $("#searchForm").attr("action"),
		data: $("#searchForm").serialize(),
		dataType: "jsonp",
		timeout:300000
	}).done(function(results){
		results.itemsPerPage = getItemsPerPage();
		results.start = getPageStartIndex(startVal);
		if(showList) {
			results.showList = true;
		}
		sendResults(results);
	}).fail(function() { 
		showError("Failed to get results from server");
	}).always(function() {
		pendingRequest = null;
	});
}

$("#searchForm").submit(function () {
	resultsMapping = {};
	maxResults = 0;
	getResults(1, true);
	return false;
});

function getMetacard(index) {
	if(! resultsMapping[index]) {
		getResults(index);
	}
	return resultsMapping[index];
}

function showMetacard(index) {
	getViewSwitcher().setCurrentIndex(index);	
	getViewSwitcher().showRecordView(index);
}

function hideLoading(startVal, showResults) {
	$("#loadingView").hide();
	if(showResults || getViewSwitcher().getCurrentView() === getViewSwitcher().RESULTS_VIEW) {
		getViewSwitcher().showResultsView();
	} else {
		showMetacard(getViewSwitcher().getCurrentIndex());		
	}
}

function showResults(results) {
	setCountTotal(results.hits);
	setUpPaging(results.hits, results.start, results.itemsPerPage);
	assignResults(results.results, results.start);
	buildRows(results.start, results.start + results.results.length - 1);

	hideLoading(results.start, results.showList);	
}

function updateFederationWarning(src) {
	if(src) {
		$('#federationListWarning').hide();
	} else {
		$('#federationListWarning').show();		
	}	
}

function restoreFederationSelections(src) {	
	if(src) {
		var sources = src.split(",");
	
		$.each(sources, function() {
			$("select[name=federationSources] option:contains('" + this + "')").attr("selected", "selected");
		});
	}
	updateFederationWarning(src);
}

function updateFederation() {
	var src = $("select[id=federationSources] :selected").map(function(){ return this.value; }).get().join(",");
	$('input[name=src]').val(src);
	updateFederationWarning(src);
}

function restoreTypeSelection(type) {	
	$("select[name=typeList] option:contains('" + type + "')").attr("selected", "selected");
}

function restoreCountSelection(count) {	
	$("select[name=count] option").filter(function() {return $(this).text() === count;}).attr("selected", "selected");
}

function updateType() {
	$('input[name=type]').val($('select[name=typeList]').val());
}

function clearType() {
	$('input[name=type]').val("");
}

function getPositiveIntValue(offset) {
	var offsetValue, offsetIntValue;
	offsetValue = Number(offset);
	offsetIntValue = Math.floor(offsetValue);
	
	if(offsetIntValue > 0) { 
		return offsetIntValue;
	} else { 
		return "";				
	} 
}


function isNonNegativeInteger(value) {
	var val, intVal;
	if(isNaN(value)) {
		return false;
	}
	val = Number(value);
	intVal = Math.round(val);
	return val === intVal && intVal >= 0;
}


function validateNumberInRange(min, max, value, revertValue, revertIfOutOfRange) {	
	var newValue = value;
	if(! value) {
		newValue = "";
	} else if(isNaN(value)) {
		newValue = revertValue;
	} else if(!isNaN(min) && Number(min) > Number(value)) {
		if(revertIfOutOfRange) {
			newValue = revertValue;
		} else {
			newValue = Number(min);	
		}
	} else if(!isNaN(max) && Number(max) < Number(value)) {
		if(revertIfOutOfRange) {
			newValue = revertValue;			
		} else {
			newValue = Number(max);
		}
	} 
	
	return newValue;
}

function validatePositiveInteger(posIntElement, revertValue) {
	var val = validateNumberInRange(0, Number.MAX_VALUE, $.trim(posIntElement.val()), revertValue, true);
	if(Number(val) === 0) {
		val = "";
	}
	val = getPositiveIntValue(val);
	posIntElement.val(val);
	return val;
}

function validateNumber(numberElement, revertValue) {
	var val = validateNumberInRange(numberElement.attr("min"), numberElement.attr("max"), $.trim(numberElement.val()), revertValue);
	numberElement.val(val);
	return val;
}

function getTimeInMillis(offset, units) {
	var now, timeInMillis;
	switch(units) {
		case "milliseconds":
			return offset;
		case "seconds":
			return offset * 1000;
		case "minutes":
			return offset * 60000;
		case "hours":
			return offset * 3600000;
		case "days":
			return offset * 86400000;
		case "weeks":
			return offset * 604800000;
		case "months": // not decimal friendly
			now = new Date();
			timeInMillis = now.valueOf();
			now.setMonth(now.getMonth() - offset);
			return (timeInMillis - now.valueOf());
		case "years": // not decimal friendly
			now = new Date();
			timeInMillis = now.valueOf();
			now.setFullYear(now.getFullYear() - offset);
			return (timeInMillis - now.valueOf());
		default:
			return offset * 60000;
	}
}

function clearOffset() {
	$('input[name=dtoffset]').val("");
}

function clearAbsoluteTime() {
	$('input[name=dtstart]').val("");
	$('input[name=dtend]').val("");
}

function updateAbsoluteTime() {
	var start, end;
	
	start = $('input[name=absoluteStartTime]').datepicker( "getDate" );
	end = $('input[name=absoluteEndTime]').datepicker( "getDate" );

	if(start && end) {
		if(start > end) {
			$('input[name=absoluteStartTime]').datepicker( "setDate", end);
			$('input[name=absoluteEndTime]').datepicker( "setDate", start);
		}
		
		$('input[name=dtstart]').val($('input[name=absoluteStartTime]').val());
		$('input[name=dtend]').val($('input[name=absoluteEndTime]').val());
		
		$('#timeAbsoluteWarning').hide();
	} else {
		$('#timeAbsoluteWarning').show();
		clearAbsoluteTime();
	}
}

function getDistanceInMeters(distance, units) {

	switch(units) {
		case "meters":
			return distance;
		case "kilometers":
			return distance * 1000;
		case "feet":
			return Math.ceil(distance * 0.3048);
		case "yards":
			return Math.ceil(distance * 0.9144);
		case "miles":
			return Math.ceil(distance * 1609.34);
		default:
			return distance;
	}
}

validateAbsoluteDates = function(dateText, instance) {
	updateAbsoluteTime();
};

$('#absoluteStartTime').datetimepicker({
	dateFormat: $.datepicker.ATOM,
	timeFormat: "HH:mm:ss.lz",
	separator: "T",
	timezoneIso8601: true,
	useLocalTimezone: true,
	showHour: false,
	showMinute: false,
	showSecond: false,
	showMillisec: false,
	showTimezone: false,
	minDate: new Date(100, 0, 2),
	maxDate: new Date(9999, 11, 30),
	onClose: validateAbsoluteDates
});

$('#absoluteEndTime').datetimepicker({
	dateFormat: $.datepicker.ATOM,
	timeFormat: "HH:mm:ss.lz",
	separator: "T",
	timezoneIso8601: true,
	useLocalTimezone: true,
	showHour: false,
	showMinute: false,
	showSecond: false,
	showMillisec: false,
	showTimezone: false,
	minDate: new Date(100, 0, 2),
	maxDate: new Date(9999, 11, 30),
	onClose: validateAbsoluteDates
});

function clearPointRadius() {
	$('input[name=lat]').val("");
	$('input[name=lon]').val("");	
	$('input[name=radius]').val("");				
}

function clearBoundingBox() {
	$('input[name=bbox]').val("");
}

//initialization logic to load from url
var urlVals = $.url();

var pointRadiusLatitude = validateNumberInRange(-90, 90, urlVals.param('latitude'), "");
$('input[name=latitude]').val(pointRadiusLatitude);
var pointRadiusLongitude = validateNumberInRange(-180, 180, urlVals.param('longitude'), "");
$('input[name=longitude]').val(pointRadiusLongitude);
var radiusValue = getPositiveIntValue(urlVals.param('radiusValue'));
$('input[name=radiusValue]').val(radiusValue);
if(urlVals.param('radiusUnits')) {
	$('select[name=radiusUnits]').val(urlVals.param('radiusUnits'));
}	

var relOffsetEntry = getPositiveIntValue(urlVals.param('offsetTime'));
$('input[name=offsetTime]').val(relOffsetEntry);
			

function updateOffset() {
	relOffsetEntry = validatePositiveInteger($('input[name=offsetTime]'), relOffsetEntry);
	if(relOffsetEntry) {
		$('input[name=dtoffset]').val(getTimeInMillis(relOffsetEntry, $('select[name=offsetTimeUnits]').val()));
		$('#timeRelativeWarning').hide();
	} else {
		clearOffset();
		$('#timeRelativeWarning').show();
	}
}

function updatePointRadius() {
	pointRadiusLatitude = validateNumber($('input[name=latitude]'), pointRadiusLatitude);
	pointRadiusLongitude = validateNumber($('input[name=longitude]'), pointRadiusLongitude);
	radiusValue = validatePositiveInteger($('input[name=radiusValue]'), radiusValue);
	
	if(pointRadiusLatitude && pointRadiusLongitude && radiusValue) {
		$('input[name=lat]').val(pointRadiusLatitude);
		$('input[name=lon]').val(pointRadiusLongitude);	
		$('input[name=radius]').val(getDistanceInMeters(radiusValue, $('select[name=radiusUnits]').val()));
		$('#pointRadiusWarning').hide();
	} else {
		clearPointRadius();
		$('#pointRadiusWarning').show();
	}	
}

var bboxWest =  validateNumberInRange(-180, 180, urlVals.param('west'), "");
$('input[name=west]').val(bboxWest);
var bboxSouth =  validateNumberInRange(-90, 90, urlVals.param('south'), "");
$('input[name=south]').val(bboxSouth);
var bboxEast =  validateNumberInRange(-180, 180, urlVals.param('east'), "");
$('input[name=east]').val(bboxEast);
var bboxNorth =  validateNumberInRange(-90, 90, urlVals.param('north'), "");
$('input[name=north]').val(bboxNorth);

function updateBoundingBox() {
	bboxWest= validateNumber($('input[name=west]'), bboxWest);
	bboxSouth = validateNumber($('input[name=south]'), bboxSouth);
	bboxEast= validateNumber($('input[name=east]'), bboxEast);
	bboxNorth= validateNumber($('input[name=north]'), bboxNorth);
	
	if(bboxNorth && bboxSouth && Number(bboxSouth) > Number(bboxNorth)) {
		var tmp = bboxSouth;
		bboxSouth = bboxNorth;
		bboxNorth = tmp;
		
		$('input[name=north]').val(bboxNorth);
		$('input[name=south]').val(bboxSouth);		
	}
	
	if(bboxWest && bboxSouth && bboxEast && bboxNorth) {
		$('input[name=bbox]').val(bboxWest + "," + bboxSouth + "," + bboxEast + "," + bboxNorth);
		$('#boundingBoxWarning').hide();
	} else {
		clearBoundingBox();
		$('#boundingBoxWarning').show();
	}
}

$('input[name=q]').val(urlVals.param('q'));


if(urlVals.param('offsetTimeUnits')) {
	$('select[name=offsetTimeUnits]').val(urlVals.param('offsetTimeUnits'));
}	

$('input[name=absoluteStartTime]').val(urlVals.param('absoluteStartTime'));
$('input[name=absoluteEndTime]').val(urlVals.param('absoluteEndTime'));

if(urlVals.param('dtoffset')) {
	$('input[name=dtoffset]').val(urlVals.param('dtoffset'));
	$('button[name=relativeTimeButton]').click();
	updateOffset();	
} else if(urlVals.param('dtstart') && urlVals.param('dtend')) {
	$('input[name=dtstart]').val(urlVals.param('dtstart'));
	$('input[name=dtend]').val(urlVals.param('dtend'));
	$('button[name=absoluteTimeButton]').click();
	updateAbsoluteTime();
}



if(urlVals.param('radius')) {
	$('button[name=pointRadiusButton]').click();		
	updatePointRadius();
//} else if(urlVals.param('geometry')) {
//	$('button[name=wktButton]').click();		
} else if(urlVals.param('bbox')) {
	$('button[name=bboxButton]').click();		
	updateBoundingBox();
} else {
	$('button[name=noLocationButton]').click();
}

var src = urlVals.param('src');
if(!src && src !== "") {
	src = "local";
}
$('input[name=src]').val(src);
if(!src) {
	$('button[name=enterpriseFederationButton]').click();
} else if(src === "local") {
	$('button[name=noFederationButton]').click();		
} else {
	$('button[name=selectedFederationButton]').click();
	restoreFederationSelections(src);
}

var type= urlVals.param('type');
$('input[name=type]').val(type);
restoreTypeSelection(urlVals.param('typeList'));
if(type) {
	$('button[name=typeButton]').click();
} else {
	$('button[name=noTypeButton]').click();		
}

restoreCountSelection(urlVals.param('count'));

// end of initialization logic

$('button[name=noTemporalButton]').on('click', function (e) {
	clearOffset();
	clearAbsoluteTime();
});

$('button[name=relativeTimeButton]').on('click', function (e) {
	updateOffset();
	clearAbsoluteTime();
});

$('button[name=absoluteTimeButton]').on('click', function (e) {
	updateAbsoluteTime();
	clearOffset();
});
		
$('button[name=noLocationButton]').on('click', function (e) {
	clearPointRadius();
	clearBoundingBox();
});

$('button[name=pointRadiusButton]').on('click', function (e) {
	updatePointRadius();
	clearBoundingBox();
});

$('button[name=bboxButton]').on('click', function (e) {
	clearPointRadius();
	updateBoundingBox();
});

//$('button[name=wktButton]').on('click', function (e) {
//	clearPointRadius();
//	clearBoundingBox();
//});

$('button[name=noTypeButton]').on('click', function (e) {
	clearType();
});
		
$('button[name=typeButton]').on('click', function (e) {
	updateType();
});

$('button[name=noFederationButton]').on('click', function (e) {
	$('input[name=src]').val("local");
});

$('button[name=selectedFederationButton]').on('click', function (e) {
	updateFederation();
});

$('button[name=enterpriseFederationButton]').on('click', function (e) {
	$('input[name=src]').val("");
});

function resetForm() {
	jQuery(':hidden').val('');
	
	$('input[name=format]').val("geojson");
	$('select[name=count]').val("10");
	$('input[name=start]').val("1");
	
	$('button[name=noLocationButton]').click();
	clearPointRadius();
	clearBoundingBox();
		
	$('button[name=noTemporalButton]').click();
	clearOffset();
	clearAbsoluteTime();

	$('button[name=noFederationButton]').click();
	$('input[name=src]').val("local");
	
	$('button[name=noTypeButton]').click();	
	clearType();
}

		