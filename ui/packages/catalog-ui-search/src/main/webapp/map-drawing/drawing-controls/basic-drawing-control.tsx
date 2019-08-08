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
import DrawingControl from './drawing-control'
import { Shape } from '../shape-utils'
import {
  GeometryJSON,
  GeometryJSONProperties,
  makeBufferedGeo,
  makeEmptyGeometry,
  geoToExtent,
} from '../geometry'

type GeoProps = GeometryJSONProperties & {
  [index: string]: any
}

abstract class BasicDrawingControl implements DrawingControl {
  context: DrawingContext
  receiver: UpdatedGeoReceiver
  geoFormat: ol.format.GeoJSON
  mouseDragActive: boolean
  drawingActive: boolean
  protected properties: GeoProps

  abstract startDrawing(geoJSON: GeometryJSON): void

  protected constructor(context: DrawingContext, receiver: UpdatedGeoReceiver) {
    this.context = context
    this.receiver = receiver
    this.geoFormat = new ol.format.GeoJSON()
    this.mouseDragActive = false
    this.setProperties(makeEmptyGeometry('', this.getShape()).properties)
  }

  setProperties(properties: GeoProps): void {
    this.properties = {
      ...properties,
      shape: this.getShape(),
    }
  }

  getProperties(): GeoProps {
    return this.properties
  }

  protected applyPropertiesToFeature(feature: ol.Feature) {
    if (this.properties.id) {
      feature.setId(this.properties.id)
    }
    Object.keys(this.properties).forEach(key => {
      if (key !== 'id') {
        feature.set(key, this.properties[key])
      }
    })
  }

  protected abstract getShape(): Shape

  protected abstract getGeoType(): ol.geom.GeometryType

  protected featureToGeo(feature: ol.Feature): GeometryJSON {
    // @ts-ignore openlayers GeoJSON type incompatibility
    return this.geoFormat.writeFeatureObject(feature) as GeometryJSON
  }

  protected writeExtendedGeoJSON(feature: ol.Feature): GeometryJSON {
    const shape = this.getShape()
    const geo = this.featureToGeo(feature)
    const bufferedGeo = makeBufferedGeo({
      ...geo,
      properties: {
        ...geo.properties,
        shape,
      },
    } as GeometryJSON)
    return {
      ...geo,
      bbox: geoToExtent(bufferedGeo),
      properties: {
        ...this.properties,
        shape,
      },
    }
  }

  cancelDrawing(): void {
    this.context.removeListeners()
    this.context.removeInteractions()
    this.drawingActive = false
  }

  setActive(active: boolean): void {
    this.context.setInteractionsActive(active)
  }

  isActive() {
    return this.context.areInteractionsActive()
  }

  isMouseDragActive(): boolean {
    return this.mouseDragActive
  }

  isDrawing(): boolean {
    return this.drawingActive
  }
}

export default BasicDrawingControl
