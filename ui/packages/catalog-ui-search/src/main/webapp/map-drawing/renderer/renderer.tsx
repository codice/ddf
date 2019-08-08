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
import { GeometryJSON, Extent } from '../geometry'
import { makeBufferedGeo } from '../geometry'

type Renderable = {
  geo: GeometryJSON
}

class Renderer {
  map: ol.Map
  vectorLayer: ol.layer.Vector
  geoFormat: ol.format.GeoJSON
  maxZoom: number

  constructor(
    map: ol.Map,
    style: ol.style.Style | ol.StyleFunction | ol.style.Style[],
    maxZoom: number
  ) {
    this.map = map
    this.geoFormat = new ol.format.GeoJSON()
    this.maxZoom = maxZoom
    const vectorSource = new ol.source.Vector({
      features: [],
    })
    this.vectorLayer = new ol.layer.Vector({
      source: vectorSource,
      zIndex: 1,
    })
    this.vectorLayer.setStyle(style)
    this.map.addLayer(this.vectorLayer)
  }
  renderList(geometryList: Renderable[]): void {
    for (const geometry of geometryList) {
      this.addGeo(geometry)
    }
  }
  makeGeometryFeature(geometry: Renderable): ol.Feature {
    const buffered = makeBufferedGeo(geometry.geo)
    return this.geoFormat.readFeature(buffered)
  }
  addGeo(geometry: Renderable): void {
    const feature = this.makeGeometryFeature(geometry)
    feature.setId(geometry.geo.properties.id)
    // Note: In the future we may want to optimize performance
    // here by using feature ids to update only what has
    // changed and remove only what has been removed.
    this.vectorLayer.getSource().addFeature(feature)
  }
  clearGeos(): void {
    this.vectorLayer.getSource().clear()
  }
  panToGeo(geometry: Renderable) {
    this.panToExtent(this.getExtent(geometry))
  }
  panToGeoList(geometryList: Renderable[]) {
    if (geometryList.length > 0) {
      let minX = Number.MAX_SAFE_INTEGER
      let minY = Number.MAX_SAFE_INTEGER
      let maxX = Number.MIN_SAFE_INTEGER
      let maxY = Number.MIN_SAFE_INTEGER
      geometryList.forEach((geometry: Renderable) => {
        const extent = this.getExtent(geometry)
        minX = Math.min(minX, extent[0])
        minY = Math.min(minY, extent[1])
        maxX = Math.max(maxX, extent[2])
        maxY = Math.max(maxY, extent[3])
      })
      this.panToExtent([minX, minY, maxX, maxY])
    }
  }
  panToExtent(extent: Extent) {
    this.map.getView().fit(extent, {
      size: this.map.getSize(),
      duration: 500,
      maxZoom: this.maxZoom,
    })
  }
  protected getExtent(geometry: Renderable): Extent {
    if (geometry.geo.bbox) {
      return geometry.geo.bbox
    } else {
      const feature = this.geoFormat.readFeature(geometry.geo)
      return feature.getGeometry().getExtent()
    }
  }
  resizeMap() {
    this.map.updateSize()
  }
}

export default Renderer
