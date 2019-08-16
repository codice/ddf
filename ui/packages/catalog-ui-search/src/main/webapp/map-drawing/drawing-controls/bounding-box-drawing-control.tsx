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
import * as turf from '@turf/turf'
import DrawingContext from './drawing-context'
import UpdatedGeoReceiver from './geo-receiver'
import BasicDrawingControl from './basic-drawing-control'
import { Shape } from '../shape-utils'
import { GeometryJSON, Extent } from '../geometry'

type ExtentEvent = {
  extent: Extent
}

class BoundingBoxDrawingControl extends BasicDrawingControl {
  constructor(context: DrawingContext, receiver: UpdatedGeoReceiver) {
    super(context, receiver)
    this.extentChanged = this.extentChanged.bind(this)
  }

  getGeoType(): ol.geom.GeometryType {
    return 'Polygon'
  }

  getShape(): Shape {
    return 'Bounding Box'
  }

  startDrawing(geoJSON: GeometryJSON): void {
    const feature = this.geoFormat.readFeature(geoJSON)
    const extent = feature.getGeometry().getExtent()
    this.setProperties((geoJSON as GeometryJSON).properties || {})
    this.startDrawingExtent(extent)
  }

  startDrawingExtent(extent: Extent): void {
    this.drawingActive = true
    const geoJSON = this.extentToGeoJSON(extent)
    const feature = this.geoFormat.readFeature(geoJSON)
    // @ts-ignore ol.interaction.Extent is not in typescript for this version of Open Layers
    const draw = new ol.interaction.Extent({
      extent: feature.getGeometry().getExtent(),
    })
    this.applyPropertiesToFeature(feature)
    if (!this.isEmptyFeature(feature)) {
      this.context.updateFeature(feature)
      this.context.updateBufferFeature(feature)
    }
    this.context.setDrawInteraction(draw)
    this.context.setEvent('draw', 'extentchanged', this.extentChanged)
    this.context.addInteractionsWithoutModify()
  }

  extentChanged(e: ExtentEvent): void {
    if (e.extent !== null) {
      this.receiver(this.extentToGeoJSON(e.extent))
      const feature = this.extentToFeature(e.extent)
      this.applyPropertiesToFeature(feature)
      this.context.updateFeature(feature)
      this.context.updateBufferFeature(feature)
    }
  }

  extentToFeature(extent: Extent): ol.Feature {
    return this.geoFormat.readFeature(this.extentToGeoJSON(extent))
  }

  extentToGeoJSON(bbox: Extent): GeometryJSON {
    const bboxPolygon = turf.bboxPolygon(bbox)
    return {
      bbox,
      type: 'Feature',
      properties: {
        ...this.properties,
        shape: this.getShape(),
      },
      geometry: bboxPolygon.geometry as GeoJSON.Polygon,
    }
  }
}

export default BoundingBoxDrawingControl
