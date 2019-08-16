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
import { GeometryJSON, makeBufferedGeo } from '../geometry'

type EventListener = (e: any) => void

type ListenerTarget = 'draw' | 'snap' | 'modify'

type ListenerRecord = {
  target: ListenerTarget
  event: string
  listener: EventListener
}

class DrawingContext {
  map: ol.Map
  drawLayer: ol.layer.Vector
  bufferLayer: ol.layer.Vector
  modify: ol.interaction.Modify
  snap: ol.interaction.Snap
  draw: ol.interaction.Interaction | null
  listenerList: ListenerRecord[]
  style: ol.style.Style | ol.StyleFunction | ol.style.Style[]
  geoFormat: ol.format.GeoJSON
  animationFrameId: number

  constructor({
    map,
    drawingStyle,
  }: {
    map: ol.Map
    drawingStyle: ol.style.Style | ol.StyleFunction | ol.style.Style[]
  }) {
    this.bufferUpdateEvent = this.bufferUpdateEvent.bind(this)
    this.animationFrameId = 0
    this.geoFormat = new ol.format.GeoJSON()
    this.style = drawingStyle
    this.draw = null
    this.listenerList = []
    this.map = map
    this.drawLayer = new ol.layer.Vector({
      source: new ol.source.Vector(),
      style: drawingStyle,
      zIndex: 2,
      updateWhileInteracting: true,
    })
    this.bufferLayer = new ol.layer.Vector({
      source: new ol.source.Vector(),
      style: drawingStyle,
      zIndex: 1,
    })
    this.map.addLayer(this.bufferLayer)
    this.map.addLayer(this.drawLayer)
    this.modify = new ol.interaction.Modify({
      source: this.drawLayer.getSource(),
    })
    this.snap = new ol.interaction.Snap({
      source: this.drawLayer.getSource(),
    })
  }

  getStyle(): ol.style.Style | ol.StyleFunction | ol.style.Style[] {
    return this.style
  }

  updateFeature(feature: ol.Feature): void {
    this.drawLayer.getSource().clear()
    this.drawLayer.getSource().addFeature(feature)
  }

  updateBufferFeature(feature: ol.Feature): void {
    this.bufferLayer.getSource().clear()
    const buffer: number | undefined = feature.get('buffer')
    if (buffer !== undefined && buffer > 0) {
      const geo: GeometryJSON = JSON.parse(this.geoFormat.writeFeature(feature))
      const bufferedGeo = makeBufferedGeo(geo)
      const bufferFeature = this.geoFormat.readFeature(bufferedGeo)
      this.bufferLayer.getSource().addFeature(bufferFeature)
      this.map.on('pointerdrag', this.bufferUpdateEvent)
    }
  }

  protected bufferUpdateEvent() {
    const featureList = this.drawLayer.getSource().getFeatures()
    if (featureList.length) {
      const feature = featureList[0]
      this.animationFrameId = requestAnimationFrame(() => {
        this.updateBufferFeature(feature)
      })
    }
  }

  setDrawInteraction(draw: ol.interaction.Interaction): void {
    this.draw = draw
  }

  setEvent(
    target: ListenerTarget,
    event: string,
    listener: EventListener
  ): void {
    const listenerTarget = this[target]
    if (listenerTarget !== null) {
      listenerTarget.on(event, listener)
      this.listenerList.push({
        target,
        event,
        listener,
      })
    }
  }

  removeListeners(): void {
    for (const listener of this.listenerList) {
      const listenerTarget = this[listener.target]
      if (listenerTarget !== null) {
        listenerTarget.un(listener.event, listener.listener)
      }
    }
    this.listenerList = []
    cancelAnimationFrame(this.animationFrameId)
    this.map.un('pointerdrag', this.bufferUpdateEvent)
  }

  addInteractions(): void {
    if (this.draw !== null) {
      this.map.addInteraction(this.draw)
    }
    this.map.addInteraction(this.snap)
    this.map.addInteraction(this.modify)
  }

  addInteractionsWithoutModify(): void {
    if (this.draw !== null) {
      this.map.addInteraction(this.draw)
    }
    this.map.addInteraction(this.snap)
  }

  removeInteractions(): void {
    this.map.removeInteraction(this.modify)
    this.map.removeInteraction(this.snap)
    if (this.draw !== null) {
      this.map.removeInteraction(this.draw)
    }
    this.drawLayer.getSource().clear()
    this.bufferLayer.getSource().clear()
  }

  remakeInteractions(): void {
    this.modify = new ol.interaction.Modify({
      source: this.drawLayer.getSource(),
    })
    this.snap = new ol.interaction.Snap({
      source: this.drawLayer.getSource(),
    })
  }

  setInteractionsActive(active: boolean): void {
    this.modify.setActive(active)
    this.snap.setActive(active)
    if (this.draw) {
      this.draw.setActive(active)
    }
  }

  areInteractionsActive(): boolean {
    return (
      (this.draw === null || this.draw.getActive()) &&
      this.modify.getActive() &&
      this.snap.getActive()
    )
  }

  circleRadiusToMeters(radius: number): number {
    return (
      radius *
      this.map
        .getView()
        .getProjection()
        .getMetersPerUnit()
    )
  }
}

export default DrawingContext
