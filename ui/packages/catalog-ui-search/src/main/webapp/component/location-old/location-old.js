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
/*global define*/
const _ = require('underscore')
const Backbone = require('backbone')
const usngs = require('usng.js')
const store = require('../../js/store.js')
const Common = require('../../js/Common.js')
const dmsUtils = require('../location-new/utils/dms-utils.js')

var converter = new usngs.Converter()
var minimumDifference = 0.0001
var minimumBuffer = 0.000001
var utmUpsLocationType = 'utmUps'
// offset used by utmUps for southern hemisphere
const utmUpsBoundaryNorth = 84
const utmUpsBoundarySouth = -80
var northingOffset = 10000000
var usngPrecision = 6
const Direction = dmsUtils.Direction

function convertToValid(key, model) {
  if (
    key.mapSouth !== undefined &&
    (key.mapSouth >= key.mapNorth ||
      (key.mapNorth === undefined && key.mapSouth >= model.get('mapNorth')))
  ) {
    key.mapSouth =
      parseFloat(key.mapNorth || model.get('mapNorth')) - minimumDifference
  }
  if (
    key.mapNorth !== undefined &&
    (key.mapNorth <= key.mapSouth ||
      (key.mapSouth === undefined && key.mapNorth <= model.get('mapSouth')))
  ) {
    key.mapNorth =
      parseFloat(key.mapSouth || model.get('mapSouth')) + minimumDifference
  }
  if (key.mapNorth !== undefined) {
    key.mapNorth = Math.max(-90, key.mapNorth)
    key.mapNorth = Math.min(90, key.mapNorth)
  }
  if (key.mapSouth !== undefined) {
    key.mapSouth = Math.max(-90, key.mapSouth)
    key.mapSouth = Math.min(90, key.mapSouth)
  }
  if (key.mapWest !== undefined) {
    key.mapWest = Math.max(-180, key.mapWest)
    key.mapWest = Math.min(180, key.mapWest)
  }
  if (key.mapEast !== undefined) {
    key.mapEast = Math.max(-180, key.mapEast)
    key.mapEast = Math.min(180, key.mapEast)
  }
  if (key.lat !== undefined) {
    key.lat = Math.max(-90, key.lat)
    key.lat = Math.min(90, key.lat)
  }
  if (key.lon !== undefined) {
    key.lon = Math.max(-180, key.lon)
    key.lon = Math.min(180, key.lon)
  }
  if (key.radius !== undefined) {
    key.radius = Math.max(minimumBuffer, key.radius)
  }
  if (key.lineWidth !== undefined) {
    key.lineWidth = Math.max(minimumBuffer, key.lineWidth)
  }
  if (key.polygonBufferWidth) {
    key.polygonBufferWidth = Math.max(minimumBuffer, key.polygonBufferWidth)
  }
}

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    drawing: false,
    north: undefined,
    east: undefined,
    south: undefined,
    west: undefined,
    dmsNorth: '',
    dmsSouth: '',
    dmsEast: '',
    dmsWest: '',
    dmsNorthDirection: Direction.North,
    dmsSouthDirection: Direction.North,
    dmsEastDirection: Direction.East,
    dmsWestDirection: Direction.East,
    mapNorth: undefined,
    mapEast: undefined,
    mapWest: undefined,
    mapSouth: undefined,
    radiusUnits: 'meters',
    radius: 1,
    locationType: 'latlon',
    prevLocationType: 'latlon',
    lat: undefined,
    lon: undefined,
    dmsLat: '',
    dmsLon: '',
    dmsLatDirection: Direction.North,
    dmsLonDirection: Direction.East,
    bbox: undefined,
    usngbb: undefined,
    usng: undefined,
    utmUps: undefined,
    color: undefined,
    line: undefined,
    multiline: undefined,
    lineWidth: 1,
    lineUnits: 'meters',
    polygon: undefined,
    polygonBufferWidth: 0,
    polyType: undefined,
    polygonBufferUnits: 'meters',
    hasKeyword: false,
    keywordValue: undefined,
    utmUpsUpperLeftEasting: undefined,
    utmUpsUpperLeftNorthing: undefined,
    utmUpsUpperLeftHemisphere: 'Northern',
    utmUpsUpperLeftZone: 1,
    utmUpsLowerRightEasting: undefined,
    utmUpsLowerRightNorthing: undefined,
    utmUpsLowerRightHemisphere: 'Northern',
    utmUpsLowerRightZone: 1,
    utmUpsEasting: undefined,
    utmUpsNorthing: undefined,
    utmUpsZone: 1,
    utmUpsHemisphere: 'Northern',
  },
  set: function(key, value, options) {
    if (!_.isObject(key)) {
      var keyObject = {}
      keyObject[key] = value
      key = keyObject
      value = options
    }
    convertToValid(key, this)
    Backbone.AssociatedModel.prototype.set.call(this, key, value, options)
    Common.queueExecution(
      function() {
        this.trigger('change', Object.keys(key))
      }.bind(this)
    )
  },

  initialize: function() {
    this.listenTo(
      this,
      'change:north change:south change:east change:west',
      this.setBBox
    )
    this.listenTo(
      this,
      'change:dmsNorth change:dmsNorthDirection',
      this.setBboxDmsNorth
    )
    this.listenTo(
      this,
      'change:dmsSouth change:dmsSouthDirection',
      this.setBboxDmsSouth
    )
    this.listenTo(
      this,
      'change:dmsEast change:dmsEastDirection',
      this.setBboxDmsEast
    )
    this.listenTo(
      this,
      'change:dmsWest change:dmsWestDirection',
      this.setBboxDmsWest
    )
    this.listenTo(
      this,
      'change:dmsLat change:dmsLatDirection',
      this.setRadiusDmsLat
    )
    this.listenTo(
      this,
      'change:dmsLon change:dmsLonDirection',
      this.setRadiusDmsLon
    )
    this.listenTo(this, 'change:locationType', this.handleLocationType)
    this.listenTo(this, 'change:bbox', this.setBboxLatLon)
    this.listenTo(this, 'change:lat change:lon', this.setRadiusLatLon)
    this.listenTo(this, 'change:usngbb', this.setBboxUsng)
    this.listenTo(this, 'change:usng', this.setRadiusUsng)
    this.listenTo(
      this,
      'change:utmUpsEasting change:utmUpsNorthing change:utmUpsZone change:utmUpsHemisphere',
      this.setRadiusUtmUps
    )
    this.listenTo(
      this,
      'change:utmUpsUpperLeftEasting change:utmUpsUpperLeftNorthing change:utmUpsUpperLeftZone change:utmUpsUpperLeftHemisphere change:utmUpsLowerRightEasting change:utmUpsLowerRightNorthing change:utmUpsLowerRightZone change:utmUpsLowerRightHemisphere',
      this.setBboxUtmUps
    )
    this.listenTo(this, 'EndExtent', this.notDrawing)
    this.listenTo(this, 'BeginExtent', this.drawingOn)
    if (this.get('color') === undefined && store.get('content').get('query')) {
      this.set(
        'color',
        store
          .get('content')
          .get('query')
          .get('color')
      )
    } else if (this.get('color') === undefined) {
      this.set('color', '#c89600')
    }
  },
  notDrawing: function() {
    const prevLocationType = this.get('prevLocationType')
    if (prevLocationType === 'utmUps') {
      this.set('prevLocationType', '')
      this.set('locationType', 'utmUps')
    }
    this.drawing = false
    store.get('content').turnOffDrawing()
  },

  drawingOn: function() {
    const locationType = this.get('locationType')
    if (locationType === 'utmUps') {
      this.set('prevLocationType', 'utmUps')
      this.set('locationType', 'latlon')
    }
    this.drawing = true
    store.get('content').turnOnDrawing(this)
  },

  repositionLatLonUtmUps: function(isDefined, parse, assign, clear) {
    if (isDefined(this)) {
      var utmUpsParts = parse(this)
      if (utmUpsParts !== undefined) {
        var result = this.utmUpstoLL(utmUpsParts)

        if (result !== undefined) {
          var newResult = {}
          assign(newResult, result.lat, result.lon)

          this.set(newResult)
        } else {
          clear(this)
        }
      }
    }
  },

  repositionLatLon: function() {
    if (this.get('usngbb') !== undefined) {
      try {
        var result = converter.USNGtoLL(this.get('usngbb'))
        var newResult = {}
        newResult.mapNorth = result.north
        newResult.mapSouth = result.south
        newResult.mapEast = result.east
        newResult.mapWest = result.west

        this.set(newResult)
      } catch (err) {}
    }

    this.repositionLatLonUtmUps(
      function(_this) {
        return _this.isUtmUpsUpperLeftDefined()
      },
      function(_this) {
        return _this.parseUtmUpsUpperLeft()
      },
      function(newResult, lat, lon) {
        newResult.mapNorth = lat
        newResult.mapWest = lon
      },
      function(_this) {
        return _this.clearUtmUpsUpperLeft(true)
      }
    )

    this.repositionLatLonUtmUps(
      function(_this) {
        return _this.isUtmUpsLowerRightDefined()
      },
      function(_this) {
        return _this.parseUtmUpsLowerRight()
      },
      function(newResult, lat, lon) {
        newResult.mapSouth = lat
        newResult.mapEast = lon
      },
      function(_this) {
        return _this.clearUtmUpsLowerRight(true)
      }
    )
  },

  setLatLonUtmUps: function(result, isDefined, parse, assign, clear) {
    if (
      !(
        result.north !== undefined &&
        result.south !== undefined &&
        result.west !== undefined &&
        result.east !== undefined
      ) &&
      isDefined(this)
    ) {
      var utmUpsParts = parse(_this)
      if (utmUpsParts !== undefined) {
        var utmUpsResult = this.utmUpstoLL(utmUpsParts)

        if (utmUpsResult !== undefined) {
          assign(result, utmUpsResult.lat, utmUpsResult.lon)
        } else {
          clear(this)
        }
      }
    }
  },

  setLatLon: function() {
    if (this.get('locationType') === 'latlon') {
      var result = {}
      result.north = this.get('mapNorth')
      result.south = this.get('mapSouth')
      result.west = this.get('mapWest')
      result.east = this.get('mapEast')
      if (
        !(
          result.north !== undefined &&
          result.south !== undefined &&
          result.west !== undefined &&
          result.east !== undefined
        ) &&
        this.get('usngbb')
      ) {
        try {
          result = converter.USNGtoLL(this.get('usngbb'))
        } catch (err) {}
      }

      this.setLatLonUtmUps(
        result,
        function(_this) {
          return _this.isUtmUpsUpperLeftDefined()
        },
        function(_this) {
          return _this.parseUtmUpsUpperLeft()
        },
        function(result, lat, lon) {
          result.north = lat
          result.west = lon
        },
        function(_this) {
          _this.clearUtmUpsUpperLeft(true)
        }
      )

      this.setLatLonUtmUps(
        result,
        function(_this) {
          return _this.isUtmUpsLowerRightDefined()
        },
        function(_this) {
          return _this.parseUtmUpsLowerRight()
        },
        function(result, lat, lon) {
          result.south = lat
          result.east = lon
        },
        function(_this) {
          _this.clearUtmUpsLowerRight(true)
        }
      )

      this.set(result)
    } else if (this.get('locationType') === 'dms') {
      this.setBboxDmsFromMap()
    }
  },

  setFilterBBox: function(model) {
    var north = parseFloat(model.get('north'))
    var south = parseFloat(model.get('south'))
    var west = parseFloat(model.get('west'))
    var east = parseFloat(model.get('east'))

    model.set({
      mapNorth: north,
      mapSouth: south,
      mapEast: east,
      mapWest: west,
    })
  },

  setBboxLatLon: function() {
    const north = parseFloat(this.get('north')),
      south = parseFloat(this.get('south')),
      west = parseFloat(this.get('west')),
      east = parseFloat(this.get('east'))
    if (!this.isLatLonValid(north, west) || !this.isLatLonValid(south, east)) {
      return
    }

    let utmUps = this.LLtoUtmUps(north, west)
    if (utmUps !== undefined) {
      var utmUpsParts = this.formatUtmUps(utmUps)
      this.setUtmUpsUpperLeft(utmUpsParts, !this.isLocationTypeUtmUps())
    }

    utmUps = this.LLtoUtmUps(south, east)
    if (utmUps !== undefined) {
      var utmUpsParts = this.formatUtmUps(utmUps)
      this.setUtmUpsLowerRight(utmUpsParts, !this.isLocationTypeUtmUps())
    }

    if (this.isLocationTypeUtmUps() && this.drawing) {
      this.repositionLatLon()
    }

    const lat = (north + south) / 2
    const lon = (east + west) / 2
    if (this.isInUpsSpace(lat, lon)) {
      this.set('usngbb', undefined)
      return
    }

    var usngsStr = converter.LLBboxtoUSNG(north, south, east, west)

    this.set('usngbb', usngsStr, {
      silent: this.get('locationType') !== 'usng',
    })
    if (this.get('locationType') === 'usng' && this.drawing) {
      this.repositionLatLon()
    }
  },

  setRadiusLatLon: function() {
    var lat = this.get('lat'),
      lon = this.get('lon')

    if (
      (!store.get('content').get('drawing') &&
        this.get('locationType') !== 'latlon') ||
      !this.isLatLonValid(lat, lon)
    ) {
      return
    }

    this.setRadiusDmsFromMap()

    const utmUps = this.LLtoUtmUps(lat, lon)
    if (utmUps !== undefined) {
      var utmUpsParts = this.formatUtmUps(utmUps)
      this.setUtmUpsPointRadius(utmUpsParts, true)
    } else {
      this.clearUtmUpsPointRadius(false)
    }

    if (this.isInUpsSpace(lat, lon)) {
      this.set('usng', undefined)
      return
    }

    var usngsStr = converter.LLtoUSNG(lat, lon, usngPrecision)
    this.set('usng', usngsStr, { silent: true })
  },

  setRadiusDmsLat: function() {
    this.setLatLonFromDms('dmsLat', 'dmsLatDirection', 'lat')
  },

  setRadiusDmsLon: function() {
    this.setLatLonFromDms('dmsLon', 'dmsLonDirection', 'lon')
  },

  setBboxUsng: function() {
    if (this.get('locationType') !== 'usng') {
      return
    }

    let result
    try {
      result = converter.USNGtoLL(this.get('usngbb'))
    } catch (err) {}

    if (result === undefined) {
      return
    }

    var newResult = {}
    newResult.mapNorth = result.north
    newResult.mapSouth = result.south
    newResult.mapEast = result.east
    newResult.mapWest = result.west
    this.set(newResult)
    this.set(result, { silent: true })

    var utmUps = this.LLtoUtmUps(result.north, result.west)
    if (utmUps !== undefined) {
      var utmUpsFormatted = this.formatUtmUps(utmUps)
      this.setUtmUpsUpperLeft(utmUpsFormatted, true)
    }

    var utmUps = this.LLtoUtmUps(result.south, result.east)
    if (utmUps !== undefined) {
      var utmUpsFormatted = this.formatUtmUps(utmUps)
      this.setUtmUpsLowerRight(utmUpsFormatted, true)
    }
  },

  setBBox: function() {
    //we need these to always be inferred
    //as numeric values and never as strings
    var north = parseFloat(this.get('north'))
    var south = parseFloat(this.get('south'))
    var west = parseFloat(this.get('west'))
    var east = parseFloat(this.get('east'))

    if (
      north !== undefined &&
      south !== undefined &&
      east !== undefined &&
      west !== undefined
    ) {
      this.set('bbox', [west, south, east, north].join(','), {
        silent:
          (this.get('locationType') === 'usng' ||
            this.isLocationTypeUtmUps()) &&
          !this.drawing,
      })
    }
    if (this.get('locationType') !== 'usng' && !this.isLocationTypeUtmUps()) {
      this.set({
        mapNorth: north,
        mapSouth: south,
        mapEast: east,
        mapWest: west,
      })
    }
  },

  setRadiusUsng: function() {
    var usng = this.get('usng')
    if (usng === undefined) {
      return
    }

    let result
    try {
      result = converter.USNGtoLL(usng, true)
    } catch (err) {}

    if (!isNaN(result.lat) && !isNaN(result.lon)) {
      this.set(result)

      var utmUps = this.LLtoUtmUps(result.lat, result.lon)
      if (utmUps !== undefined) {
        var utmUpsParts = this.formatUtmUps(utmUps)
        this.setUtmUpsPointRadius(utmUpsParts, true)
      }
    } else {
      this.clearUtmUpsPointRadius(true)
      this.set({
        usng: undefined,
        lat: undefined,
        lon: undefined,
        radius: 1,
      })
    }
  },

  isLatLonValid: function(lat, lon) {
    lat = parseFloat(lat)
    lon = parseFloat(lon)
    return lat > -90 && lat < 90 && lon > -180 && lon < 180
  },

  isInUpsSpace: function(lat, lon) {
    lat = parseFloat(lat)
    lon = parseFloat(lon)
    return (
      this.isLatLonValid(lat, lon) &&
      (lat < utmUpsBoundarySouth || lat > utmUpsBoundaryNorth)
    )
  },

  // This method is called when the UTM/UPS point radius coordinates are changed by the user.
  setRadiusUtmUps: function() {
    if (!this.isLocationTypeUtmUps() && !this.isUtmUpsPointRadiusDefined()) {
      return
    }

    const utmUpsParts = this.parseUtmUpsPointRadius()
    if (utmUpsParts === undefined) {
      return
    }

    const utmUpsResult = this.utmUpstoLL(utmUpsParts)
    if (utmUpsResult === undefined) {
      if (utmUpsParts.zoneNumber !== 0) {
        this.clearUtmUpsPointRadius(true)
      }
      this.set({
        lat: undefined,
        lon: undefined,
        usng: undefined,
        radius: 1,
      })
      return
    }
    this.set(utmUpsResult)

    const { lat, lon } = utmUpsResult
    if (!this.isLatLonValid(lat, lon) || this.isInUpsSpace(lat, lon)) {
      this.set({ usng: undefined })
      return
    }

    const usngsStr = converter.LLtoUSNG(lat, lon, usngPrecision)

    this.set('usng', usngsStr, { silent: true })
  },

  // This method is called when the UTM/UPS bounding box coordinates are changed by the user.
  setBboxUtmUps: function() {
    if (!this.isLocationTypeUtmUps()) {
      return
    }
    var upperLeft = undefined
    var lowerRight = undefined

    if (this.isUtmUpsUpperLeftDefined()) {
      var upperLeftParts = this.parseUtmUpsUpperLeft()
      if (upperLeftParts !== undefined) {
        upperLeft = this.utmUpstoLL(upperLeftParts)

        if (upperLeft !== undefined) {
          this.set({ mapNorth: upperLeft.lat, mapWest: upperLeft.lon })
          this.set(
            { north: upperLeft.lat, west: upperLeft.lon },
            { silent: true }
          )
        } else {
          if (upperLeftParts.zoneNumber !== 0) {
            this.clearUtmUpsUpperLeft(true)
          }
          upperLeft = undefined
          this.set({
            mapNorth: undefined,
            mapSouth: undefined,
            mapEast: undefined,
            mapWest: undefined,
            usngbb: undefined,
          })
        }
      }
    }

    if (this.isUtmUpsLowerRightDefined()) {
      var lowerRightParts = this.parseUtmUpsLowerRight()
      if (lowerRightParts !== undefined) {
        lowerRight = this.utmUpstoLL(lowerRightParts)

        if (lowerRight !== undefined) {
          this.set({ mapSouth: lowerRight.lat, mapEast: lowerRight.lon })
          this.set(
            { south: lowerRight.lat, east: lowerRight.lon },
            { silent: true }
          )
        } else {
          if (lowerRightParts.zoneNumber !== 0) {
            this.clearUtmUpsLowerRight(true)
          }
          lowerRight = undefined
          this.set({
            mapNorth: undefined,
            mapSouth: undefined,
            mapEast: undefined,
            mapWest: undefined,
            usngbb: undefined,
          })
        }
      }
    }

    if (upperLeft === undefined || lowerRight == undefined) {
      return
    }

    const lat = (upperLeft.lat + lowerRight.lat) / 2
    const lon = (upperLeft.lon + lowerRight.lon) / 2

    if (!this.isLatLonValid(lat, lon) || this.isInUpsSpace(lat, lon)) {
      this.set('usngbb', undefined)
      return
    }

    var usngsStr = converter.LLBboxtoUSNG(
      upperLeft.lat,
      lowerRight.lat,
      lowerRight.lon,
      upperLeft.lon
    )
    this.set('usngbb', usngsStr, {
      silent: this.get('locationType') === 'usng',
    })
  },

  setBboxDmsNorth: function() {
    this.setLatLonFromDms('dmsNorth', 'dmsNorthDirection', 'north')
  },

  setBboxDmsSouth: function() {
    this.setLatLonFromDms('dmsSouth', 'dmsSouthDirection', 'south')
  },

  setBboxDmsEast: function() {
    this.setLatLonFromDms('dmsEast', 'dmsEastDirection', 'east')
  },

  setBboxDmsWest: function() {
    this.setLatLonFromDms('dmsWest', 'dmsWestDirection', 'west')
  },

  setBboxDmsFromMap: function() {
    const dmsNorth = dmsUtils.ddToDmsCoordinateLat(
      this.get('mapNorth'),
      dmsUtils.getSecondsPrecision(this.get('dmsNorth'))
    )
    const dmsSouth = dmsUtils.ddToDmsCoordinateLat(
      this.get('mapSouth'),
      dmsUtils.getSecondsPrecision(this.get('dmsSouth'))
    )
    const dmsWest = dmsUtils.ddToDmsCoordinateLon(
      this.get('mapWest'),
      dmsUtils.getSecondsPrecision(this.get('dmsWest'))
    )
    const dmsEast = dmsUtils.ddToDmsCoordinateLon(
      this.get('mapEast'),
      dmsUtils.getSecondsPrecision(this.get('dmsEast'))
    )
    this.set(
      {
        dmsNorth: (dmsNorth && dmsNorth.coordinate) || '',
        dmsNorthDirection: (dmsNorth && dmsNorth.direction) || Direction.North,
        dmsSouth: (dmsSouth && dmsSouth.coordinate) || '',
        dmsSouthDirection: (dmsSouth && dmsSouth.direction) || Direction.North,
        dmsWest: (dmsWest && dmsWest.coordinate) || '',
        dmsWestDirection: (dmsWest && dmsWest.direction) || Direction.East,
        dmsEast: (dmsEast && dmsEast.coordinate) || '',
        dmsEastDirection: (dmsEast && dmsEast.direction) || Direction.East,
      },
      { silent: true }
    )
  },

  setRadiusDmsFromMap: function() {
    const dmsLat = dmsUtils.ddToDmsCoordinateLat(
      this.get('lat'),
      dmsUtils.getSecondsPrecision(this.get('dmsLat'))
    )
    const dmsLon = dmsUtils.ddToDmsCoordinateLon(
      this.get('lon'),
      dmsUtils.getSecondsPrecision(this.get('dmsLon'))
    )
    this.set(
      {
        dmsLat: (dmsLat && dmsLat.coordinate) || '',
        dmsLatDirection: (dmsLat && dmsLat.direction) || Direction.North,
        dmsLon: (dmsLon && dmsLon.coordinate) || '',
        dmsLonDirection: (dmsLon && dmsLon.direction) || Direction.East,
      },
      { silent: true }
    )
  },

  setLatLonFromDms: function(dmsCoordinateKey, dmsDirectionKey, latLonKey) {
    const coordinate = {}
    coordinate.coordinate = this.get(dmsCoordinateKey)

    const isDmsInputIncomplete =
      coordinate.coordinate && coordinate.coordinate.includes('_')
    if (isDmsInputIncomplete) {
      return
    }

    coordinate.direction = this.get(dmsDirectionKey)

    const dmsCoordinate = dmsUtils.parseDmsCoordinate(coordinate)
    if (dmsCoordinate) {
      this.set(latLonKey, dmsUtils.dmsCoordinateToDD(dmsCoordinate))
    } else {
      this.set(latLonKey, undefined)
    }
  },

  handleLocationType: function() {
    if (this.get('locationType') === 'latlon') {
      this.set({
        north: this.get('mapNorth'),
        south: this.get('mapSouth'),
        east: this.get('mapEast'),
        west: this.get('mapWest'),
      })
    } else if (this.get('locationType') === 'dms') {
      this.setBboxDmsFromMap()
      this.setRadiusDmsFromMap()
    }
  },

  // Convert Lat-Lon to UTM/UPS coordinates. Returns undefined if lat or lon is undefined or not a number.
  // Returns undefined if the underlying call to usng fails. Otherwise, returns an object with:
  //
  //   easting    : FLOAT
  //   northing   : FLOAT
  //   zoneNumber : INTEGER (>=0 and <= 60)
  //   hemisphere : STRING (NORTHERN or SOUTHERN)
  LLtoUtmUps: function(lat, lon) {
    lat = parseFloat(lat)
    lon = parseFloat(lon)
    if (!this.isLatLonValid(lat, lon)) {
      return undefined
    }

    let utmUps = converter.LLtoUTMUPSObject(lat, lon)
    const { zoneNumber, northing } = utmUps
    const isUps = zoneNumber === 0
    utmUps.northing = isUps || lat >= 0 ? northing : northing + northingOffset

    utmUps.hemisphere = lat >= 0 ? 'NORTHERN' : 'SOUTHERN'
    return utmUps
  },

  // Convert UTM/UPS coordinates to Lat-Lon. Expects an argument object with:
  //
  //   easting    : FLOAT
  //   northing   : FLOAT
  //   zoneNumber : INTEGER (>=0 and <= 60)
  //   hemisphere : STRING (NORTHERN or SOUTHERN)
  //
  // Returns an object with:
  //
  //   lat : FLOAT
  //   lon : FLOAT
  //
  // Returns undefined if the latitude is out of range.
  //
  utmUpstoLL: function(utmUpsParts) {
    const { hemisphere, zoneNumber, northing, easting } = utmUpsParts
    const northernHemisphere = hemisphere === 'NORTHERN'

    utmUpsParts = {
      ...utmUpsParts,
      northPole: northernHemisphere,
    }

    const isUps = zoneNumber === 0
    utmUpsParts.northing =
      isUps || northernHemisphere ? northing : northing - northingOffset

    const upsValidDistance = distance =>
      distance >= 800000 && distance <= 3200000
    if (isUps && (!upsValidDistance(northing) || !upsValidDistance(easting))) {
      return undefined
    }

    let { lat, lon } = converter.UTMUPStoLL(utmUpsParts)
    lon = lon % 360
    if (lon < -180) {
      lon = lon + 360
    }
    if (lon > 180) {
      lon = lon - 360
    }

    return this.isLatLonValid(lat, lon) ? { lat, lon } : undefined
  },

  // Return true if the current location type is UTM/UPS, otherwise false.
  isLocationTypeUtmUps: function() {
    return this.get('locationType') === utmUpsLocationType
  },

  // Set the model fields for the Upper-Left bounding box UTM/UPS. The arguments are:
  //
  //   utmUpsFormatted : output from the method 'formatUtmUps'
  //   silent       : BOOLEAN (true if events should be generated)
  setUtmUpsUpperLeft: function(utmUpsFormatted, silent) {
    this.set('utmUpsUpperLeftEasting', utmUpsFormatted.easting, {
      silent: silent,
    })
    this.set('utmUpsUpperLeftNorthing', utmUpsFormatted.northing, {
      silent: silent,
    })
    this.set('utmUpsUpperLeftZone', utmUpsFormatted.zoneNumber, {
      silent: silent,
    })
    this.set('utmUpsUpperLeftHemisphere', utmUpsFormatted.hemisphere, {
      silent: silent,
    })
  },

  // Set the model fields for the Lower-Right bounding box UTM/UPS. The arguments are:
  //
  //   utmUpsFormatted : output from the method 'formatUtmUps'
  //   silent       : BOOLEAN (true if events should be generated)
  setUtmUpsLowerRight: function(utmUpsFormatted, silent) {
    this.set('utmUpsLowerRightEasting', utmUpsFormatted.easting, {
      silent: silent,
    })
    this.set('utmUpsLowerRightNorthing', utmUpsFormatted.northing, {
      silent: silent,
    })
    this.set('utmUpsLowerRightZone', utmUpsFormatted.zoneNumber, {
      silent: silent,
    })
    this.set('utmUpsLowerRightHemisphere', utmUpsFormatted.hemisphere, {
      silent: silent,
    })
  },

  // Set the model fields for the Point Radius UTM/UPS. The arguments are:
  //
  //   utmUpsFormatted : output from the method 'formatUtmUps'
  //   silent       : BOOLEAN (true if events should be generated)
  setUtmUpsPointRadius: function(utmUpsFormatted, silent) {
    this.set('utmUpsEasting', utmUpsFormatted.easting, { silent: silent })
    this.set('utmUpsNorthing', utmUpsFormatted.northing, { silent: silent })
    this.set('utmUpsZone', utmUpsFormatted.zoneNumber, { silent: silent })
    this.set('utmUpsHemisphere', utmUpsFormatted.hemisphere, {
      silent: silent,
    })
  },

  clearUtmUpsPointRadius: function(silent) {
    this.set('utmUpsEasting', undefined, { silent: silent })
    this.set('utmUpsNorthing', undefined, { silent: silent })
    this.set('utmUpsZone', 1, { silent: silent })
    this.set('utmUpsHemisphere', 'Northern', { silent: silent })
  },

  clearUtmUpsUpperLeft: function(silent) {
    this.set(
      {
        utmUpsUpperLeftEasting: undefined,
        utmUpsUpperLeftNorthing: undefined,
        utmUpsUpperLeftZone: 1,
        utmUpsUpperLeftHemisphere: 'Northern',
      },
      { silent: silent }
    )
  },

  clearUtmUpsLowerRight: function(silent) {
    this.set('utmUpsLowerRightEasting', undefined, { silent: silent })
    this.set('utmUpsLowerRightNorthing', undefined, { silent: silent })
    this.set('utmUpsLowerRightZone', 1, { silent: silent })
    this.set('utmUpsLowerRightHemisphere', 'Northern', { silent: silent })
  },

  // Parse the UTM/UPS fields that come from the HTML layer. The parameters eastingRaw and northingRaw
  // are string representations of floating pointnumbers. The zoneRaw parameter is a string
  // representation of an integer in the range [0,60]. The hemisphereRaw parameters is a string
  // that should be 'Northern' or 'Southern'.
  parseUtmUps: function(eastingRaw, northingRaw, zoneRaw, hemisphereRaw) {
    var easting = parseFloat(eastingRaw)
    var northing = parseFloat(northingRaw)
    var zone = parseInt(zoneRaw)
    var hemisphere =
      hemisphereRaw === 'Northern'
        ? 'NORTHERN'
        : hemisphereRaw === 'Southern'
          ? 'SOUTHERN'
          : undefined

    if (
      !isNaN(easting) &&
      !isNaN(northing) &&
      !isNaN(zone) &&
      hemisphere !== undefined &&
      zone >= 0 &&
      zone <= 60
    ) {
      return {
        zoneNumber: zone,
        hemisphere: hemisphere,
        easting: easting,
        northing: northing,
      }
    }

    return undefined
  },

  // Format the internal representation of UTM/UPS coordinates into the form expected by the model.
  formatUtmUps: function(utmUps) {
    return {
      easting: utmUps.easting,
      northing: utmUps.northing,
      zoneNumber: utmUps.zoneNumber,
      hemisphere:
        utmUps.hemisphere === 'NORTHERN'
          ? 'Northern'
          : utmUps.hemisphere === 'SOUTHERN'
            ? 'Southern'
            : undefined,
    }
  },

  // Return true if all of the UTM/UPS upper-left model fields are defined. Otherwise, false.
  isUtmUpsUpperLeftDefined: function() {
    return (
      this.get('utmUpsUpperLeftEasting') !== undefined &&
      this.get('utmUpsUpperLeftNorthing') !== undefined &&
      this.get('utmUpsUpperLeftZone') !== undefined &&
      this.get('utmUpsUpperLeftHemisphere') !== undefined
    )
  },

  // Return true if all of the UTM/UPS lower-right model fields are defined. Otherwise, false.
  isUtmUpsLowerRightDefined: function() {
    return (
      this.get('utmUpsLowerRightEasting') !== undefined &&
      this.get('utmUpsLowerRightNorthing') !== undefined &&
      this.get('utmUpsLowerRightZone') !== undefined &&
      this.get('utmUpsLowerRightHemisphere') !== undefined
    )
  },

  // Return true if all of the UTM/UPS point radius model fields are defined. Otherwise, false.
  isUtmUpsPointRadiusDefined: function() {
    return (
      this.get('utmUpsEasting') !== undefined &&
      this.get('utmUpsNorthing') !== undefined &&
      this.get('utmUpsZone') !== undefined &&
      this.get('utmUpsHemisphere') !== undefined
    )
  },

  // Get the UTM/UPS Upper-Left bounding box fields in the internal format. See 'parseUtmUps'.
  parseUtmUpsUpperLeft: function() {
    return this.parseUtmUps(
      this.get('utmUpsUpperLeftEasting'),
      this.get('utmUpsUpperLeftNorthing'),
      this.get('utmUpsUpperLeftZone'),
      this.get('utmUpsUpperLeftHemisphere')
    )
  },

  // Get the UTM/UPS Lower-Right bounding box fields in the internal format. See 'parseUtmUps'.
  parseUtmUpsLowerRight: function() {
    return this.parseUtmUps(
      this.get('utmUpsLowerRightEasting'),
      this.get('utmUpsLowerRightNorthing'),
      this.get('utmUpsLowerRightZone'),
      this.get('utmUpsLowerRightHemisphere')
    )
  },

  // Get the UTM/UPS point radius fields in the internal format. See 'parseUtmUps'.
  parseUtmUpsPointRadius: function() {
    return this.parseUtmUps(
      this.get('utmUpsEasting'),
      this.get('utmUpsNorthing'),
      this.get('utmUpsZone'),
      this.get('utmUpsHemisphere')
    )
  },
})
