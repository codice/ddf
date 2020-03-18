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
import React from 'react'
import withListenTo from '../../backbone-container'
const LocationView = require('../../location/index.js')
const LocationOldModel = require('../../../component/location-old/location-old')
const ShapeUtils = require('../../../js/ShapeUtils.js')
const CQLUtils = require('../../../js/CQLUtils.js')
const wreqr = require('../../../js/wreqr.js')
const wkx = require('wkx')
import { Drawing } from '../../../component/singletons/drawing'
import { deserialize } from '../../../component/location-old/location-serialization'

const typesToDisplays = {
  BBOX: 'bboxdisplay',
  MULTIPOLYGON: 'polydisplay',
  POLYGON: 'polydisplay',
  POINTRADIUS: 'circledisplay',
  LINE: 'linedisplay',
}

const filterToLocationOldModel = filter => {
  if (typeof filter.geojson === 'object') {
    return deserialize(filter.geojson)
  }

  // for backwards compatability with wkt
  if (filter.value && typeof filter.value === 'string') {
    const geometry = wkx.Geometry.parse(filter.value).toGeoJSON()
    return deserialize({
      type: 'Feature',
      geometry,
      properties: {
        type: geometry.type,
        buffer: {
          width: filter.distance,
          unit: 'meters',
        },
      },
    })
  }
}

const minimumBuffer = 0.000001

class LocationInput extends React.Component {
  locationModel
  constructor(props) {
    super(props)
    this.locationModel = new LocationOldModel()
    this.state = this.locationModel.toJSON()
    this.deserialize()
    this.onChange()
  }
  setModelState() {
    this.setState(this.locationModel.toJSON())
    this.onChange()
  }
  componentWillMount() {
    this.locationModel.on('change', this.setModelState, this)
  }
  componentDidMount() {
    this.props.listenTo(
      this.locationModel,
      'change:mapNorth change:mapSouth change:mapEast change:mapWest',
      this.locationModel.setLatLon
    )
    this.props.listenTo(this.locationModel, 'change', this.updateMap)
    this.props.listenTo(this.locationModel, 'change:polygon', () => {
      if (this.locationModel.get('mode') !== 'poly') {
        wreqr.vent.trigger('search:polydisplay', this.locationModel)
      }
    })
    this.props.listenTo(this.locationModel, 'change:mode', () => {
      this.clearLocation()
    })
  }
  componentWillUnmount() {
    this.locationModel.off('change', this.setModelState)
    wreqr.vent.trigger('search:drawend', this.locationModel)
  }
  updateMap = () => {
    const mode = this.locationModel.get('mode')
    if (mode !== undefined && Drawing.isDrawing() !== true) {
      wreqr.vent.trigger('search:' + mode + 'display', this.locationModel)
    }
  }
  deserialize = () => {
    const filter = this.props.value
    if (!filter) {
      return
    }

    this.locationModel.set(filterToLocationOldModel(filter))

    switch (filter.type) {
      // these cases are for when the model matches the filter model
      case 'DWITHIN':
        if (CQLUtils.isPointRadiusFilter(filter.value)) {
          wreqr.vent.trigger('search:circledisplay', this.locationModel)
        } else if (CQLUtils.isPolygonFilter(filter.value)) {
          wreqr.vent.trigger('search:polydisplay', this.locationModel)
        } else {
          wreqr.vent.trigger('search:linedisplay', this.locationModel)
        }
        break
      case 'INTERSECTS':
        wreqr.vent.trigger('search:polydisplay', this.locationModel)
        break
      // these cases are for when the model matches the location model
      default:
        wreqr.vent.trigger(
          `search:${typesToDisplays[filter.type]}`,
          this.locationModel
        )
        break
    }
  }
  clearLocation() {
    this.locationModel.set(new LocationOldModel().toJSON())
    wreqr.vent.trigger('search:drawend', this.locationModel)
    this.setState(this.locationModel.toJSON())
  }
  getCurrentValue() {
    const modelJSON = this.locationModel.toJSON()
    let type
    if (modelJSON.polygon !== undefined) {
      type = ShapeUtils.isArray3D(modelJSON.polygon)
        ? 'MULTIPOLYGON'
        : 'POLYGON'
    } else if (
      modelJSON.lat !== undefined &&
      modelJSON.lon !== undefined &&
      modelJSON.radius !== undefined
    ) {
      type = 'POINTRADIUS'
    } else if (
      modelJSON.line !== undefined &&
      modelJSON.lineWidth !== undefined
    ) {
      type = 'LINE'
    } else if (
      modelJSON.north !== undefined &&
      modelJSON.south !== undefined &&
      modelJSON.east !== undefined &&
      modelJSON.west !== undefined
    ) {
      type = 'BBOX'
    }

    return Object.assign(modelJSON, {
      type,
      lineWidth: Math.max(modelJSON.lineWidth, minimumBuffer),
      radius: Math.max(modelJSON.radius, minimumBuffer),
    })
  }
  onChange = () => {
    const value = this.getCurrentValue()
    this.props.onChange(value)
  }
  render() {
    const options = {
      onDraw: drawingType => {
        wreqr.vent.trigger(
          'search:draw' + this.locationModel.get('mode'),
          this.locationModel
        )
      },
    }
    return (
      <LocationView
        state={this.state}
        options={options}
        setState={(...args) => this.locationModel.set(...args)}
      />
    )
  }
}

export default withListenTo(LocationInput)
