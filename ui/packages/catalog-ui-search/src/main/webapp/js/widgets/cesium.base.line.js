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
const Cesium = require('cesium')
const _ = require('underscore')
const wreqr = require('../wreqr.js')
const NotificationView = require('./notification.view')
const DrawingController = require('./drawing.controller')

const CAMERA_MAGNITUDE_THRESHOLD = 8000000

const capitalize = value => value.charAt(0).toUpperCase() + value.slice(1)

const getCurrentMagnitudeFromView = view => {
  return view.map.camera.getMagnitude()
}

const needsRedraw = view => {
  const currentMagnitude = getCurrentMagnitudeFromView(view)

  if (
    view.cameraMagnitude < CAMERA_MAGNITUDE_THRESHOLD &&
    currentMagnitude > CAMERA_MAGNITUDE_THRESHOLD
  ) {
    return true
  }
  if (
    view.cameraMagnitude > CAMERA_MAGNITUDE_THRESHOLD &&
    currentMagnitude < CAMERA_MAGNITUDE_THRESHOLD
  ) {
    return true
  }

  return false
}

class GeometryRenderView extends Marionette.View {
  constructor(options) {
    super(...options)

    Object.assign(this, { ...options })
  }

  updatePrimitive = () => {
    this.drawGeometry(this.model)
  }

  drawGeometry = model => {
    throw new Error('This method must be overwritten by implementing geometry!')
  }

  constructLinePrimitive = coordinates => {
    const color = this.model.get('color')

    return {
      width: 8,
      material: Cesium.Material.fromType('PolylineOutline', {
        color: color
          ? Cesium.Color.fromCssColorString(color)
          : Cesium.Color.KHAKI,
        outlineColor: Cesium.Color.WHITE,
        outlineWidth: 4,
      }),
      id: 'userDrawing',
      positions: Cesium.Cartesian3.fromDegreesArray(_.flatten(coordinates)),
    }
  }

  constructDottedLinePrimitive = coordinates => {
    const color = this.model.get('color')

    return {
      width: 4,
      material: Cesium.Material.fromType('PolylineDash', {
        color: color
          ? Cesium.Color.fromCssColorString(color)
          : Cesium.Color.KHAKI,
        dashLength: 16.0,
        dashPattern: 7.0,
      }),
      id: 'userDrawing',
      positions: Cesium.Cartesian3.fromDegreesArray(_.flatten(coordinates)),
    }
  }

  listenForCameraChange = () => {
    this.map.scene.camera.moveStart.addEventListener(
      this.handleCameraMoveStart,
      this
    )
    this.map.scene.camera.moveEnd.addEventListener(
      this.handleCameraMoveEnd,
      this
    )
  }

  handleCameraMoveStart = () => {
    this.animationFrameId = window.requestAnimationFrame(
      function() {
        if (needsRedraw(this)) {
          this.updatePrimitive()
        }
        this.handleCameraMoveStart()
      }.bind(this)
    )
  }

  handleCameraMoveEnd = () => {
    window.cancelAnimationFrame(this.animationFrameId)
    if (needsRedraw(this)) {
      this.updatePrimitive()
    }
  }

  destroy = () => {
    if (!this.map || !this.map.scene) {
      return
    }

    this.map.scene.camera.moveStart.removeEventListener(
      this.handleCameraMoveStart,
      this
    )
    this.map.scene.camera.moveEnd.removeEventListener(
      this.handleCameraMoveEnd,
      this
    )
    window.cancelAnimationFrame(this.animationFrameId)
    if (this.primitive) {
      this.map.scene.primitives.remove(this.primitive)
    }
    this.remove() // backbone cleanup.
  }
}

class GeometryController extends DrawingController {
  constructor(options) {
    DrawingController.prototype.drawingType = options.drawingType
    DrawingController.prototype.options = options

    super(...options)

    Object.assign(this, { ...options })
  }

  show = model => {
    if (!this.enabled) {
      return
    }
    this.drawHelper.stopDrawing()
    // remove old polygon
    const existingView = this.getViewForModel(model)
    if (existingView) {
      existingView.destroy()
      this.removeViewForModel(model)
    }

    this.addView(this.createRenderView(model))
  }

  createRenderView = model => {
    throw new Error('This method must be implemented by the subclass!')
  }

  draw = model => {
    const controller = this
    const toDeg = Cesium.Math.toDegrees
    if (!this.enabled) {
      return
    }
    // start geometry draw.
    this.notificationView = new NotificationView({
      el: this.notificationEl,
    }).render()

    this.drawHelper[`startDrawing${capitalize(this.geometry)}`]({
      callback: positions => {
        if (controller.notificationView) {
          controller.notificationView.destroy()
        }
        const latLonRadPoints = _.map(positions, function(cartPos) {
          const latLon = controller.map.scene.globe.ellipsoid.cartesianToCartographic(
            cartPos
          )
          return [toDeg(latLon.longitude), toDeg(latLon.latitude)]
        })

        //this shouldn't ever get hit because the draw library should protect against it, but just in case it does, remove the point
        if (
          latLonRadPoints.length > 3 &&
          latLonRadPoints[latLonRadPoints.length - 1][0] ===
            latLonRadPoints[latLonRadPoints.length - 2][0] &&
          latLonRadPoints[latLonRadPoints.length - 1][1] ===
            latLonRadPoints[latLonRadPoints.length - 2][1]
        ) {
          latLonRadPoints.pop()
        }

        model.set(controller.geometryModel, latLonRadPoints)

        // doing this out of formality since bbox/circle call this after drawing has ended.
        model.trigger('EndExtent', model)

        // lets go ahead and show our new shiny polygon.
        wreqr.vent.trigger(`search:${controller.geometryDisplay}`, model)
      },
    })
  }
}

module.exports = {
  GeometryRenderView,
  GeometryController,
  CAMERA_MAGNITUDE_THRESHOLD,
}
