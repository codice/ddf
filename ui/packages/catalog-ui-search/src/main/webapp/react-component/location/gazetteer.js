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
const React = require('react')
const Keyword = require('./keyword.js')
const properties = require('../../js/properties.js')

import fetch from '../../react-component/utils/fetch'

const onlineGazetteer = properties.onlineGazetteer

const getLargestBbox = (polygonCoordinates, isMultiPolygon) => {
  let finalMax = { x: Number.MIN_SAFE_INTEGER, y: Number.MIN_SAFE_INTEGER }
  let finalMin = { x: Number.MAX_SAFE_INTEGER, y: Number.MAX_SAFE_INTEGER }
  const boundingBoxLimit = 75
  let encompassingBoundingBox = {
    maxX: Number.MIN_SAFE_INTEGER,
    minX: Number.MAX_SAFE_INTEGER,
    maxY: Number.MIN_SAFE_INTEGER,
    minY: Number.MAX_SAFE_INTEGER,
  }
  let maxArea = -1
  let currentArea = -1
  let currentMax
  let currentMin
  polygonCoordinates.map(rowCoordinates => {
    currentMax = { x: Number.MIN_SAFE_INTEGER, y: Number.MIN_SAFE_INTEGER }
    currentMin = { x: Number.MAX_SAFE_INTEGER, y: Number.MAX_SAFE_INTEGER }
    if (isMultiPolygon) {
      rowCoordinates[0].map(coordinates => {
        currentMax.x = Math.max(coordinates[0], currentMax.x)
        currentMax.y = Math.max(coordinates[1], currentMax.y)
        currentMin.x = Math.min(coordinates[0], currentMin.x)
        currentMin.y = Math.min(coordinates[1], currentMin.y)
        encompassingBoundingBox.maxX = Math.max(
          coordinates[0],
          encompassingBoundingBox.maxX
        )
        encompassingBoundingBox.maxY = Math.max(
          coordinates[1],
          encompassingBoundingBox.maxY
        )
        encompassingBoundingBox.minX = Math.min(
          coordinates[0],
          encompassingBoundingBox.minX
        )
        encompassingBoundingBox.minY = Math.min(
          coordinates[1],
          encompassingBoundingBox.minY
        )
      })
    } else {
      rowCoordinates.map(coordinates => {
        currentMax.x = Math.max(coordinates[0], currentMax.x)
        currentMax.y = Math.max(coordinates[1], currentMax.y)
        currentMin.x = Math.min(coordinates[0], currentMin.x)
        currentMin.y = Math.min(coordinates[1], currentMin.y)
        encompassingBoundingBox.maxX = Math.max(
          coordinates[0],
          encompassingBoundingBox.maxX
        )
        encompassingBoundingBox.maxY = Math.max(
          coordinates[1],
          encompassingBoundingBox.maxY
        )
        encompassingBoundingBox.minX = Math.min(
          coordinates[0],
          encompassingBoundingBox.minX
        )
        encompassingBoundingBox.minY = Math.min(
          coordinates[1],
          encompassingBoundingBox.minY
        )
      })
    }
    currentArea = (currentMax.x - currentMin.x) * (currentMax.y - currentMin.y)
    if (currentArea > maxArea) {
      maxArea = currentArea
      finalMax = currentMax
      finalMin = currentMin
    }
  })
  const encompassingBoundingBoxHeight =
    encompassingBoundingBox.maxY - encompassingBoundingBox.minY
  const encompassingBoundingBoxWidth =
    encompassingBoundingBox.maxX - encompassingBoundingBox.minX
  return encompassingBoundingBoxWidth >= boundingBoxLimit ||
    encompassingBoundingBoxHeight >= boundingBoxLimit
    ? {
        maxX: finalMax.x,
        minX: finalMin.x,
        maxY: finalMax.y,
        minY: finalMin.y,
      }
    : encompassingBoundingBox
}

class Gazetteer extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
    this.fetch = this.props.fetch || fetch
  }
  expandPoint(geo) {
    const offset = 0.1
    if (geo.length === 1) {
      const point = geo[0]
      return [
        {
          lat: point.lat + offset,
          lon: point.lon + offset,
        },
        {
          lat: point.lat + offset,
          lon: point.lon - offset,
        },
        {
          lat: point.lat - offset,
          lon: point.lon - offset,
        },
        {
          lat: point.lat - offset,
          lon: point.lon + offset,
        },
      ]
    }
    return geo
  }
  extractGeo(suggestion) {
    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [
          this.expandPoint(suggestion.geo).map(coord => [coord.lon, coord.lat]),
        ],
      },
      properties: {},
      id: suggestion.id,
    }
  }
  async suggesterWithLiteralSupport(input) {
    const res = await this.fetch(
      `./internal/geofeature/suggestions?q=${encodeURIComponent(input)}`
    )
    return await res.json()
  }
  async geofeatureWithLiteralSupport(suggestion) {
    if (suggestion.id.startsWith('LITERAL')) {
      return this.extractGeo(suggestion)
    }
    const { id } = suggestion
    const res = await this.fetch(`./internal/geofeature?id=${id}`)
    const data = await res.json()
    const finalArea = getLargestBbox(
      data.geometry.coordinates,
      this.isMultiPolygon(data.geometry.coordinates)
    )
    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [finalArea.minX, finalArea.minY],
            [finalArea.maxX, finalArea.minY],
            [finalArea.maxX, finalArea.maxY],
            [finalArea.minX, finalArea.maxY],
          ],
        ],
      },
      properties: {},
      id: data.display_name,
    }
  }
  getOsmTypeSymbol(type) {
    switch (type) {
      case 'node':
        return 'N'
      case 'way':
        return 'W'
      case 'relation':
        return 'R'
      default:
        throw 'Unexpected OSM type ' + type
    }
  }
  async suggester(input) {
    const res = await window.fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(
        input
      )}`
    )
    const suggestions = await res.json()
    return suggestions.map(place => {
      return {
        id: this.getOsmTypeSymbol(place.osm_type) + ':' + place.osm_id,
        name: place.display_name,
      }
    })
  }
  isMultiPolygon(coordinates) {
    return (
      coordinates[0][0][0] !== undefined &&
      coordinates[0][0][0][0] !== undefined
    )
  }
  async geofeature(suggestion) {
    const [type, id] = suggestion.id.split(':')
    const res = await window.fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&osm_type=${type}&osm_id=${id}&polygon_geojson=1`
    )
    const data = await res.json()
    const boundingBoxLimit = 75
    const boundingBoxWidth = data.boundingbox[3] - data.boundingbox[2]
    const boundingBoxHeight = data.boundingbox[1] - data.boundingbox[0]
    if (
      (boundingBoxWidth >= boundingBoxLimit ||
        boundingBoxHeight >= boundingBoxLimit) &&
      Object.keys(data.address).length === 2
    ) {
      const finalArea = getLargestBbox(
        data.geojson.coordinates,
        this.isMultiPolygon(data.geojson.coordinates)
      )
      data.boundingbox[0] = finalArea.minY
      data.boundingbox[1] = finalArea.maxY
      data.boundingbox[2] = finalArea.minX
      data.boundingbox[3] = finalArea.maxX
    }
    return {
      type: 'Feature',
      geometry: {
        type: 'Polygon',
        coordinates: [
          [
            [data.boundingbox[2], data.boundingbox[0]],
            [data.boundingbox[3], data.boundingbox[0]],
            [data.boundingbox[3], data.boundingbox[1]],
            [data.boundingbox[2], data.boundingbox[1]],
          ],
        ],
      },
      properties: {},
      id: data.display_name,
    }
  }
  render() {
    return (
      <div>
        {onlineGazetteer ? (
          <Keyword
            setState={this.props.setState}
            suggester={input => this.suggester(input)}
            geofeature={suggestItem => this.geofeature(suggestItem)}
          />
        ) : (
          <Keyword
            setState={this.props.setState}
            suggester={input => this.suggesterWithLiteralSupport(input)}
            geofeature={suggestItem =>
              this.geofeatureWithLiteralSupport(suggestItem)
            }
          />
        )}
      </div>
    )
  }
}

module.exports = Gazetteer
module.exports.getLargestBbox = getLargestBbox
