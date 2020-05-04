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
import React, { useState, useEffect } from 'react'
import {
  validateGeo,
  ErrorComponent,
  initialErrorState,
  initialErrorStateWithDefault,
} from '../utils/validation'
const Group = require('../group')
const Label = require('./label')
const TextField = require('../text-field')
const { Radio, RadioItem } = require('../radio')
const { Zone, Hemisphere, MinimumSpacing } = require('./common')
const {
  DmsLatitude,
  DmsLongitude,
} = require('../../component/location-new/geo-components/coordinates.js')
const DirectionInput = require('../../component/location-new/geo-components/direction.js')
const { Direction } = require('../../component/location-new/utils/dms-utils.js')

const BoundingBoxLatLonDd = props => {
  const { north, east, south, west, setState } = props
  const [ddError, setDdError] = useState(initialErrorStateWithDefault)

  useEffect(
    () => {
      if (props.drawing) {
        setDdError(initialErrorStateWithDefault)
      }
      if (!ddError.error) {
        setDdError(validateGeo('bbox', { north, south, west, east }))
      }
    },
    [props.east, props.west, props.south, props.north]
  )

  function validateDd(key, value, type) {
    const label = key.includes('east') || key.includes('west') ? 'lon' : 'lat'
    const { error, message, defaultValue } = validateGeo(label, value)
    if (type === 'blur' || defaultValue) {
      setDdError({ error, message, defaultValue })
    }
    if (!error && label === 'lat') {
      const opposite = key.includes('north') ? south : north
      setDdError(
        validateGeo('bbox', {
          north: key.includes('north') ? value : opposite,
          south: key.includes('south') ? value : opposite,
        })
      )
    } else if (!error && label === 'lon') {
      const opposite = key.includes('west') ? east : west
      setDdError(
        validateGeo('bbox', {
          west: key.includes('west') ? value : opposite,
          east: key.includes('east') ? value : opposite,
        })
      )
    }
    setState({ [key]: defaultValue ? defaultValue : value })
  }

  return (
    <div className="input-location">
      <TextField
        label="West"
        value={west !== undefined ? String(west) : west}
        onChange={value => validateDd('west', value)}
        onBlur={event => validateDd('west', west, event.type)}
        type="number"
        step="any"
        min={-180}
        max={180}
        addon="°"
      />
      <TextField
        label="South"
        value={south !== undefined ? String(south) : south}
        onChange={value => validateDd('south', value)}
        onBlur={event => validateDd('south', south, event.type)}
        type="number"
        step="any"
        min={-90}
        max={90}
        addon="°"
      />
      <TextField
        label="East"
        value={east !== undefined ? String(east) : east}
        onChange={value => validateDd('east', value)}
        onBlur={event => validateDd('east', east, event.type)}
        type="number"
        step="any"
        min={-180}
        max={180}
        addon="°"
      />
      <TextField
        label="North"
        value={north !== undefined ? String(north) : north}
        onChange={value => validateDd('north', value)}
        onBlur={event => validateDd('north', north, event.type)}
        type="number"
        step="any"
        min={-90}
        max={90}
        addon="°"
      />
      <ErrorComponent errorState={ddError} />
    </div>
  )
}

const BoundingBoxLatLonDms = props => {
  const {
    dmsSouth,
    dmsNorth,
    dmsWest,
    dmsEast,
    dmsSouthDirection,
    dmsNorthDirection,
    dmsWestDirection,
    dmsEastDirection,
    setState,
  } = props
  const [dmsError, setDmsError] = useState(initialErrorStateWithDefault)
  const latitudeDirections = [Direction.North, Direction.South]
  const longitudeDirections = [Direction.East, Direction.West]

  useEffect(
    () => {
      if (props.drawing) {
        setDmsError(initialErrorStateWithDefault)
      }
      if (!dmsError.error) {
        setDmsError(
          validateGeo('bbox', {
            isDms: true,
            dmsNorthDirection,
            dmsSouthDirection,
            dmsWestDirection,
            dmsEastDirection,
            north: dmsNorth,
            south: dmsSouth,
            west: dmsWest,
            east: dmsEast,
          })
        )
      }
    },
    [props.dmsWest, props.dmsSouth, props.dmsEast, props.dmsNorth]
  )

  function validateDms(key, value, type) {
    const label =
      key.includes('East') || key.includes('West') ? 'dmsLon' : 'dmsLat'
    const { error, message, defaultValue } = validateGeo(label, value)
    if (type === 'blur' || defaultValue) {
      setDmsError({
        error,
        message,
        defaultValue,
      })
    }
    if (!error && label === 'dmsLat') {
      const opposite = key.includes('North') ? dmsSouth : dmsNorth
      setDmsError(
        validateGeo('bbox', {
          isDms: true,
          dmsNorthDirection,
          dmsSouthDirection,
          north: key.includes('North') ? value : opposite,
          south: key.includes('South') ? value : opposite,
        })
      )
    } else if (!error && label === 'dmsLon') {
      const opposite = key.includes('West') ? dmsEast : dmsWest
      setDmsError(
        validateGeo('bbox', {
          isDms: true,
          dmsWestDirection,
          dmsEastDirection,
          west: key.includes('West') ? value : opposite,
          east: key.includes('East') ? value : opposite,
        })
      )
    }
    setState({ [key]: defaultValue ? defaultValue : value })
  }

  return (
    <div className="input-location">
      <DmsLongitude
        label="West"
        value={dmsWest}
        onChange={(value, type) => validateDms('dmsWest', value, type)}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dmsWestDirection}
          onChange={value => setState({ ['dmsWestDirection']: value })}
        />
      </DmsLongitude>
      <DmsLatitude
        label="South"
        value={dmsSouth}
        onChange={(value, type) => validateDms('dmsSouth', value, type)}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dmsSouthDirection}
          onChange={value => setState({ ['dmsSouthDirection']: value })}
        />
      </DmsLatitude>
      <DmsLongitude
        label="East"
        value={dmsEast}
        onChange={(value, type) => validateDms('dmsEast', value, type)}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dmsEastDirection}
          onChange={value => setState({ ['dmsEastDirection']: value })}
        />
      </DmsLongitude>
      <DmsLatitude
        label="North"
        value={dmsNorth}
        onChange={(value, type) => validateDms('dmsNorth', value, type)}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dmsNorthDirection}
          onChange={value => setState({ ['dmsNorthDirection']: value })}
        />
      </DmsLatitude>
      <ErrorComponent errorState={dmsError} />
    </div>
  )
}

const BoundingBoxUsngMgrs = props => {
  const { usngbbUpperLeft, usngbbLowerRight, setState } = props
  const [usngError, setUsngError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setUsngError(initialErrorState)
      }
      if (!usngError.error) {
        setUsngError(
          validateGeo('bbox', {
            isUsng: true,
            upperLeft: usngbbUpperLeft,
            lowerRight: usngbbLowerRight,
          })
        )
      }
    },
    [props.usngbbUpperLeft, props.usngbbLowerRight]
  )

  function validateUsng(value) {
    const { error, message } = validateGeo('usng', value)
    setUsngError({ error, message })
    if (!error) {
      setUsngError(
        validateGeo('bbox', {
          isUsng: true,
          upperLeft: usngbbUpperLeft,
          lowerRight: usngbbLowerRight,
        })
      )
    }
  }

  return (
    <div className="input-location">
      <TextField
        label="Upper Left"
        style={{ minWidth: 200 }}
        value={usngbbUpperLeft}
        onChange={value => setState({ ['usngbbUpperLeft']: value })}
        onBlur={() => validateUsng(usngbbUpperLeft)}
      />
      <TextField
        label="Lower Right"
        style={{ minWidth: 200 }}
        value={usngbbLowerRight}
        onChange={value => setState({ ['usngbbLowerRight']: value })}
        onBlur={() => validateUsng(usngbbLowerRight)}
      />
      <ErrorComponent errorState={usngError} />
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
    setState,
  } = props
  const upperLeft = {
    easting: utmUpsUpperLeftEasting,
    northing: utmUpsUpperLeftNorthing,
    zoneNumber: utmUpsUpperLeftZone,
    hemisphere: utmUpsUpperLeftHemisphere,
  }
  const lowerRight = {
    easting: utmUpsLowerRightEasting,
    northing: utmUpsLowerRightNorthing,
    zoneNumber: utmUpsLowerRightZone,
    hemisphere: utmUpsLowerRightHemisphere,
  }
  const [upperLeftError, setUpperLeftError] = useState(initialErrorState)
  const [lowerRightError, setLowerRightError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setUpperLeftError(initialErrorState)
        setLowerRightError(initialErrorState)
      }
      if (
        !lowerRightError.error ||
        lowerRightError.message.includes('must be located above') ||
        lowerRightError.message.includes('cannot equal')
      ) {
        setLowerRightError(
          validateGeo('bbox', { isUtmUps: true, upperLeft, lowerRight })
        )
      }
    },
    [
      props.utmUpsUpperLeftEasting,
      props.utmUpsUpperLeftNorthing,
      props.utmUpsUpperLeftZone,
      props.utmUpsUpperLeftHemisphere,
      props.utmUpsLowerRightEasting,
      props.utmUpsLowerRightNorthing,
      props.utmUpsLowerRightZone,
      props.utmUpsLowerRightHemisphere,
    ]
  )

  function validateUtmUps(field, key, value) {
    if (field === 'upperLeft') {
      upperLeft[key] = value
      setUpperLeftError(validateGeo(key, upperLeft))
      // If lower right was previously located above upper left,
      // perform an update to the error message in case that has changed
      if (lowerRightError.message.includes('must be located above')) {
        setLowerRightError(
          validateGeo('bbox', { isUtmUps: true, upperLeft, lowerRight })
        )
      }
    } else {
      lowerRight[key] = value
      const { error, message } = validateGeo(key, lowerRight)
      setLowerRightError({ error, message })
      if (!error) {
        setLowerRightError(
          validateGeo('bbox', { isUtmUps: true, upperLeft, lowerRight })
        )
      }
    }
  }

  return (
    <div>
      <div className="input-location">
        <Group>
          <Label>Upper Left</Label>
          <div>
            <TextField
              label="Easting"
              value={
                utmUpsUpperLeftEasting !== undefined
                  ? String(utmUpsUpperLeftEasting)
                  : utmUpsUpperLeftEasting
              }
              onChange={value =>
                setState({ ['utmUpsUpperLeftEasting']: value })
              }
              onBlur={() =>
                validateUtmUps('upperLeft', 'easting', utmUpsUpperLeftEasting)
              }
              addon="m"
            />
            <TextField
              label="Northing"
              value={
                utmUpsUpperLeftNorthing !== undefined
                  ? String(utmUpsUpperLeftNorthing)
                  : utmUpsUpperLeftNorthing
              }
              onChange={value =>
                setState({ ['utmUpsUpperLeftNorthing']: value })
              }
              onBlur={() =>
                validateUtmUps('upperLeft', 'northing', utmUpsUpperLeftNorthing)
              }
              addon="m"
            />
            <Zone
              value={utmUpsUpperLeftZone}
              onChange={value => {
                setState({ ['utmUpsUpperLeftZone']: value })
                validateUtmUps('upperLeft', 'zoneNumber', value)
              }}
            />
            <Hemisphere
              value={utmUpsUpperLeftHemisphere}
              onChange={value => {
                setState({ ['utmUpsUpperLeftHemisphere']: value })
                validateUtmUps('upperLeft', 'hemisphere', value)
              }}
            />
          </div>
        </Group>
        <ErrorComponent errorState={upperLeftError} />
      </div>
      <div className="input-location">
        <Group>
          <Label>Lower Right</Label>
          <div>
            <TextField
              label="Easting"
              value={
                utmUpsLowerRightEasting !== undefined
                  ? String(utmUpsLowerRightEasting)
                  : utmUpsLowerRightEasting
              }
              onChange={value =>
                setState({ ['utmUpsLowerRightEasting']: value })
              }
              onBlur={() =>
                validateUtmUps('lowerRight', 'easting', utmUpsLowerRightEasting)
              }
              addon="m"
            />
            <TextField
              label="Northing"
              value={
                utmUpsLowerRightNorthing !== undefined
                  ? String(utmUpsLowerRightNorthing)
                  : utmUpsLowerRightNorthing
              }
              onChange={value =>
                setState({ ['utmUpsLowerRightNorthing']: value })
              }
              onBlur={() =>
                validateUtmUps(
                  'lowerRight',
                  'northing',
                  utmUpsLowerRightNorthing
                )
              }
              addon="m"
            />
            <Zone
              value={utmUpsLowerRightZone}
              onChange={value => {
                setState({ ['utmUpsLowerRightZone']: value })
                validateUtmUps('lowerRight', 'zoneNumber', value)
              }}
            />
            <Hemisphere
              value={utmUpsLowerRightHemisphere}
              onChange={value => {
                setState({ ['utmUpsLowerRightHemisphere']: value })
                validateUtmUps('lowerRight', 'hemisphere', value)
              }}
            />
          </div>
        </Group>
        <ErrorComponent errorState={lowerRightError} />
      </div>
    </div>
  )
}

const BoundingBox = props => {
  const { setState, locationType } = props

  const inputs = {
    dd: BoundingBoxLatLonDd,
    dms: BoundingBoxLatLonDms,
    usng: BoundingBoxUsngMgrs,
    utmUps: BoundingBoxUtmUps,
  }

  const Component = inputs[locationType] || null

  return (
    <div>
      <Radio
        value={locationType}
        onChange={value => setState({ ['locationType']: value })}
      >
        <RadioItem value="dd">Lat/Lon (DD)</RadioItem>
        <RadioItem value="dms">Lat/Lon (DMS)</RadioItem>
        <RadioItem value="usng">USNG / MGRS</RadioItem>
        <RadioItem value="utmUps">UTM / UPS</RadioItem>
      </Radio>
      <MinimumSpacing />
      {Component !== null ? <Component {...props} /> : null}
    </div>
  )
}

module.exports = BoundingBox
