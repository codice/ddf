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

var $ = require('jquery')
var _ = require('underscore')
var Backbone = require('backbone')

var Cesium = require('cesium')
var announcement = require('component/announcement')
var properties = require('properties')

// Note: using a non-secure resource will fail when running DDF with TLS.
var geocoderOnlineEndpoint =
  'https://nominatim.openstreetmap.org/search?format=json&q='
var geocoderOfflineEndpoint = './internal/REST/v1/Locations'
var onlineGazetteer = properties.onlineGazetteer

module.exports = Backbone.Model.extend({
  geocode(input) {
    // Check for online/offline gazetteer, falling back to offline
    // gazetteer if online request fails.
    if (onlineGazetteer) {
      return this.queryGazetteerOnline(input)
    } else {
      return this.queryGazetteerOffline(input)
    }
  },
  queryGazetteerOnline(input) {
    // Load in request string
    return new Promise((resolve, reject) => {
      $.ajax({
        url: geocoderOnlineEndpoint + input,
        success: function(results) {
          const formattedResults = results.map(function(resultObject) {
            const bboxDegrees = resultObject.boundingbox
            return {
              displayName: resultObject.display_name,
              destination: Cesium.Rectangle.fromDegrees(
                bboxDegrees[2],
                bboxDegrees[0],
                bboxDegrees[3],
                bboxDegrees[1]
              ),
            }
          })
          resolve(formattedResults)
        },
        error: function(error) {
          reject(error)
          announcement.announce({
            title: 'Online gazetteer not working, please try again.',
            message:
              'If the problem persists, contact your administrator. Caused by: ' +
              String(error),
            type: 'error',
          })
        },
      })
    })
  },
  queryGazetteerOffline(input) {
    return new Promise((resolve, reject) => {
      $.ajax({
        url: geocoderOfflineEndpoint,
        data: 'jsonp=jsonp&query=' + input,
        contentType: 'application/javascript',
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function(result) {
          const resource = result.resourceSets[0].resources[0]
          const formattedResult =
            typeof resource !== 'undefined'
              ? {
                  displayName: resource.name,
                  destination: Cesium.Rectangle.fromDegrees(
                    resource.bbox[1],
                    resource.bbox[0],
                    resource.bbox[3],
                    resource.bbox[2]
                  ),
                }
              : {
                  displayName: 'Location not found',
                }
          resolve(formattedResult)
        },
        error: function(error) {
          reject(error)
          announcement.announce({
            title: 'Geocoder Error',
            message: String(error),
            type: 'error',
          })
        },
      })
    })
  },
})
