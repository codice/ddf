import * as turf from '@turf/turf'
import { GeometryJSON, LengthUnit, Extent, bboxToExtent } from '../geometry'
const DistanceUtils = require('../../js/DistanceUtils.js')

const updateCircleGeo = (
  geo: GeometryJSON,
  lat: number,
  lon: number,
  radius: number,
  radiusUnit: LengthUnit
): GeometryJSON => {
  const center: [number, number] = [lon, lat]
  let bbox: Extent = [lon, lat, lon, lat]
  if (radius > 0) {
    const meters = DistanceUtils.getDistanceInMeters(radius, radiusUnit)
    const circle = turf.circle(center, meters, { units: 'meters' })
    bbox = bboxToExtent(turf.bbox(circle))
  }
  return {
    ...geo,
    geometry: {
      ...geo.geometry,
      coordinates: center,
    } as GeoJSON.Point,
    bbox,
    properties: {
      ...geo.properties,
      buffer: radius,
      bufferUnit: radiusUnit,
    },
  }
}

export { updateCircleGeo }
