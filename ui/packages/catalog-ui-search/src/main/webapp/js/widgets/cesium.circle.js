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
const Cesium = require('cesium')
const _ = require('underscore')
const wreqr = require('../wreqr.js')
const NotificationView = require('./notification.view')
const Turf = require('@turf/turf')
const TurfCircle = require('@turf/circle')
const DrawingController = require('./drawing.controller')
const DistanceUtils = require('../DistanceUtils.js')

const DrawCircle = {}

DrawCircle.CircleModel = Backbone.Model.extend({
  defaults: {
    lat: undefined,
    lon: undefined,
    radius: undefined,
  },
})
const defaultAttrs = ['lat', 'lon', 'radius']
DrawCircle.CircleView = Marionette.View.extend({
  initialize() {
    this.mouseHandler = new Cesium.ScreenSpaceEventHandler(
      this.options.map.scene.canvas
    )

    this.listenTo(
      this.model,
      'change:lat change:lon change:radius change:radiusUnits',
      this.updatePrimitive
    )
    this.updatePrimitive(this.model)
  },
  enableInput() {
    const controller = this.options.map.scene.screenSpaceCameraController
    controller.enableTranslate = true
    controller.enableZoom = true
    controller.enableRotate = true
    controller.enableTilt = true
    controller.enableLook = true
  },
  disableInput() {
    const controller = this.options.map.scene.screenSpaceCameraController
    controller.enableTranslate = false
    controller.enableZoom = false
    controller.enableRotate = false
    controller.enableTilt = false
    controller.enableLook = false
  },

  setCircleRadius(mn, mx) {
    const startCartographic = this.options.map.scene.globe.ellipsoid.cartographicToCartesian(
        mn
      ),
      stopCart = this.options.map.scene.globe.ellipsoid.cartographicToCartesian(
        mx
      ),
      radius = Math.abs(Cesium.Cartesian3.distance(startCartographic, stopCart))

    const modelProp = {
      lat: DistanceUtils.coordinateRound((mn.latitude * 180) / Math.PI),
      lon: DistanceUtils.coordinateRound((mn.longitude * 180) / Math.PI),
      radius: DistanceUtils.coordinateRound(
        DistanceUtils.getDistanceFromMeters(
          radius,
          this.model.get('radiusUnits')
        )
      ),
    }

    this.model.set(modelProp)
  },

  isModelReset(modelProp) {
    if (
      _.every(defaultAttrs, val => _.isUndefined(modelProp[val])) ||
      _.isEmpty(modelProp)
    ) {
      return true
    }
    return false
  },

  updatePrimitive(model) {
    const modelProp = model.toJSON()
    if (this.isModelReset(modelProp)) {
      this.options.map.scene.primitives.remove(this.primitive)
      this.stopListening()
      return
    }

    if (modelProp.radius === 0 || isNaN(modelProp.radius)) {
      modelProp.radius = 1
    }

    this.drawBorderedCircle(model)
  },
  drawBorderedCircle(model) {
    // if model has been reset

    const modelProp = model.toJSON()

    // first destroy old one
    if (this.primitive && !this.primitive.isDestroyed()) {
      this.options.map.scene.primitives.remove(this.primitive)
    }

    if (
      this.isModelReset(modelProp) ||
      modelProp.lat === undefined ||
      modelProp.lon === undefined
    ) {
      return
    }

    const color = this.model.get('color')

    const centerPt = Turf.point([modelProp.lon, modelProp.lat])
    const circleToCheck = new TurfCircle(
      centerPt,
      DistanceUtils.getDistanceInMeters(
        modelProp.radius,
        modelProp.radiusUnits
      ),
      64,
      'meters'
    )

    this.primitive = new Cesium.PolylineCollection()
    this.primitive.add({
      width: 8,
      material: Cesium.Material.fromType('PolylineOutline', {
        color: color
          ? Cesium.Color.fromCssColorString(color)
          : Cesium.Color.KHAKI,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 4,
      }),
      id: 'userDrawing',
      positions: Cesium.Cartesian3.fromDegreesArray(
        _.flatten(circleToCheck.geometry.coordinates)
      ),
    })

    this.options.map.scene.primitives.add(this.primitive)
  },
  handleRegionStop() {
    this.enableInput()
    if (!this.mouseHandler.isDestroyed()) {
      this.mouseHandler.destroy()
    }
    this.drawBorderedCircle(this.model)
    this.stopListening(
      this.model,
      'change:lat change:lon change:radius',
      this.updatePrimitive
    )
    this.listenTo(
      this.model,
      'change:lat change:lon change:radius',
      this.drawBorderedCircle
    )
    this.model.trigger('EndExtent', this.model)
    wreqr.vent.trigger('search:circledisplay', this.model)
  },
  handleRegionInter(movement) {
    let cartesian = this.options.map.scene.camera.pickEllipsoid(
        movement.endPosition,
        this.options.map.scene.globe.ellipsoid
      ),
      cartographic
    if (cartesian) {
      cartographic = this.options.map.scene.globe.ellipsoid.cartesianToCartographic(
        cartesian
      )
      this.setCircleRadius(this.click1, cartographic)
    }
  },
  handleRegionStart(movement) {
    const cartesian = this.options.map.scene.camera.pickEllipsoid(
        movement.position,
        this.options.map.scene.globe.ellipsoid
      ),
      that = this
    if (cartesian) {
      this.click1 = this.options.map.scene.globe.ellipsoid.cartesianToCartographic(
        cartesian
      )
      this.mouseHandler.setInputAction(() => {
        that.handleRegionStop()
      }, Cesium.ScreenSpaceEventType.LEFT_UP)
      this.mouseHandler.setInputAction(movement => {
        that.handleRegionInter(movement)
      }, Cesium.ScreenSpaceEventType.MOUSE_MOVE)
    }
  },
  start() {
    this.disableInput()

    const that = this

    // Now wait for start
    this.mouseHandler.setInputAction(movement => {
      that.handleRegionStart(movement)
    }, Cesium.ScreenSpaceEventType.LEFT_DOWN)
  },
  stop() {
    this.stopListening()
    this.enableInput()
  },

  drawStop() {
    this.enableInput()
    this.mouseHandler.destroy()
  },

  destroyPrimitive() {
    if (!this.mouseHandler.isDestroyed()) {
      this.mouseHandler.destroy()
    }
    if (this.primitive && !this.primitive.isDestroyed()) {
      this.options.map.scene.primitives.remove(this.primitive)
    }
  },
  destroy() {
    this.destroyPrimitive()
    this.remove() // backbone cleanup.
  },
})

DrawCircle.Controller = DrawingController.extend({
  drawingType: 'circle',
  show(model) {
    if (this.enabled) {
      const circleModel = model || new DrawCircle.CircleModel()

      const existingView = this.getViewForModel(model)
      if (existingView) {
        existingView.drawStop()
        existingView.destroyPrimitive()
        existingView.updatePrimitive(model)
      } else {
        const view = new DrawCircle.CircleView({
          map: this.options.map,
          model: circleModel,
        })
        view.updatePrimitive(model)
        this.addView(view)
      }

      return circleModel
    }
  },
  draw(model) {
    if (this.enabled) {
      const circleModel = model || new DrawCircle.CircleModel()
      const view = new DrawCircle.CircleView({
        map: this.options.map,
        model: circleModel,
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
        el: this.options.notificationEl,
      }).render()
      this.listenToOnce(circleModel, 'EndExtent', function() {
        this.notificationView.destroy()
      })

      return circleModel
    }
  },
})

module.exports = DrawCircle
