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

/**
 * 
 * This file creates a 'RecordView' object, which is an HTML representation of a metacard.
 * 
 * A RecordView is created by passing in a JSON metacard object.  Once created,
 * the buildView() function can be called, and the specified \<div\> will be filled
 * with the HTML representation of the record.
 * 
 * DEPENDENCIES: metadataHelper.js
 * 
 */

Handlebars.registerHelper("formatDate", function(dateString, options) {
	return new Handlebars.SafeString(dateString.replace("T",", ").replace("+0000"," UTC"));
});

Handlebars.registerHelper("formatGeometry", function(geometry, options) {
	var getCoordinates;
	getCoordinates = function(coordinates) {
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
	};

    try {
        while(geometry.type.toUpperCase() === "GEOMETRYCOLLECTION") {
            geometry = geometry.geometries[0];
        }
        return geometry.type + "(" + getCoordinates(geometry.coordinates) + ")";
    } catch(err) {
        return "";
    }
});

Handlebars.registerHelper("hasServicesUrl", function(options){
	if(typeof(getServicesUrl) === "function") {
		return options.fn(this);
	} else {
		return options.inverse(this);
	}
});

Handlebars.registerHelper("servicesUrl", function(){
	return getServicesUrl();
});

Handlebars.registerHelper("buildMetadata", function(metadata, options){
	var tmp = $("<div></div>");
	tmp.append(buildMetadataHtml(metadata));
	return new Handlebars.SafeString(tmp.html());
});

function toggleMetadata(e) {
	if($('#metadataContents').is(":hidden")){
		$('#metadataContents').show();
		$('#metadataExpandButton').text("collapse");
	}else{
		$('#metadataContents').hide();
		$('#metadataExpandButton').text("expand");
	}
}

function RecordView(metacard){
	this.metacard = metacard;
	this.properties = metacard.properties;
	
	this._updateRecordDiv = function(divId){
		$.ajax({
			url: "templates/recordContents.hbt",
			cache: true,
			context: metacard,
			success: function (source) {
				var template = Handlebars.compile(source);
				$("#"+divId).html(template(this));

				// hacky way to make sure the values don't overrun their bounds
				// Should be improved in the future.
				$(".value-div").css("width",$(".panel-info").width()-185);
			}
		});
	};	
}

RecordView.prototype = {
	
	buildView : function(divId){	
		this._updateRecordDiv(divId);
	}
	
};


		
