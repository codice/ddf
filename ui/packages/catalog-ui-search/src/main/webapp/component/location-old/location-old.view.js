/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
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

const minimumDifference = 0.0001
const minimumBuffer = 0.000001

module.exports = Marionette.LayoutView.extend({
  template: function() {
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
  initialize: function(options) {
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
  updateMap: function() {
    if (!this.isDestroyed) {
      var mode = this.model.get('mode')
      if (mode !== undefined && store.get('content').get('drawing') !== true) {
        wreqr.vent.trigger('search:' + mode + 'display', this.model)
      }
    }
  },
  setupListeners: function() {
    this.listenTo(
      this.model,
      'change:mapNorth change:mapSouth change:mapEast change:mapWest',
      this.updateMaxAndMin
    )
  },
  updateMaxAndMin: function() {
    this.model.setLatLon()
  },
  deserialize: function() {
    if (this.propertyModel) {
      var filter = this.propertyModel.get('value')
      if(filter === null || filter.value == undefined || filter.value == null){
        return
      }
      var filterValue =
        typeof filter.value === 'object' ? filter.value.value : filter.value
      switch (filter.type) {
        // these cases are for when the model matches the filter model
        case 'DWITHIN':
          if (CQLUtils.isPointRadiusFilter(filter)) {
            let pointText = filterValue.substring(6)
            pointText = pointText.substring(0, pointText.length - 1)
            var latLon = pointText.split(' ')
            this.model.set({
              mode: 'circle',
              locationType: 'latlon',
              lat: latLon[1],
              lon: latLon[0],
              radius: filter.distance,
            })
            wreqr.vent.trigger('search:circledisplay', this.model)
          } else if (CQLUtils.isPolygonFilter(filter)) {
            this.handlePolygonDeserialization(filter)
          } else {
            let pointText = filterValue.substring(11)
            pointText = pointText.substring(0, pointText.length - 1)
            this.model.set({
              mode: 'line',
              lineWidth: filter.distance,
              line: pointText.split(',').map(function(coordinate) {
                return coordinate.split(' ').map(function(value) {
                  return Number(value)
                })
              }),
            })
            wreqr.vent.trigger('search:linedisplay', this.model)
          }
          break
        case 'INTERSECTS':
          if (!filterValue || typeof filterValue !== 'string') {
            break
          }
          this.handlePolygonDeserialization({
            polygon: CQLUtils.arrayFromPolygonWkt(filterValue),
          })
          break
        // these cases are for when the model matches the location model
        case 'BBOX':
          this.model.set({
            mode: 'bbox',
            locationType: 'latlon',
            north: filter.north,
            south: filter.south,
            east: filter.east,
            west: filter.west,
          })
          wreqr.vent.trigger('search:bboxdisplay', this.model)
          break
        case 'MULTIPOLYGON':
        case 'POLYGON':
          this.handlePolygonDeserialization(filter)
          break
        case 'POINTRADIUS':
          this.model.set({
            mode: 'circle',
            locationType: 'latlon',
            lat: filter.lat,
            lon: filter.lon,
            radius: filter.radius,
          })
          wreqr.vent.trigger('search:circledisplay', this.model)
          break
        case 'LINE':
          this.model.set({
            mode: 'line',
            line: filter.line,
            lineWidth: filter.lineWidth,
          })
          wreqr.vent.trigger('search:linedisplay', this.model)
          break
      }
    }
  },
  handlePolygonDeserialization(filter) {
    const polygonArray =
      (filter.value &&
        filter.value.value &&
        CQLUtils.arrayFromPolygonWkt(filter.value.value)) ||
      []
    const bufferWidth = filter.polygonBufferWidth || filter.distance

    this.model.set({
      mode: 'poly',
      polygon: filter.polygon || polygonArray,
      ...(bufferWidth && { polygonBufferWidth: bufferWidth }),
    })
    wreqr.vent.trigger('search:polydisplay', this.model)
  },
  clearLocation: function() {
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
  getCurrentValue: function() {
    var modelJSON = this.model.toJSON()
    var type
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
      type: type,
      lineWidth: Math.max(modelJSON.lineWidth, minimumBuffer),
      radius: Math.max(modelJSON.radius, minimumBuffer),
    })
  },
  onDestroy: function() {
    wreqr.vent.trigger('search:drawend', this.model)
  },
  isValid: function() {
    return this.getCurrentValue().type != undefined
  },
})
