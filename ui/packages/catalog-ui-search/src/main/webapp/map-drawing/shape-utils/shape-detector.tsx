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
import { GeometryJSON } from '../geometry'
import Shape from './shape'

class ShapeDetector {
  geoFormat: ol.format.GeoJSON

  constructor() {
    this.geoFormat = new ol.format.GeoJSON()
  }

  shapeFromGeoJSON(geoJSON: GeometryJSON): Shape {
    const feature = this.geoFormat.readFeature(geoJSON)
    return this.shapeFromFeature(feature)
  }

  shapeFromFeature(feature: ol.Feature): Shape {
    if (this.isLineFeature(feature)) {
      return 'Line'
    } else if (this.isBoundingBoxFeature(feature)) {
      return 'Bounding Box'
    } else if (this.isPointFeature(feature)) {
      return 'Point'
    } else if (this.isPointRadiusFeature(feature)) {
      return 'Point Radius'
    } else {
      return 'Polygon'
    }
  }

  isLineFeature(feature: ol.Feature): boolean {
    return feature.getGeometry().getType() === 'LineString'
  }

  isPointFeature(feature: ol.Feature): boolean {
    return (
      feature.getGeometry().getType() === 'Point' &&
      (feature.get('buffer') === undefined || feature.get('buffer') <= 0)
    )
  }

  isBoundingBoxFeature(feature: ol.Feature): boolean {
    if (!this.isPolygonFeature(feature)) {
      return false
    } else {
      const coordinates = (feature.getGeometry() as ol.geom.Polygon).getCoordinates()[0]
      const extent = feature.getGeometry().getExtent()
      const expectedCoordinates = (turf.bboxPolygon(extent)
        .geometry as turf.Polygon).coordinates[0] as [number, number][]
      return (
        coordinates.length === 5 &&
        expectedCoordinates.every(expectedPoint =>
          coordinates.some(
            point =>
              point[0] === expectedPoint[0] && point[1] === expectedPoint[1]
          )
        )
      )
    }
  }

  isPointRadiusFeature(feature: ol.Feature): boolean {
    return (
      feature.getGeometry().getType() === 'Point' && feature.get('buffer') > 0
    )
  }

  isPolygonFeature(feature: ol.Feature): boolean {
    return feature.getGeometry().getType() === 'Polygon'
  }
}

export default ShapeDetector
