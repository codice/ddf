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

const Group = require('../group')
const Label = require('./label')
const TextField = require('../text-field')
const { Radio, RadioItem } = require('../radio')

const { Zone, Hemisphere } = require('./common')

const {
  DmsLatitude,
  DmsLongitude,
} = require('../../component/location-new/geo-components/coordinates.js')
const DirectionInput = require('../../component/location-new/geo-components/direction.js')
const { Direction } = require('../../component/location-new/utils/dms-utils.js')

const minimumDifference = 0.0001

const BoundingBoxLatLon = props => {
  const { north, east, south, west, cursor } = props

  const { mapEast, mapWest, mapSouth, mapNorth } = props

  const westMax = parseFloat(mapEast) - minimumDifference
  const eastMin = parseFloat(mapWest) + minimumDifference
  const northMin = parseFloat(mapSouth) + minimumDifference
  const southMax = parseFloat(mapNorth) - minimumDifference

  return (
    <div className="input-location">
      <TextField
        label="West"
        value={west}
        onChange={cursor('west')}
        type="number"
        step="any"
        min={-180}
        max={westMax || 180}
        addon="°"
      />
      <TextField
        label="South"
        value={south}
        onChange={cursor('south')}
        type="number"
        step="any"
        min={-90}
        max={southMax || 90}
        addon="°"
      />
      <TextField
        label="East"
        value={east}
        onChange={cursor('east')}
        type="number"
        step="any"
        min={eastMin || -180}
        max={180}
        addon="°"
      />
      <TextField
        label="North"
        value={north}
        onChange={cursor('north')}
        type="number"
        step="any"
        min={northMin || -90}
        max={90}
        addon="°"
      />
    </div>
  )
}

const BoundingBoxUsngMgrs = props => {
  const { usngbb, cursor } = props
  return (
    <div className="input-location">
      <TextField
        label="USNG / MGRS"
        value={usngbb}
        onChange={cursor('usngbb')}
      />
    </div>
  )
}

const BoundingBoxUtmUps = props => {
  const {
    utmUpsUpperLeftEasting,
    utmUpsUpperLeftNorthing,
    utmUpsUpperLeftZone,
    utmUpsUpperLeftHemisphere,

    utmUpsLowerRightEasting,
    utmUpsLowerRightNorthing,
    utmUpsLowerRightZone,
    utmUpsLowerRightHemisphere,

    cursor,
  } = props

  return (
    <div>
      <div className="input-location">
        <Group>
          <Label>Upper-Left</Label>
          <div>
            <TextField
              label="Easting"
              value={utmUpsUpperLeftEasting}
              onChange={cursor('utmUpsUpperLeftEasting')}
              addon="m"
            />
            <TextField
              label="Northing"
              value={utmUpsUpperLeftNorthing}
              onChange={cursor('utmUpsUpperLeftNorthing')}
              addon="m"
            />
            <Zone
              value={utmUpsUpperLeftZone}
              onChange={cursor('utmUpsUpperLeftZone')}
            />
            <Hemisphere
              value={utmUpsUpperLeftHemisphere}
              onChange={cursor('utmUpsUpperLeftHemisphere')}
            />
          </div>
        </Group>
      </div>
      <div className="input-location">
        <Group>
          <Label>Lower-Right</Label>
          <div>
            <TextField
              label="Easting"
              value={utmUpsLowerRightEasting}
              onChange={cursor('utmUpsLowerRightEasting')}
              addon="m"
            />
            <TextField
              label="Northing"
              value={utmUpsLowerRightNorthing}
              onChange={cursor('utmUpsLowerRightNorthing')}
              addon="m"
            />
            <Zone
              value={utmUpsLowerRightZone}
              onChange={cursor('utmUpsLowerRightZone')}
            />
            <Hemisphere
              value={utmUpsLowerRightHemisphere}
              onChange={cursor('utmUpsLowerRightHemisphere')}
            />
          </div>
        </Group>
      </div>
    </div>
  )
}

const BoundingBoxDms = props => {
  const {
    dmsSouth,
    dmsNorth,
    dmsWest,
    dmsEast,

    dmsSouthDirection,
    dmsNorthDirection,
    dmsWestDirection,
    dmsEastDirection,

    cursor,
  } = props

  const latitudeDirections = [Direction.North, Direction.South]
  const longitudeDirections = [Direction.East, Direction.West]

  return (
    <div className="input-location">
      <DmsLongitude label="West" value={dmsWest} onChange={cursor('dmsWest')}>
        <DirectionInput
          options={longitudeDirections}
          value={dmsWestDirection}
          onChange={cursor('dmsWestDirection')}
        />
      </DmsLongitude>
      <DmsLatitude label="South" value={dmsSouth} onChange={cursor('dmsSouth')}>
        <DirectionInput
          options={latitudeDirections}
          value={dmsSouthDirection}
          onChange={cursor('dmsSouthDirection')}
        />
      </DmsLatitude>
      <DmsLongitude label="East" value={dmsEast} onChange={cursor('dmsEast')}>
        <DirectionInput
          options={longitudeDirections}
          value={dmsEastDirection}
          onChange={cursor('dmsEastDirection')}
        />
      </DmsLongitude>
      <DmsLatitude label="North" value={dmsNorth} onChange={cursor('dmsNorth')}>
        <DirectionInput
          options={latitudeDirections}
          value={dmsNorthDirection}
          onChange={cursor('dmsNorthDirection')}
        />
      </DmsLatitude>
    </div>
  )
}

const BoundingBox = props => {
  const { cursor, locationType } = props

  const inputs = {
    latlon: BoundingBoxLatLon,
    usng: BoundingBoxUsngMgrs,
    utmUps: BoundingBoxUtmUps,
    dms: BoundingBoxDms,
  }

  const Component = inputs[locationType] || null

  return (
    <div>
      <Radio value={locationType} onChange={cursor('locationType')}>
        <RadioItem value="latlon">Lat/Lon (DD)</RadioItem>
        <RadioItem value="dms">Lat/Lon (DMS)</RadioItem>
        <RadioItem value="usng">USNG / MGRS</RadioItem>
        <RadioItem value="utmUps">UTM / UPS</RadioItem>
      </Radio>
      {Component !== null ? <Component {...props} /> : null}
    </div>
  )
}

module.exports = BoundingBox
