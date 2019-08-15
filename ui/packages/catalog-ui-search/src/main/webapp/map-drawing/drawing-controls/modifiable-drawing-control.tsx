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
import BasicDrawingControl from './basic-drawing-control'
import { GeometryJSON } from '../geometry'

abstract class ModifiableDrawingControl extends BasicDrawingControl {
  protected constructor(context: DrawingContext, receiver: UpdatedGeoReceiver) {
    super(context, receiver)
    this.onCompleteDrawing = this.onCompleteDrawing.bind(this)
    this.onStartDrawing = this.onStartDrawing.bind(this)
    this.onCompleteModify = this.onCompleteModify.bind(this)
  }

  getGeoJSONFromCompleteDrawEvent(e: any): GeometryJSON {
    return this.writeExtendedGeoJSON(e.feature)
  }

  getGeoJSONFromCompleteModifyEvent(e: any): GeometryJSON {
    return this.writeExtendedGeoJSON(e.features.getArray()[0])
  }

  onCompleteDrawing(e: any) {
    const geoJSON = this.getGeoJSONFromCompleteDrawEvent(e)
    this.mouseDragActive = false
    const feature = this.makeFeature(geoJSON)
    this.applyPropertiesToFeature(feature)
    this.context.updateFeature(feature)
    this.context.updateBufferFeature(feature)
    this.receiver(geoJSON)
  }

  onStartDrawing(_e: any) {
    this.mouseDragActive = true
  }

  onCompleteModify(e: any) {
    this.mouseDragActive = true
    this.context.updateBufferFeature(e.features.getArray()[0])
    this.receiver(this.getGeoJSONFromCompleteModifyEvent(e))
  }

  makeFeature(geoJSON: GeometryJSON): ol.Feature {
    const feature = this.geoFormat.readFeature(geoJSON)
    if (feature.getGeometry().getType() !== this.getGeoType()) {
      throw new Error(
        `Wrong geometry type! expected ${this.getGeoType()} but got ${feature
          .getGeometry()
          .getType()} instead.`
      )
    }
    return feature
  }

  getStaticStyle(feature: ol.Feature): ol.style.Style | ol.style.Style[] {
    const style = this.context.getStyle()
    if (typeof style === 'function') {
      return style(feature, 1)
    } else {
      return style
    }
  }

  startDrawing(geoJSON: GeometryJSON) {
    this.drawingActive = true
    this.setProperties((geoJSON as GeometryJSON).properties || {})
    const feature = this.makeFeature(geoJSON)
    this.applyPropertiesToFeature(feature)
    if (!this.isEmptyFeature(feature)) {
      this.context.updateFeature(feature)
      this.context.updateBufferFeature(feature)
    }
    const draw = new ol.interaction.Draw({
      type: this.getGeoType(),
      style: this.getStaticStyle(feature),
    })
    this.context.setDrawInteraction(draw)
    this.context.setEvent('draw', 'drawend', this.onCompleteDrawing)
    this.context.setEvent('draw', 'drawstart', this.onStartDrawing)
    this.context.setEvent('modify', 'modifyend', this.onCompleteModify)
    this.context.addInteractions()
  }
}

export default ModifiableDrawingControl
