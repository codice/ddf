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

const Marionette = require('marionette')
const Backbone = require('backbone')
const ol = require('openlayers')
const _ = require('underscore')
const properties = require('../properties.js')
const wreqr = require('../wreqr.js')
const NotificationView = require('./notification.view')
const ShapeUtils = require('../ShapeUtils.js')
const Turf = require('@turf/turf')
const DrawingController = require('./drawing.controller')
const olUtils = require('../OpenLayersGeometryUtils')
const DistanceUtils = require('../DistanceUtils.js')

const translateFromOpenlayersCoordinates = coords => {
  return coords
    .map(value =>
      value.map(point => {
        const mappedPoint = ol.proj.transform(
          [
            DistanceUtils.coordinateRound(point[0]),
            DistanceUtils.coordinateRound(point[1]),
          ],
          properties.projection,
          'EPSG:4326'
        )
        if (mappedPoint[1] > 90) {
          mappedPoint[1] = 89.9
        } else if (mappedPoint[1] < -90) {
          mappedPoint[1] = -89.9
        }
        return mappedPoint
      })
    )
    .flatten()
}

const Draw = {}

Draw.PolygonModel = Backbone.Model.extend({
  defaults: {
    north: undefined,
    east: undefined,
    west: undefined,
    south: undefined,
  },
})

Draw.PolygonView = Marionette.View.extend({
  initialize(options) {
    this.map = options.map
    this.listenTo(
      this.model,
      'change:polygon change:polygonBufferWidth change:polygonBufferUnits',
      this.updateGeometry
    )
    this.updatePrimitive(this.model)
  },

  setModelFromGeometry(geometry) {
    this.model.set({
      polygon: translateFromOpenlayersCoordinates(geometry.getCoordinates()),
    })
  },

  coordsToLineString(rawCoords) {
    const coords = []
    const setArr = _.uniq(rawCoords)
    if (setArr.length < 3) {
      return
    }
    _.each(setArr, function(item) {
      coords.push(
        ol.proj.transform(
          [item[0], item[1]],
          'EPSG:4326',
          properties.projection
        )
      )
    })

    // Ensure that the first and last coordinate are the same
    if (!_.isEqual(coords[0], coords[coords.length - 1])) {
      coords.push(coords[0])
    }
    return [coords]
  },

  modelToPolygon(model) {
    const coords = model.get('polygon')
    if (!coords) {
      return
    }
    const isMultiPolygon = ShapeUtils.isArray3D(coords)
    const multiPolygon = isMultiPolygon ? coords : [coords]

    const polygons = []

    _.each(
      multiPolygon,
      function(polygon) {
        polygons.push(this.coordsToLineString(polygon))
      }.bind(this)
    )

    return polygons
  },

  updatePrimitive(model) {
    const polygon = this.modelToPolygon(model)
    // make sure the current model has width and height before drawing
    if (polygon && !_.isUndefined(polygon)) {
      this.drawBorderedPolygon(polygon)
    }
  },

  updateGeometry(model) {
    const rectangle = this.modelToPolygon(model)
    if (rectangle) {
      this.drawBorderedPolygon(rectangle)
    }
  },

  drawBorderedPolygon(rectangle) {
    if (!rectangle) {
      // handles case where model changes to empty vars and we don't want to draw anymore
      return
    }

    const coordinates = (Array.isArray(rectangle) && rectangle) || [
      rectangle.getCoordinates(),
    ]

    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }

    const bufferWidth =
      DistanceUtils.getDistanceInMeters(
        this.model.get('polygonBufferWidth'),
        this.model.get('polygonBufferUnits')
      ) || 1

    const drawnPolygonSegments = coordinates.map(set => {
      return Turf.multiLineString([translateFromOpenlayersCoordinates(set)])
        .geometry.coordinates
    })

    const bufferPolygonSegments = coordinates.map(set => {
      const polySegment = Turf.multiLineString([
        translateFromOpenlayersCoordinates(set),
      ])
      const bufferPolygons = Turf.buffer(
        polySegment,
        bufferWidth,
        'meters'
      ).geometry.coordinates.map(set => {
        return Turf.polygon([set])
      })
      return Turf.union(...bufferPolygons).geometry.coordinates
    })

    const bufferGeometryRepresentation =
      (bufferPolygonSegments &&
        new ol.geom.MultiPolygon(bufferPolygonSegments)) ||
      coordinates

    const drawnGeometryRepresentation =
      (drawnPolygonSegments &&
        new ol.geom.MultiPolygon(drawnPolygonSegments)) ||
      coordinates

    this.billboard = new ol.Feature({
      geometry: bufferGeometryRepresentation,
    })

    this.billboard.setId(this.model.cid)

    const drawnPolygonFeature = new ol.Feature({
      geometry: drawnGeometryRepresentation,
    })

    const color = this.model.get('color')

    const bufferPolygonIconStyle = new ol.style.Style({
      stroke: new ol.style.Stroke({
        color: color ? color : '#914500',
        width: 3,
      }),
      zIndex: 1,
    })

    const drawnPolygonIconStyle = new ol.style.Style({
      stroke: new ol.style.Stroke({
        color: color ? color : '#914500',
        width: 2,
        lineDash: [10, 5],
      }),
      zIndex: 0,
    })

    this.billboard.setStyle(bufferPolygonIconStyle)
    drawnPolygonFeature.setStyle(drawnPolygonIconStyle)

    const vectorSource = new ol.source.Vector({
      features: [this.billboard, drawnPolygonFeature],
    })

    const vectorLayer = new ol.layer.Vector({
      source: vectorSource,
    })

    this.vectorLayer = vectorLayer
    this.map.addLayer(vectorLayer)
  },

  handleRegionStop(sketchFeature) {
    const geometry = olUtils.wrapCoordinatesFromGeometry(
      sketchFeature.feature.getGeometry()
    )
    this.setModelFromGeometry(geometry)
    this.drawBorderedPolygon(geometry)
    this.listenTo(
      this.model,
      'change:polygon change:polygonBufferWidth',
      this.updateGeometry
    )

    this.model.trigger('EndExtent', this.model)
    wreqr.vent.trigger('search:polydisplay', this.model)
  },
  start() {
    const that = this

    this.primitive = new ol.interaction.Draw({
      type: 'Polygon',
      style: new ol.style.Style({
        stroke: new ol.style.Stroke({
          color: [0, 0, 255, 0],
        }),
      }),
    })

    this.map.addInteraction(this.primitive)
    this.primitive.on('drawend', function(sketchFeature) {
      window.cancelAnimationFrame(that.accuratePolygonId)
      that.handleRegionStop(sketchFeature)
      that.map.removeInteraction(that.primitive)
    })
    this.primitive.on('drawstart', function(sketchFeature) {
      that.showAccuratePolygon(sketchFeature)
    })
  },
  accuratePolygonId: undefined,
  showAccuratePolygon(sketchFeature) {
    this.accuratePolygonId = window.requestAnimationFrame(
      function() {
        this.drawBorderedPolygon(sketchFeature.feature.getGeometry())
        this.showAccuratePolygon(sketchFeature)
      }.bind(this)
    )
  },

  stop() {
    this.stopListening()
  },

  destroyPrimitive() {
    window.cancelAnimationFrame(this.accuratePolygonId)
    if (this.primitive) {
      this.map.removeInteraction(this.primitive)
    }
    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }
  },
  destroy() {
    this.destroyPrimitive()
    this.remove()
  },
})

Draw.Controller = DrawingController.extend({
  drawingType: 'poly',
  show(model) {
    if (this.enabled) {
      const polygonModel = model || new Draw.PolygonModel()

      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.destroyPrimitive()
        existingView.updatePrimitive(model)
      } else {
        const view = new Draw.PolygonView({
          map: this.options.map,
          model: polygonModel,
        })
        view.updatePrimitive(model)
        this.addView(view)
      }

      return polygonModel
    }
  },
  draw(model) {
    if (this.enabled) {
      const polygonModel = model || new Draw.PolygonModel()
      const view = new Draw.PolygonView({
        map: this.options.map,
        model: polygonModel,
      })

      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.stop()
        existingView.destroyPrimitive()
        this.removeView(existingView)
      }
      view.start()
      this.addView(view)
      this.notificationView = new NotificationView({
        el: this.notificationEl,
      }).render()
      polygonModel.trigger('BeginExtent')
      this.listenToOnce(polygonModel, 'EndExtent', function() {
        this.notificationView.destroy()
      })

      return polygonModel
    }
  },
})

module.exports = Draw
