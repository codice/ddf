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

const Marionette = require('marionette')
const Backbone = require('backbone')
const ol = require('openlayers')
const _ = require('underscore')
const properties = require('../properties.js')
const wreqr = require('../wreqr.js')
const NotificationView = require('./notification.view')
const DrawingController = require('./drawing.controller')
const olUtils = require('../OpenLayersGeometryUtils')
const DistanceUtils = require('../DistanceUtils.js')

const Draw = {}

Draw.BboxModel = Backbone.Model.extend({
  defaults: {
    north: undefined,
    east: undefined,
    west: undefined,
    south: undefined,
  },
})
Draw.BboxView = Marionette.View.extend({
  initialize(options) {
    this.map = options.map
    this.listenTo(
      this.model,
      'change:mapNorth change:mapSouth change:mapEast change:mapWest',
      this.updateGeometry
    )
  },
  setModelFromGeometry(geometry) {
    const extent = geometry.getExtent()

    const northWest = ol.proj.transform(
      [extent[0], extent[3]],
      properties.projection,
      'EPSG:4326'
    )
    const southEast = ol.proj.transform(
      [extent[2], extent[1]],
      properties.projection,
      'EPSG:4326'
    )
    this.model.set({
      north: DistanceUtils.coordinateRound(northWest[1]),
      south: DistanceUtils.coordinateRound(southEast[1]),
      west: DistanceUtils.coordinateRound(northWest[0]),
      east: DistanceUtils.coordinateRound(southEast[0]),
    })
  },

  modelToRectangle(model) {
    //ensure that the values are numeric
    //so that the openlayer projections
    //do not fail
    const north = parseFloat(model.get('mapNorth'))
    const south = parseFloat(model.get('mapSouth'))
    let east = parseFloat(model.get('mapEast'))
    let west = parseFloat(model.get('mapWest'))

    // If we are crossing the date line, we must go outside [-180, 180]
    // for openlayers to draw correctly. This means we can't draw boxes
    // that encompass more than half the world. This actually matches
    // how the backend searches anyway.
    if (east - west < -180) {
      east += 360
    } else if (east - west > 180) {
      west += 360
    }

    const northWest = ol.proj.transform(
      [west, north],
      'EPSG:4326',
      properties.projection
    )
    const northEast = ol.proj.transform(
      [east, north],
      'EPSG:4326',
      properties.projection
    )
    const southWest = ol.proj.transform(
      [west, south],
      'EPSG:4326',
      properties.projection
    )
    const southEast = ol.proj.transform(
      [east, south],
      'EPSG:4326',
      properties.projection
    )

    const coords = []
    coords.push(northWest)
    coords.push(northEast)
    coords.push(southEast)
    coords.push(southWest)
    coords.push(northWest)
    const rectangle = new ol.geom.LineString(coords)
    return rectangle
  },

  updatePrimitive(model) {
    const rectangle = this.modelToRectangle(model)
    // make sure the current model has width and height before drawing
    if (
      rectangle &&
      !_.isUndefined(rectangle) &&
      (model.get('north') !== model.get('south') &&
        model.get('east') !== model.get('west'))
    ) {
      this.drawBorderedRectangle(rectangle)
      //only call this if the mouse button isn't pressed, if we try to draw the border while someone is dragging
      //the filled in shape won't show up
      if (!this.buttonPressed) {
        this.drawBorderedRectangle(rectangle)
      }
    }
  },

  updateGeometry(model) {
    const rectangle = this.modelToRectangle(model)
    if (rectangle) {
      this.drawBorderedRectangle(rectangle)
    }
  },

  drawBorderedRectangle(rectangle) {
    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }

    if (!rectangle) {
      // handles case where model changes to empty vars and we don't want to draw anymore
      return
    }

    this.billboard = new ol.Feature({
      geometry: rectangle,
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

  handleRegionStop() {
    const geometry = olUtils.wrapCoordinatesFromGeometry(
      this.primitive.getGeometry()
    )
    this.setModelFromGeometry(geometry)
    this.updateGeometry(this.model)
    this.listenTo(
      this.model,
      'change:mapNorth change:mapSouth change:mapEast change:mapWest',
      this.updateGeometry
    )

    this.model.trigger('EndExtent', this.model)
    wreqr.vent.trigger('search:bboxdisplay', this.model)
  },
  start() {
    const that = this
    this.primitive = new ol.interaction.DragBox({
      condition: ol.events.condition.always,
      style: new ol.style.Style({
        stroke: new ol.style.Stroke({
          color: [0, 0, 255, 0],
        }),
      }),
    })

    this.map.addInteraction(this.primitive)
    this.primitive.on('boxend', () => {
      that.handleRegionStop()
      that.map.removeInteraction(that.primitive)
    })
    this.primitive.on('boxstart', sketchFeature => {
      that.startCoordinate = sketchFeature.coordinate
    })
    this.primitive.on('boxdrag', sketchFeature => {
      const geometryRepresentation = new ol.geom.LineString([
        that.startCoordinate,
        [that.startCoordinate[0], sketchFeature.coordinate[1]],
        sketchFeature.coordinate,
        [sketchFeature.coordinate[0], that.startCoordinate[1]],
        that.startCoordinate,
      ])
      that.drawBorderedRectangle(geometryRepresentation)
      that.setModelFromGeometry(that.primitive.getGeometry())
    })
  },
  startCoordinate: undefined,
  stop() {
    this.stopListening()
  },

  destroyPrimitive() {
    if (this.primitive) {
      this.map.removeInteraction(this.primitive)
    }
    if (this.vectorLayer) {
      this.map.removeLayer(this.vectorLayer)
    }
  },
})

Draw.Controller = DrawingController.extend({
  drawingType: 'bbox',
  show(model) {
    if (this.enabled) {
      const bboxModel = model || new Draw.BboxModel()

      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.destroyPrimitive()
        existingView.updatePrimitive(model)
      } else {
        const view = new Draw.BboxView({
          map: this.options.map,
          model: bboxModel,
        })
        view.updatePrimitive(model)
        this.addView(view)
      }

      return bboxModel
    }
  },
  draw(model) {
    if (this.enabled) {
      const bboxModel = model || new Draw.BboxModel()
      const view = new Draw.BboxView({
        map: this.options.map,
        model: bboxModel,
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
      bboxModel.trigger('BeginExtent')
      this.listenToOnce(bboxModel, 'EndExtent', function() {
        this.notificationView.destroy()
      })

      return bboxModel
    }
  },
})

module.exports = Draw
