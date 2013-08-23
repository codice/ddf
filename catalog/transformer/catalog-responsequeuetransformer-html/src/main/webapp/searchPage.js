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

$('.search-controls').partialaffix({
	offset: {
		top: 63,
		bottom: 30
	}
});

var validateAbsoluteDates = function(dateText, instance) {
	updateAbsoluteTime();
}

$('#absoluteStartTime').datetimepicker({
	dateFormat: $.datepicker.ATOM,
	timeFormat: "HH:mm:ss.lz",
	separator: "T",
	timezoneIso8601: true,
	useLocalTimezone: true,
	showSecond: true,
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
	showSecond: true,
	showMillisec: false,
	showTimezone: false,
	minDate: new Date(100, 0, 2),
	maxDate: new Date(9999, 11, 30),
	onClose: validateAbsoluteDates
});

// initialization logic to load from url
var urlVals = $.url();

$('input[name=q]').val(urlVals.param('q'));

// format the resource size for display and make it visible
var divs = document.getElementsByClassName('resourceSize');
for ( var i = 0; i < divs.length; i++) {
	var formattedSize = formatResourceSize(divs[i].innerHTML);
	divs[i].innerHTML = formattedSize;
	divs[i].style.visibility = "visible";
}


var relOffsetEntry = getPositiveIntValue(urlVals.param('offsetTime'));
$('input[name=offsetTime]').val(relOffsetEntry);
			
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

var pointRadiusLatitude = validateNumberInRange(-90, 90, urlVals.param('latitude'), "");
$('input[name=latitude]').val(pointRadiusLatitude);
var pointRadiusLongitude = validateNumberInRange(-180, 180, urlVals.param('longitude'), "");
$('input[name=longitude]').val(pointRadiusLongitude);
var radiusValue = getPositiveIntValue(urlVals.param('radiusValue'));
$('input[name=radiusValue]').val(radiusValue);
if(urlVals.param('radiusUnits')) {
	$('select[name=radiusUnits]').val(urlVals.param('radiusUnits'));
}	

var bboxWest =  validateNumberInRange(-180, 180, urlVals.param('west'), "");
$('input[name=west]').val(bboxWest);
var bboxSouth =  validateNumberInRange(-90, 90, urlVals.param('south'), "");
$('input[name=south]').val(bboxSouth);
var bboxEast =  validateNumberInRange(-180, 180, urlVals.param('east'), "");
$('input[name=east]').val(bboxEast);
var bboxNorth =  validateNumberInRange(-90, 90, urlVals.param('north'), "");
$('input[name=north]').val(bboxNorth);

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
if(!src && src !=="") {
	src = "local";
}
$('input[name=src]').val(src);
if(!src) {
	$('button[name=enterpriseFederationButton]').click();
} else if(src == "local") {
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
})

$('button[name=relativeTimeButton]').on('click', function (e) {
	updateOffset();
	clearAbsoluteTime();
})

$('button[name=absoluteTimeButton]').on('click', function (e) {
	updateAbsoluteTime();
	clearOffset();
})
		
$('button[name=noLocationButton]').on('click', function (e) {
	clearPointRadius();
	clearBoundingBox();
})

$('button[name=pointRadiusButton]').on('click', function (e) {
	updatePointRadius();
	clearBoundingBox();
})

$('button[name=bboxButton]').on('click', function (e) {
	clearPointRadius();
	updateBoundingBox();
})

//$('button[name=wktButton]').on('click', function (e) {
//	clearPointRadius();
//	clearBoundingBox();
//})

$('button[name=noTypeButton]').on('click', function (e) {
	clearType();
})
		
$('button[name=typeButton]').on('click', function (e) {
	updateType();
})

$('button[name=noFederationButton]').on('click', function (e) {
	$('input[name=src]').val("local");
})

$('button[name=selectedFederationButton]').on('click', function (e) {
	updateFederation();
})

$('button[name=enterpriseFederationButton]').on('click', function (e) {
	$('input[name=src]').val("");
})

$('.metacard-modal').live('click', function(e) {
    e.preventDefault();
    var url = $(this).attr('href');
    $('#metacardModal .modal-body').html('<iframe id="metacardIframe" frameborder="0" scrolling="yes" allowtransparency="false" src="' + url + '"></iframe>');
    $('#metacardModal').modal('show');
});

function resetForm() {
	jQuery(':hidden').val('');
	
	$('input[name=format]').val("querypage");
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

function restoreFederationSelections(src) {	
	if(src) {
		var sources = src.split(",");
		$("select[name=federationSources]").val(sources);	
	}
	updateFederationWarning(src);
}

function updateFederationWarning(src) {
	if(src) {
		$('#federationListWarning').hide();
	} else {
		$('#federationListWarning').show();		
	}	
}

function updateFederation() {
	var src = $("select[name=federationSources] :selected").map(function(){ return this.value; }).get().join(",");
	$('input[name=src]').val(src);
	updateFederationWarning(src);
}

function restoreTypeSelection(type) {	
	$("select[name=typeList] option:contains('" + type + "')").attr("selected", "selected");
}

function restoreCountSelection(count) {	
	$("select[name=count] option").filter(function() {return $(this).text() == count}).attr("selected", "selected");
}

function updateType() {
	$('input[name=type]').val($('select[name=typeList]').val());
}

function clearType() {
	$('input[name=type]').val("");
}

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

function clearOffset() {
	$('input[name=dtoffset]').val("");
}

function updateAbsoluteTime() {

	var start = $('input[name=absoluteStartTime]').datepicker( "getDate" );
	var end = $('input[name=absoluteEndTime]').datepicker( "getDate" );

	if(start && end) {
		if(start > end) {
			$('input[name=absoluteStartTime]').datepicker( "setDate", end);
			$('input[name=absoluteEndTime]').datepicker( "setDate", start);
		}
		
		$('input[name=dtstart]').val($('input[name=absoluteStartTime]').val())
		$('input[name=dtend]').val($('input[name=absoluteEndTime]').val())
		
		$('#timeAbsoluteWarning').hide();
	} else {
		$('#timeAbsoluteWarning').show();
		clearAbsoluteTime();
	}
}

function clearAbsoluteTime() {
	$('input[name=dtstart]').val("");
	$('input[name=dtend]').val("");
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

function clearPointRadius() {
	$('input[name=lat]').val("");
	$('input[name=lon]').val("");	
	$('input[name=radius]').val("");				
}

function clearBoundingBox() {
	$('input[name=bbox]').val("");
}

function getDistanceInMeters(distance, units) {

	switch(units) {
		case "meters":
			return distance;
		case "kilometers":
			return distance * 1000;
		case "feet":
			return Math.ceil(distance * .3048);
		case "yards":
			return Math.ceil(distance * .9144);
		case "miles":
			return Math.ceil(distance * 1609.34);
		default:
			return distance;
	}
}

function getTimeInMillis(offset, units) {

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
			var now = new Date();
			var timeInMillis = now.valueOf();
			now.setMonth(now.getMonth() - offset);
			return (timeInMillis - now.valueOf());
		case "years": // not decimal friendly
			var now = new Date();
			var timeInMillis = now.valueOf();
			now.setFullYear(now.getFullYear() - offset);
			return (timeInMillis - now.valueOf());
		default:
			return offset * 60000;
	}
}

function validateNumber(numberElement, revertValue) {
	var val = validateNumberInRange(numberElement.attr("min"), numberElement.attr("max"), $.trim(numberElement.val()), revertValue);
	numberElement.val(val);
	return val;
}

function validateNumberInRange(min, max, value, revertValue) {	
	validateNumberInRange(min, max, value, revertValue, false);
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
	if(Number(val) == 0) {
		val = "";
	}
	val = getPositiveIntValue(val);
	posIntElement.val(val);
	return val;
}

function getPositiveIntValue(offset) {
	var offsetValue = Number(offset);
	var offsetIntValue = Math.floor(offsetValue);
	
	if(offsetIntValue > 0) { 
		return offsetIntValue;
	} else { 
		return "";				
	} 
}

function isNonNegativeInteger(value) {
	if(isNaN(value)) {
		return false;
	}
	var val = Number(value);
	var intVal = ~~val;
	return val == intVal && intVal >= 0;
}

// return a formatted (human readable) string for the resource size in bytes
function formatResourceSize(resourceSize) {
	var SizePrefixes = ' KMGTPEZYXWVU';

	if (resourceSize.toLowerCase().indexOf("n/a") >= 0)
		return '';

	// if the size is not a number, and it isn't 'n/a', assume it is
	// already formatted, ie "10 MB"
	if (isNaN(resourceSize))
		return resourceSize;

	if (resourceSize <= 0)
		return '0';
	var t2 = Math.min(Math.round(Math.log(resourceSize) / Math.log(1024)), 12);
	return (Math.round(resourceSize * 100 / Math.pow(1024, t2)) / 100)
			+ SizePrefixes.charAt(t2).replace(' ', '') + 'B';
}


		
