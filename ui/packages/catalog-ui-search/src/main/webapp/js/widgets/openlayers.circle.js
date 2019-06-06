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
const TurfCircle = require('@turf/circle')
const DrawingController = require('./drawing.controller')
const olUtils = require('../OpenLayersGeometryUtils')
const DistanceUtils = require('../DistanceUtils.js')

function translateFromOpenlayersCoordinate(coord) {
  return ol.proj.transform(
    [Number(coord[0]), Number(coord[1])],
    properties.projection,
    'EPSG:4326'
  )
}

function translateToOpenlayersCoordinate(coord) {
  return ol.proj.transform(
    [Number(coord[0]), Number(coord[1])],
    'EPSG:4326',
    properties.projection
  )
}

function translateToOpenlayersCoordinates(coords) {
  const coordinates = []
  _.each(coords, item => {
    coordinates.push(translateToOpenlayersCoordinate(item))
  })
  return coordinates
}

const Draw = {}

Draw.CircleView = Marionette.View.extend({
  initialize(options) {
    this.map = options.map
    this.listenTo(
      this.model,
      'change:lat change:lon change:radius change:radiusUnits',
      this.updateGeometry
    )
    this.updateGeometry(this.model)
  },
  setModelFromGeometry(geometry) {
    const center = translateFromOpenlayersCoordinate(geometry.getCenter())
    const rad =
      geometry.getRadius() *
      this.map
        .getView()
        .getProjection()
        .getMetersPerUnit()

    this.model.set({
      lat: DistanceUtils.coordinateRound(center[1]),
      lon: DistanceUtils.coordinateRound(center[0]),
      radius: DistanceUtils.coordinateRound(
        DistanceUtils.getDistanceFromMeters(rad, this.model.get('radiusUnits'))
      ),
    })
  },

  modelToCircle(model) {
    if (model.get('lon') === undefined || model.get('lat') === undefined) {
      return undefined
    }
    const rectangle = new ol.geom.Circle(
      translateToOpenlayersCoordinate([model.get('lon'), model.get('lat')]),
      DistanceUtils.getDistanceInMeters(
        model.get('radius'),
        model.get('radiusUnits')
      ) /
        this.map
          .getView()
          .getProjection()
          .getMetersPerUnit()
    )
    return rectangle
  },

  updatePrimitive(model) {
    const polygon = this.modelToCircle(model)
    // make sure the current model has width and height before drawing
    if (polygon && !_.isUndefined(polygon)) {
      this.drawBorderedPolygon(polygon)
    }
  },

  updateGeometry(model) {
    if (
      model.get('lon') !== undefined &&
      model.get('lat') !== undefined &&
      model.get('radius')
    ) {
      const circle = this.modelToCircle(model)
      if (circle) {
        this.drawBorderedPolygon(circle)
      }
    }
  },

  drawBorderedPolygon(rectangle) {
    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }

    if (!rectangle) {
      // handles case where model changes to empty vars and we don't want to draw anymore
      return
    }

    const point = Turf.point(
      translateFromOpenlayersCoordinate(rectangle.getCenter())
    )
    const turfCircle = new TurfCircle(
      point,
      rectangle.getRadius() *
        this.map
          .getView()
          .getProjection()
          .getMetersPerUnit(),
      64,
      'meters'
    )
    const geometryRepresentation = new ol.geom.LineString(
      translateToOpenlayersCoordinates(turfCircle.geometry.coordinates[0])
    )

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
    this.listenTo(
      this.model,
      'change:lat change:lon change:radius',
      this.updateGeometry
    )

    this.model.trigger('EndExtent', this.model)
    wreqr.vent.trigger('search:circledisplay', this.model)
  },
  start() {
    const that = this

    this.primitive = new ol.interaction.Draw({
      type: 'Circle',
      style: new ol.style.Style({
        stroke: new ol.style.Stroke({
          color: [0, 0, 255, 0],
        }),
      }),
    })

    this.map.addInteraction(this.primitive)
    this.primitive.on('drawend', sketchFeature => {
      window.cancelAnimationFrame(that.accurateCircleId)
      that.handleRegionStop(sketchFeature)
      that.map.removeInteraction(that.primitive)
    })
    this.primitive.on('drawstart', sketchFeature => {
      that.showAccurateCircle(sketchFeature)
    })
  },
  accurateCircleId: undefined,
  showAccurateCircle(sketchFeature) {
    this.accurateCircleId = window.requestAnimationFrame(() => {
      this.drawBorderedPolygon(sketchFeature.feature.getGeometry())
      this.showAccurateCircle(sketchFeature)
      this.setModelFromGeometry(sketchFeature.feature.getGeometry())
    })
  },

  stop() {
    this.stopListening()
  },

  destroyPrimitive() {
    window.cancelAnimationFrame(this.accurateCircleId)
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
  drawingType: 'circle',
  show(model) {
    if (this.enabled) {
      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.destroyPrimitive()
        existingView.updateGeometry(model)
      } else {
        const view = new Draw.CircleView({
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
      const view = new Draw.CircleView({
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
