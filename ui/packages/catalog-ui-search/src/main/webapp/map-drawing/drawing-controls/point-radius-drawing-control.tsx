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
import * as ol from 'openlayers'
import DrawingContext from './drawing-context'
import UpdatedGeoReceiver from './geo-receiver'
import ModifiableDrawingControl from './modifiable-drawing-control'
import { Shape } from '../shape-utils'
import { GeometryJSON, METERS, KILOMETERS } from '../geometry'
const DistanceUtils = require('../../js/DistanceUtils.js')

class PointRadiusDrawingControl extends ModifiableDrawingControl {
  animationFrameId: number
  animationFrame: () => void

  constructor(context: DrawingContext, receiver: UpdatedGeoReceiver) {
    super(context, receiver)
    this.animationFrameId = 0
    this.animationFrame = () => {}
  }

  onCompleteDrawing(e: any) {
    cancelAnimationFrame(this.animationFrameId)
    this.animationFrame = () => {}
    super.onCompleteDrawing(e)
  }

  onStartDrawing(e: any) {
    super.onStartDrawing(e)
    if (this.properties.buffer) {
      this.animationFrame = () => {
        const geoJSON = this.getGeoJSONFromCompleteDrawEvent(e)
        const feature = this.makeFeature(geoJSON)
        this.applyPropertiesToFeature(feature)
        this.context.updateBufferFeature(feature)
        this.animationFrameId = requestAnimationFrame(this.animationFrame)
      }
      this.animationFrame()
    }
  }

  getShape(): Shape {
    return 'Point Radius'
  }

  getGeoJSONFromCompleteDrawEvent(e: any): GeometryJSON {
    return this.featureToGeoJSON(e.feature)
  }

  getGeoJSONFromCompleteModifyEvent(e: any): GeometryJSON {
    const feature = e.features.getArray()[0]
    return this.featureToGeoJSON(feature)
  }

  featureToGeoJSON(inputFeature: ol.Feature): GeometryJSON {
    let point: ol.geom.Point
    const geometry = inputFeature.getGeometry()
    const bufferUnit = this.properties.bufferUnit || METERS
    let radius = DistanceUtils.getDistanceInMeters(
      this.properties.buffer || 0,
      bufferUnit
    )
    if (geometry.getType() === 'Point') {
      point = geometry as ol.geom.Point
    } else {
      const circle = geometry as ol.geom.Circle
      point = new ol.geom.Point(circle.getCenter())
      radius = this.context.circleRadiusToMeters(circle.getRadius())
    }
    const feature = new ol.Feature(point)
    let bestFitRadiusUnit = bufferUnit
    if (bestFitRadiusUnit === METERS && radius > 1000) {
      bestFitRadiusUnit = KILOMETERS
    }
    this.setProperties({
      ...this.properties,
      buffer: DistanceUtils.getDistanceFromMeters(radius, bestFitRadiusUnit),
      bufferUnit: bestFitRadiusUnit,
    })
    const json: GeometryJSON = this.writeExtendedGeoJSON(
      feature
    ) as GeometryJSON
    return json
  }

  makeFeature(geoJSON: GeometryJSON): ol.Feature {
    const feature = this.geoFormat.readFeature(geoJSON)
    const geometry = feature.getGeometry()
    let point: ol.geom.Point
    if (geometry.getType() === 'Point') {
      point = geometry as ol.geom.Point
    } else {
      const circle = geometry as ol.geom.Circle
      point = new ol.geom.Point(circle.getCenter())
      feature.set(
        'buffer',
        DistanceUtils.getDistanceFromMeters(
          this.context.circleRadiusToMeters(circle.getRadius()),
          feature.get('bufferUnit') || METERS
        )
      )
    }
    return new ol.Feature(point)
  }

  getGeoType(): ol.geom.GeometryType {
    return 'Circle'
  }

  getStaticStyle(_feature: ol.Feature): ol.style.Style | ol.style.Style[] {
    const circleFeature = new ol.Feature(new ol.geom.Circle([0, 0], 1))
    this.applyPropertiesToFeature(circleFeature)
    const style = this.context.getStyle()
    if (typeof style === 'function') {
      return style(circleFeature, 1)
    } else {
      return style
    }
  }
}

export default PointRadiusDrawingControl
