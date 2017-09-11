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
/*global require, window*/

var $ = require('jquery');
var _ = require('underscore');
var Backbone = require('backbone');

var Cesium = require('cesium');
var announcement = require('component/announcement');
var properties = require('properties');

// Note: using a non-secure resource will fail when running DDF with TLS.
var geocoderOnlineEndpoint = 'https://nominatim.openstreetmap.org/search?format=json&q=';
var geocoderOfflineEndpoint = '/services/REST/v1/Locations?jsonp=loadJsonp&key=0&query=';
var onlineGazetteer = properties.onlineGazetteer;

module.exports = Backbone.Model.extend({
    geocode(input) {
        // Check for online/offline gazetteer, falling back to offline
        // gazetteer if online request fails.
        if (onlineGazetteer) {
            return this.queryGazetteerOnline(input);
        } else {
            return this.queryGazetteerOffline(input);
        }
    },
    queryGazetteerOnline(input) {
        // Load in request string
        return Cesium.loadJson(geocoderOnlineEndpoint + input)
            .then(function(results) {
                var bboxDegrees;

                // Extract desired fields
                return results.map(function(resultObject) {
                    bboxDegrees = resultObject.boundingbox;
                    return {
                        displayName: resultObject.display_name,
                        destination: Cesium.Rectangle.fromDegrees(
                            bboxDegrees[2],
                            bboxDegrees[0],
                            bboxDegrees[3],
                            bboxDegrees[1]
                        )
                    };
                });
            }).otherwise(function(error) {
                onlineGazetteer = false;
                // Run query against offline gazetteer
                return this.queryGazetteerOffline(geocoderOfflineEndpoint, input);
            }.bind(this));
    },
    queryGazetteerOffline(input) {
        // Load in request string
        return Cesium.loadText(geocoderOfflineEndpoint + input)
            .then(function(results) {
                // Strip out unwanted characters and parse JSON
                var jsonResult = /\((.+)\)/.exec(results)[1];
                var jsonObject = JSON.parse("[" + jsonResult + "]");

                // Extract desired fields
                return jsonObject.map(function(locationResult) {
                    var resultValues = locationResult.resourceSets[0].resources[0];
                    if (typeof resultValues != 'undefined') {
                        return {
                            displayName: resultValues.name,
                            destination: Cesium.Rectangle.fromDegrees(
                                resultValues.bbox[1],
                                resultValues.bbox[0],
                                resultValues.bbox[3],
                                resultValues.bbox[2]
                            )
                        };
                    } else {
                        return {
                            displayName: "Location not found",
                        };
                    }
                });
            }).otherwise(function(error) {
                announcement.announce({
                    title: 'Geocoder Error',
                    message: String(error),
                    type: 'error'
                });
            });
    }
});
