/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

define(['backbone', 'jquery'], function(Backbone, $) {
  var GwcLayers = {}

  GwcLayers.LayerModel = Backbone.Model.extend({
    initialize: function() {
      this.set({ layers: [] })
      this.getLayers()
    },
    getLayers: function() {
      var url = '../../geowebcache/rest/layers'
      var that = this
      $.ajax({
        url: url,
        dataType: 'xml',
        success: function(data) {
          that.parseLayersXml(data)
        },
      })
    },
    parseLayerXml: function(name, data) {
      var xmlUrl = $(data)
        .find('wmsUrl')
        .find('string')
        .text()
      var layerUrl = '../../geowebcache/rest/layers/' + name + '.xml'
      var mimes = $(data)
        .find('mimeFormats')
        .find('string')
      var mimeTypes = []
      $.each(mimes, function(index, value) {
        mimeTypes.push($(value).text())
      })

      var xmlLayers = $(data)
        .find('wmsLayers')
        .text()
      var xmlLayersArray = xmlLayers.trim().split(',')

      if (!this.nameExistsInArray(name)) {
        var layer = {
          name: name,
          xmlUrl: xmlUrl,
          layerUrl: layerUrl,
          mimeTypes: mimeTypes,
          xmlLayers: xmlLayersArray,
        }
        this.get('layers').push(layer)
      } else {
        var updateLayer = this.getLayer(name)
        updateLayer.xmlUrl = xmlUrl
        updateLayer.layerUrl = layerUrl
        updateLayer.mimeTypes = mimeTypes
        updateLayer.xmlLayers = xmlLayersArray
      }
      this.trigger('change:layers', this)
    },
    parseLayersXml: function(data) {
      var that = this
      $(data)
        .find('layer')
        .each(function() {
          var name = $(this)
            .find('name')
            .text()
          var url = '../../geowebcache/rest/layers/' + name + '.xml'
          $.ajax({
            url: url,
            dataType: 'xml',
            success: function(data) {
              that.parseLayerXml(name, data)
            },
          })
        })
    },
    deleteLayer: function(name) {
      var url = '../../geowebcache/rest/layers/' + name + '.xml'
      var that = this
      $.ajax({
        url: url,
        type: 'DELETE',
        success: function() {
          var layers = that.get('layers')
          $.each(layers, function() {
            if (name === this.name) {
              layers.splice($.inArray(this, layers), 1)
              that.set('layers', layers)
              that.trigger('change:layers', this)
            }
          })
        },
      })
    },
    postLayerData: function(layerData, type) {
      var restUrl = '../../geowebcache/rest/layers/' + layerData.name + '.xml'
      var that = this
      var data = this.generateXmlForLayer(layerData)
      $.ajax({
        url: restUrl,
        type: type,
        dataType: 'xml',
        data: data,
        success: function() {
          that.getLayers()
        },
      })
    },
    addLayer: function(layerData) {
      this.postLayerData(layerData, 'PUT')
    },
    updateLayer: function(layerData) {
      if (this.nameExistsInArray(layerData.name)) {
        this.postLayerData(layerData, 'POST')
      } else {
        this.deleteLayer(layerData.oldName)
        this.postLayerData(layerData, 'PUT')
      }
    },
    nameExistsInArray: function(name) {
      var exists
      $.each(this.get('layers'), function(index, value) {
        if (name === value.name) {
          exists = true
        }
      })
      return exists
    },
    getLayer: function(name) {
      var layer
      $.each(this.get('layers'), function(index, value) {
        if (name === value.name) {
          layer = value
        }
      })
      return layer
    },
    generateXmlForLayer: function(layerData) {
      var data
      data = '<wmsLayer>'
      data += '\t<name>' + layerData.name + '</name>'
      data += '\t<mimeFormats>'
      $.each(layerData.mimes, function(index, value) {
        data += '\t\t<string>' + value + '</string>'
      })
      data += '\t</mimeFormats>'
      data += '\t<wmsUrl>'
      data += '\t\t<string>' + layerData.url + '</string>'
      data += '\t</wmsUrl>'
      if (layerData.wmsLayerNames.length > 0) {
        data += '\t<wmsLayers>'
        data += layerData.wmsLayerNames.join(',')
        data += '</wmsLayers>'
      }
      data += '</wmsLayer>'
      return data
    },
  })
  return GwcLayers
})
