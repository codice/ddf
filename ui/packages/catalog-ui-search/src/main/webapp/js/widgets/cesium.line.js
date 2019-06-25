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

const Cesium = require('cesium')
const _ = require('underscore')
const Turf = require('@turf/turf')
const DistanceUtils = require('../DistanceUtils.js')

const {
  GeometryRenderView,
  GeometryController,
  CAMERA_MAGNITUDE_THRESHOLD,
} = require('./cesium.base.line')

class LineRenderView extends GeometryRenderView {
  constructor(options) {
    super({ ...options, modelEvents: { changed: 'updatePrimitive' } })

    this.cameraMagnitude
    this.animationFrameId

    this.init({
      ...options,
      events: 'change:line change:lineWidth change:lineUnits',
    })
  }

  init = options => {
    const { model, events } = { ...options }

    this.updatePrimitive()
    this.listenTo(model, events, this.updatePrimitive)
    this.listenForCameraChange()
  }

  drawGeometry = model => {
    const json = model.toJSON()
    let linePoints = json.line
    const lineWidth =
      DistanceUtils.getDistanceInMeters(
        json.lineWidth,
        model.get('lineUnits')
      ) || 1
    if (!linePoints) {
      return
    }

    linePoints.forEach(point => {
      point[0] = DistanceUtils.coordinateRound(point[0])
      point[1] = DistanceUtils.coordinateRound(point[1])
    })

    const setArr = _.uniq(linePoints)
    if (setArr.length < 2) {
      return
    }

    const turfLine = Turf.lineString(setArr)
    let bufferedLine = turfLine
    this.cameraMagnitude = this.map.camera.getMagnitude()
    if (lineWidth > 100 || this.cameraMagnitude < CAMERA_MAGNITUDE_THRESHOLD) {
      bufferedLine = Turf.buffer(turfLine, Math.max(lineWidth, 1), 'meters')
    }

    // first destroy old one
    if (this.primitive && !this.primitive.isDestroyed()) {
      this.map.scene.primitives.remove(this.primitive)
    }

    this.primitive = new Cesium.PolylineCollection()
    this.primitive.add(
      this.constructLinePrimitive(bufferedLine.geometry.coordinates)
    )

    this.map.scene.primitives.add(this.primitive)
  }
}

class LineController extends GeometryController {
  constructor(options) {
    super({
      ...options,
      drawingType: 'line',
      geometry: 'polyline',
      geometryModel: 'line',
      geometryDisplay: 'linedisplay',
    })
  }

  createRenderView = model => {
    return new LineRenderView({ model, map: this.map })
  }
}

module.exports = {
  LineRenderView,
  Controller: LineController,
}
