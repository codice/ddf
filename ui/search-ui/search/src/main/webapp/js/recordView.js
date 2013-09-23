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

function RecordView(metacard){
	this.metacard = metacard;
	this.properties = metacard.properties;
	
	this._createRow = function(property, value){
		
		var propTd, valTd, rowTr, valDiv;
		
		propTd = "<td class='property-td'>"+property+"</td>";
		
		if($.type(value) === "string"){
			valTd = "<td class='value-td'><div class=value-div>" + value + "</div></td>";
			rowTr = "<tr>" + propTd + valTd + "</tr>";
		}else{
			rowTr = $("<tr>");
			rowTr.append(propTd);
			valTd = $("<td class='value-td'></td>");
			valDiv = $("<div class='value-div'></div>");
			valDiv.append(value);
			valTd.append(valDiv);
			rowTr.append(valTd);
		}
		return rowTr;
	};
	
	this._getGeometry = function(geometry){
        try {
                if(geometry.type.toUpperCase() === "GEOMETRYCOLLECTION") {
                        return this._getGeometry(geometry.geometries[0]);
                }
                return geometry.type + "(" + this._getCoordinates(geometry.coordinates) + ")";
        } catch(err) {
                return "";
        }

	};
	
	this._getCoordinates = function(coordinates) {
		var val, i;
        if($.isArray(coordinates[0])) {
            val = "(" + this._getCoordinates(coordinates[0]);

            for(i = 1; i < coordinates.length; i++) {
                    val += ", " + this._getCoordinates(coordinates[i]);
            }
            val += ")";
            return val;
	    } else {
	            return coordinates.join(" ");
	    }
	};
	
	this._buildThumbnail = function(data){
		var srcString, thumbnailLink;
		if(data){
			srcString = "data:image/jpeg;charset=utf-8;base64, " + data;
			thumbnailLink = "<img class='thumbnail' alt='' src='" + srcString+"'></img>";
			return thumbnailLink;
		}else{
			return "<i>No image available</i>";
		}
	};
	
	this._buildMetadata = function(data){
		var metadataContent, metadataXml, expandButton;
		
		metadataContent = $("<div class=\"well\"></div>");

		metadataXml = buildMetadataHtml(data);
		metadataXml.css('display','none');
		metadataContent.append(metadataXml);

		expandButton = $("<a class='btn btn-small btn-info'>expand</a>");
		expandButton.click(function(){
			if($(this).prev().is(":hidden")){
				$(this).prev().show();
				$(this).text("collapse");
			}else{
				$(this).prev().hide();
				$(this).text("expand");
			}
		});
		metadataContent.append(expandButton);
		return metadataContent;
	
	};
	
	this._buildResource = function(){
		var productLink, productLinkHref;
		productLink = "";

		if(typeof(getDDFServer) === 'function'){
			productLinkHref = getDDFServer() + "/services/catalog/sources/" + this.properties["source-id"] + "/" + this.properties.id + "?transform=resource";
			productLink = "<a target='blank' href='"+productLinkHref+"'><i class='icon-download-alt icon-2x'></i></a>";
		}else{
			productLink = "<a class='btn btn-small btn-danger'>server not found</a>";
		}

		return productLink;
	};
	
	this._formatDate = function(dateString){
		return dateString.replace("T",", ").replace("+0000"," UTC");
	};
	
	this._updateRecordDiv = function(divId){
		$("#"+divId+" .panel-heading h2").html(this.metacard.properties.title);
		this._updatePropertiesTable($("#"+divId+" .record-table tbody"));
	};
	
	this._updatePropertiesTable = function(tableBody){
		var newHtmlString = this._createRow("Title",this.properties.title);
		newHtmlString += this._createRow("Thumbnail",this._buildThumbnail(this.properties.thumbnail));
		newHtmlString += this._createRow("Metadata Content Type",this.properties["metadata-content-type"]);
		newHtmlString += this._createRow("Geometry",this._getGeometry(this.metacard.geometry));
		newHtmlString += this._createRow("ID",this.properties.id);
		newHtmlString += this._createRow("Source ID",this.properties["source-id"]);
		newHtmlString += this._createRow("Created",this._formatDate(this.properties.created));
		newHtmlString += this._createRow("Last Modified",this._formatDate(this.properties.modified));
		newHtmlString += this._createRow("Resource",this._buildResource());
		if(this.properties["resource-uri"]) {
			newHtmlString += this._createRow("Resource URI", this.properties["resource-uri"]);
		}
		newHtmlString += this._createRow("Resource Size",this.properties["resource-size"]);
		newHtmlString += this._createRow("Type",this.metacard.type);
		tableBody.html(newHtmlString);		
		tableBody.append(this._createRow("Metadata",this._buildMetadata(this.properties.metadata)));
	};

}

RecordView.prototype = {
	
	buildView : function(divId){
		
		this._updateRecordDiv(divId);

		// hacky way to make sure the values don't overrun their bounds
		// Should be improved in the future.
		$(".value-div").css("width",$(".panel-info").width()-185);
	}
	
};


		
