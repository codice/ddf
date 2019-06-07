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

const withAdapter = Component =>
  class extends React.Component {
    constructor(props) {
      super(props)
      this.state = props.model.toJSON()
    }
    setModelState() {
      this.setState(this.props.model.toJSON())
    }
    componentWillMount() {
      this.props.model.on('change', this.setModelState, this)
    }
    componentWillUnmount() {
      this.props.model.off('change', this.setModelState)
    }
    render() {
      return (
        <Component
          state={this.state}
          options={this.props.options}
          setState={(...args) => this.props.model.set(...args)}
        />
      )
    }
  }

const LocationView = withAdapter(
  require('../../react-component/location/index.js')
)

const Marionette = require('marionette')
const _ = require('underscore')
const wreqr = require('../../js/wreqr.js')
const store = require('../../js/store.js')
const CustomElements = require('../../js/CustomElements.js')
const LocationOldModel = require('./location-old')
const CQLUtils = require('../../js/CQLUtils.js')
const ShapeUtils = require('../../js/ShapeUtils.js')
const { Direction } = require('../location-new/utils/dms-utils.js')
import { deserialize } from './location-serialization'
const wkx = require('wkx')

const minimumDifference = 0.0001
const minimumBuffer = 0.000001

const filterToLocationOldModel = filter => {
  if (filter === '') return filter

  if (typeof filter.geojson === 'object') {
    return deserialize(filter.geojson)
  }

  const filterValue =
    typeof filter.value === 'object' ? filter.value.value : filter.value

  // for backwards compatability with wkt
  if (filterValue && typeof filterValue === 'string') {
    const geometry = wkx.Geometry.parse(filterValue).toGeoJSON()
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

module.exports = Marionette.LayoutView.extend({
  template() {
    const props = {
      model: this.model,
      options: {
        onDraw: drawingType => {
          wreqr.vent.trigger('search:draw' + this.model.get('mode'), this.model)
        },
      },
    }
    return <LocationView {...props} />
  },
  tagName: CustomElements.register('location-old'),
  regions: {
    location: '.location-input',
  },
  initialize(options) {
    this.propertyModel = this.model
    this.model = new LocationOldModel()
    _.bindAll.apply(_, [this].concat(_.functions(this))) // underscore bindAll does not take array arg
    this.deserialize()
    this.setupListeners()
    this.listenTo(this.model, 'change', this.updateMap)
    this.listenTo(this.model, 'change:polygon', () => {
      if (this.model.get('mode') !== 'poly') {
        wreqr.vent.trigger('search:polydisplay', this.model)
      }
    })
    this.listenTo(this.model, 'change:mode', () => {
      this.clearLocation()
    })
  },
  // Updates the map with a drawing whenever the user is entering coordinates manually
  updateMap() {
    if (!this.isDestroyed) {
      const mode = this.model.get('mode')
      if (mode !== undefined && store.get('content').get('drawing') !== true) {
        wreqr.vent.trigger('search:' + mode + 'display', this.model)
      }
    }
  },
  setupListeners() {
    this.listenTo(
      this.model,
      'change:mapNorth change:mapSouth change:mapEast change:mapWest',
      this.updateMaxAndMin
    )
  },
  updateMaxAndMin() {
    this.model.setLatLon()
  },
  isFilterUndefinedOrNull(filter) {
    if (
      filter === null ||
      filter.value === undefined ||
      filter.value === null
    ) {
      return true
    }
    return false
  },
  deserialize() {
    if (this.propertyModel) {
      const filter = this.propertyModel.get('value')
      if (this.isFilterUndefinedOrNull(filter)) {
        return
      }
    }

    const filter = this.propertyModel.get('value')
    this.model.set(filterToLocationOldModel(filter))

    switch (filter.type) {
      // these cases are for when the model matches the filter model
      case 'DWITHIN':
        if (CQLUtils.isPointRadiusFilter(filter)) {
          wreqr.vent.trigger('search:circledisplay', this.model)
        } else if (CQLUtils.isPolygonFilter(filter)) {
          wreqr.vent.trigger('search:polydisplay', this.model)
        } else {
          wreqr.vent.trigger('search:linedisplay', this.model)
        }
        break
      case 'INTERSECTS':
        wreqr.vent.trigger('search:polydisplay', this.model)
        break
      // these cases are for when the model matches the location model
      case 'BBOX':
        wreqr.vent.trigger('search:bboxdisplay', this.model)
        break
      case 'MULTIPOLYGON':
      case 'POLYGON':
        wreqr.vent.trigger('search:polydisplay', this.model)
        break
      case 'POINTRADIUS':
        wreqr.vent.trigger('search:circledisplay', this.model)
        break
      case 'LINE':
        wreqr.vent.trigger('search:linedisplay', this.model)
        break
    }
  },
  clearLocation() {
    this.model.set({
      north: undefined,
      east: undefined,
      west: undefined,
      south: undefined,
      dmsNorth: '',
      dmsSouth: '',
      dmsEast: '',
      dmsWest: '',
      dmsNorthDirection: Direction.North,
      dmsSouthDirection: Direction.North,
      dmsEastDirection: Direction.East,
      dmsWestDirection: Direction.East,
      lat: undefined,
      lon: undefined,
      dmsLat: '',
      dmsLon: '',
      dmsLatDirection: Direction.North,
      dmsLonDirection: Direction.East,
      radius: 1,
      bbox: undefined,
      polygon: undefined,
      hasKeyword: false,
      usng: undefined,
      usngbb: undefined,
      utmUpsEasting: undefined,
      utmUpsNorthing: undefined,
      utmUpsZone: 1,
      utmUpsHemisphere: 'Northern',
      utmUpsUpperLeftEasting: undefined,
      utmUpsUpperLeftNorthing: undefined,
      utmUpsUpperLeftZone: 1,
      utmUpsUpperLeftHemisphere: 'Northern',
      utmUpsLowerRightEasting: undefined,
      utmUpsLowerRightNorthing: undefined,
      utmUpsLowerRightZone: 1,
      utmUpsLowerRightHemisphere: 'Northern',
      line: undefined,
      lineWidth: 1,
    })
    wreqr.vent.trigger('search:drawend', this.model)
    this.$el.trigger('change')
  },
  getCurrentValue() {
    const modelJSON = this.model.toJSON()
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

    return _.extend(modelJSON, {
      type,
      lineWidth: Math.max(modelJSON.lineWidth, minimumBuffer),
      radius: Math.max(modelJSON.radius, minimumBuffer),
    })
  },
  onDestroy() {
    wreqr.vent.trigger('search:drawend', this.model)
  },
  isValid() {
    return this.getCurrentValue().type != undefined
  },
})
