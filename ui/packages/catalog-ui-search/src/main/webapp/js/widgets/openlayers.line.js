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
const ol = require('openlayers')
const _ = require('underscore')
const properties = require('../properties.js')
const wreqr = require('../wreqr.js')
const NotificationView = require('./notification.view')
const Turf = require('@turf/turf')
const DrawingController = require('./drawing.controller')
const olUtils = require('../OpenLayersGeometryUtils')
const DistanceUtils = require('../DistanceUtils.js')

function translateFromOpenlayersCoordinates(coords) {
  const coordinates = []
  _.each(coords, point => {
    point = ol.proj.transform(
      [
        DistanceUtils.coordinateRound(point[0]),
        DistanceUtils.coordinateRound(point[1]),
      ],
      properties.projection,
      'EPSG:4326'
    )
    if (point[1] > 90) {
      point[1] = 89.9
    } else if (point[1] < -90) {
      point[1] = -89.9
    }
    coordinates.push(point)
  })
  return coordinates
}

function translateToOpenlayersCoordinates(coords) {
  const coordinates = []
  _.each(coords, item => {
    if (item[0].constructor === Array) {
      coordinates.push(translateToOpenlayersCoordinates(item))
    } else {
      coordinates.push(
        ol.proj.transform(
          [item[0], item[1]],
          'EPSG:4326',
          properties.projection
        )
      )
    }
  })
  return coordinates
}

const Draw = {}

Draw.LineView = Marionette.View.extend({
  initialize(options) {
    this.map = options.map
    this.listenTo(
      this.model,
      'change:line change:lineWidth change:lineUnits',
      this.updatePrimitive
    )
    this.updatePrimitive(this.model)
  },
  setModelFromGeometry(geometry) {
    this.model.set({
      line: translateFromOpenlayersCoordinates(geometry.getCoordinates()),
    })
  },

  modelToPolygon(model) {
    const polygon = model.get('line')
    const setArr = _.uniq(polygon)
    if (setArr.length < 2) {
      return
    }

    const rectangle = new ol.geom.LineString(
      translateToOpenlayersCoordinates(setArr)
    )
    return rectangle
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
    const lineWidth =
      DistanceUtils.getDistanceInMeters(
        this.model.get('lineWidth'),
        this.model.get('lineUnits')
      ) || 1

    const turfLine = Turf.lineString(
      translateFromOpenlayersCoordinates(rectangle.getCoordinates())
    )
    const bufferedLine = Turf.buffer(turfLine, lineWidth, 'meters')
    const geometryRepresentation = new ol.geom.MultiLineString(
      translateToOpenlayersCoordinates(bufferedLine.geometry.coordinates)
    )

    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }

    this.billboard = new ol.Feature({
      geometry: geometryRepresentation,
    })

    this.billboard.setId(this.model.cid)

    const color = this.model.get('color')

    const iconStyle = new ol.style.Style({
      stroke: new ol.style.Stroke({
        color: color ? color : '#914500',
        width: 3,
      }),
    })
    this.billboard.setStyle(iconStyle)

    const vectorSource = new ol.source.Vector({
      features: [this.billboard],
    })

    let vectorLayer = new ol.layer.Vector({
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
    this.listenTo(this.model, 'change:line', this.updateGeometry)
    this.listenTo(this.model, 'change:lineWidth', this.updateGeometry)

    this.model.trigger('EndExtent', this.model)
    wreqr.vent.trigger('search:linedisplay', this.model)
  },
  start() {
    const that = this

    this.primitive = new ol.interaction.Draw({
      type: 'LineString',
      style: new ol.style.Style({
        stroke: new ol.style.Stroke({
          color: [0, 0, 255, 0],
        }),
      }),
    })

    this.map.addInteraction(this.primitive)
    this.primitive.on('drawend', sketchFeature => {
      window.cancelAnimationFrame(that.accurateLineId)
      that.handleRegionStop(sketchFeature)
      that.map.removeInteraction(that.primitive)
    })
    this.primitive.on('drawstart', sketchFeature => {
      that.showAccurateLine(sketchFeature)
    })
  },
  accurateLineId: undefined,
  showAccurateLine(sketchFeature) {
    this.accurateLineId = window.requestAnimationFrame(() => {
      this.drawBorderedPolygon(sketchFeature.feature.getGeometry())
      this.showAccurateLine(sketchFeature)
    })
  },

  stop() {
    this.stopListening()
  },

  destroyPrimitive() {
    window.cancelAnimationFrame(this.accurateLineId)
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
  drawingType: 'line',
  show(model) {
    if (this.enabled) {
      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.destroyPrimitive()
        existingView.updatePrimitive(model)
      } else {
        const view = new Draw.LineView({
          map: this.options.map,
          model,
        })
        view.updatePrimitive(model)
        this.addView(view)
      }

      return model
    }
  },
  draw(model) {
    if (this.enabled) {
      const view = new Draw.LineView({
        map: this.options.map,
        model,
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
      model.trigger('BeginExtent')
      this.listenToOnce(model, 'EndExtent', function() {
        this.notificationView.destroy()
      })

      return model
    }
  },
})

module.exports = Draw
