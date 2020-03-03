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
const DirectionInput = require('./direction.js')
const { Direction, DmsLatitude, DmsLongitude } = require('./dms-utils.js')

const minimumDifference = 0.0001

const BoundingBoxLatLonDd = props => {
  const {
    north,
    east,
    south,
    west,
    setState,
    mapEast,
    mapWest,
    mapSouth,
    mapNorth,
  } = props
  const [ddError, setDdError] = useState(initialErrorStateWithDefault)
  const westMax = parseFloat(mapEast) - minimumDifference
  const eastMin = parseFloat(mapWest) + minimumDifference
  const northMin = parseFloat(mapSouth) + minimumDifference
  const southMax = parseFloat(mapNorth) - minimumDifference

  useEffect(
    () => {
      if (props.drawing) {
        setDdError(initialErrorStateWithDefault)
      }
    },
    [props.east, props.west, props.south, props.north]
  )

  function validateDd(key, value) {
    const label = key.includes('east') || key.includes('west') ? 'lon' : 'lat'
    const { error, message, defaultValue } = validateGeo(label, value)
    if (defaultValue) {
      setDdError({ error, message, defaultValue })
      setState({ [key]: defaultValue })
    } else {
      setState({ [key]: value })
    }
  }
  return (
    <div className="input-location">
      <TextField
        label="West"
        value={west !== undefined ? String(west) : west}
        onChange={value => validateDd('west', value)}
        onBlur={() => setDdError(validateGeo('lon', west))}
        type="number"
        step="any"
        min={-180}
        max={westMax || 180}
        addon="°"
      />
      <TextField
        label="South"
        value={south !== undefined ? String(south) : south}
        onChange={value => validateDd('south', value)}
        onBlur={() => setDdError(validateGeo('lat', south))}
        type="number"
        step="any"
        min={-90}
        max={southMax || 90}
        addon="°"
      />
      <TextField
        label="East"
        value={east !== undefined ? String(east) : east}
        onChange={value => validateDd('east', value)}
        onBlur={() => setDdError(validateGeo('lon', east))}
        type="number"
        step="any"
        min={eastMin || -180}
        max={180}
        addon="°"
      />
      <TextField
        label="North"
        value={north !== undefined ? String(north) : north}
        onChange={value => validateDd('north', value)}
        onBlur={() => setDdError(validateGeo('lat', north))}
        type="number"
        step="any"
        min={northMin || -90}
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
    },
    [props.dmsWest, props.dmsSouth, props.dmsEast, props.dmsNorth]
  )

  function validateDms(key, type, value) {
    const label =
      key.includes('East') || key.includes('West') ? 'dmsLon' : 'dmsLat'
    const { error, message, defaultValue } = validateGeo(label, value)
    if (type === 'blur') {
      setDmsError({
        error: value !== undefined && value.length === 0,
        message,
        defaultValue,
      })
    } else if (defaultValue) {
      setDmsError({
        error,
        message,
        defaultValue,
      })
    }
    defaultValue
      ? setState({ [key]: defaultValue })
      : setState({ [key]: value })
  }

  return (
    <div className="input-location">
      <DmsLongitude
        label="West"
        value={dmsWest}
        onChange={(value, type) => validateDms('dmsWest', type, value)}
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
        onChange={(value, type) => validateDms('dmsSouth', type, value)}
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
        onChange={(value, type) => validateDms('dmsEast', type, value)}
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
        onChange={(value, type) => validateDms('dmsNorth', type, value)}
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
    },
    [props.usngbbUpperLeft, props.usngbbLowerRight]
  )

  return (
    <div className="input-location">
      <TextField
        label="Upper Left"
        style={{ minWidth: 200 }}
        value={usngbbUpperLeft}
        onChange={value => setState({ ['usngbbUpperLeft']: value })}
        onBlur={() => setUsngError(validateGeo('usng', usngbbUpperLeft))}
      />
      <TextField
        label="Lower Right"
        style={{ minWidth: 200 }}
        value={usngbbLowerRight}
        onChange={value => setState({ ['usngbbLowerRight']: value })}
        onBlur={() => setUsngError(validateGeo('usng', usngbbLowerRight))}
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
  const [upperLeftError, setUpperLeftError] = useState(initialErrorState)
  const [lowerRightError, setLowerRightError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setUpperLeftError(initialErrorState)
        setLowerRightError(initialErrorState)
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

  return (
    <div>
      <div className="input-location">
        <Group>
          <Label>Upper-Left</Label>
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
                setUpperLeftError(
                  validateGeo('utmUpsEasting', {
                    utmUpsEasting: utmUpsUpperLeftEasting,
                    utmUpsNorthing: utmUpsUpperLeftNorthing,
                    zoneNumber: utmUpsUpperLeftZone,
                    hemisphere: utmUpsUpperLeftHemisphere,
                  })
                )
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
                setUpperLeftError(
                  validateGeo('utmUpsNorthing', {
                    utmUpsEasting: utmUpsUpperLeftEasting,
                    utmUpsNorthing: utmUpsUpperLeftNorthing,
                    zoneNumber: utmUpsUpperLeftZone,
                    hemisphere: utmUpsUpperLeftHemisphere,
                  })
                )
              }
              addon="m"
            />
            <Zone
              value={utmUpsUpperLeftZone}
              onChange={value => setState({ ['utmUpsUpperLeftZone']: value })}
              onBlur={() =>
                setUpperLeftError(
                  validateGeo('utmUpsZone', {
                    utmUpsEasting: utmUpsUpperLeftEasting,
                    utmUpsNorthing: utmUpsUpperLeftNorthing,
                    zoneNumber: utmUpsUpperLeftZone,
                    hemisphere: utmUpsUpperLeftHemisphere,
                  })
                )
              }
            />
            <Hemisphere
              value={utmUpsUpperLeftHemisphere}
              onChange={value =>
                setState({ ['utmUpsUpperLeftHemisphere']: value })
              }
              onBlur={() =>
                setUpperLeftError(
                  validateGeo('utmUpsHemisphere', {
                    utmUpsEasting: utmUpsUpperLeftEasting,
                    utmUpsNorthing: utmUpsUpperLeftNorthing,
                    zoneNumber: utmUpsUpperLeftZone,
                    hemisphere: utmUpsUpperLeftHemisphere,
                  })
                )
              }
            />
          </div>
        </Group>
        <ErrorComponent errorState={upperLeftError} />
      </div>
      <div className="input-location">
        <Group>
          <Label>Lower-Right</Label>
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
                setLowerRightError(
                  validateGeo('utmUpsEasting', {
                    utmUpsEasting: utmUpsLowerRightEasting,
                    utmUpsNorthing: utmUpsLowerRightNorthing,
                    zoneNumber: utmUpsLowerRightZone,
                    hemisphere: utmUpsLowerRightHemisphere,
                  })
                )
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
                setLowerRightError(
                  validateGeo('utmUpsNorthing', {
                    utmUpsEasting: utmUpsLowerRightEasting,
                    utmUpsNorthing: utmUpsLowerRightNorthing,
                    zoneNumber: utmUpsLowerRightZone,
                    hemisphere: utmUpsLowerRightHemisphere,
                  })
                )
              }
              addon="m"
            />
            <Zone
              value={utmUpsLowerRightZone}
              onChange={value => setState({ ['utmUpsLowerRightZone']: value })}
              onBlur={() =>
                setLowerRightError(
                  validateGeo('utmUpsZone', {
                    utmUpsEasting: utmUpsLowerRightEasting,
                    utmUpsNorthing: utmUpsLowerRightNorthing,
                    zoneNumber: utmUpsLowerRightZone,
                    hemisphere: utmUpsLowerRightHemisphere,
                  })
                )
              }
            />
            <Hemisphere
              value={utmUpsLowerRightHemisphere}
              onChange={value =>
                setState({ ['utmUpsLowerRightHemisphere']: value })
              }
              onBlur={() =>
                setLowerRightError(
                  validateGeo('utmUpsHemisphere', {
                    utmUpsEasting: utmUpsLowerRightEasting,
                    utmUpsNorthing: utmUpsLowerRightNorthing,
                    zoneNumber: utmUpsLowerRightZone,
                    hemisphere: utmUpsLowerRightHemisphere,
                  })
                )
              }
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
