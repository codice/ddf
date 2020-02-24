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
const { Radio, RadioItem } = require('../radio')
const TextField = require('../text-field')
import {
  validateGeo,
  initialErrorState,
  initialErrorStateWithDefault,
  ErrorComponent,
} from '../utils/validation'
const { Units, Zone, Hemisphere, MinimumSpacing } = require('./common')
const {
  DmsLatitude,
  DmsLongitude,
} = require('../../component/location-new/geo-components/coordinates.js')
const DirectionInput = require('../../component/location-new/geo-components/direction.js')
const { Direction } = require('../../component/location-new/utils/dms-utils.js')

const PointRadiusLatLonDd = props => {
  const { lat, lon, radius, radiusUnits, setState } = props
  const [ddError, setDdError] = useState(initialErrorStateWithDefault)
  const [radiusError, setRadiusError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setDdError(initialErrorStateWithDefault)
        setRadiusError(initialErrorState)
      }
    },
    [props.lat, props.lon, props.radius]
  )

  function validateDd(key, value) {
    const { error, message, defaultValue } = validateGeo(key, value)
    if (defaultValue) {
      setDdError({ error, message, defaultValue })
      setState({ [key]: defaultValue })
    } else {
      setState({ [key]: value })
    }
  }

  return (
    <div>
      <TextField
        type="number"
        label="Latitude"
        value={lat !== undefined ? String(lat) : lat}
        onChange={value => validateDd('lat', value)}
        onBlur={() => setDdError(validateGeo('lat', lat))}
        addon="°"
      />
      <TextField
        type="number"
        label="Longitude"
        value={lon !== undefined ? String(lon) : lon}
        onChange={value => validateDd('lon', value)}
        onBlur={() => setDdError(validateGeo('lon', lon))}
        addon="°"
      />
      <ErrorComponent errorState={ddError} />
      <Units
        value={radiusUnits}
        onChange={value => {
          setState({ ['radiusUnits']: value })
          setRadiusError(validateGeo('radius', { value: radius, units: value }))
        }}
      >
        <TextField
          type="number"
          label="Radius"
          value={String(radius)}
          onChange={value => {
            setState({ ['radius']: value })
          }}
          onBlur={e =>
            setRadiusError(
              validateGeo('radius', {
                value: e.target.value,
                units: radiusUnits,
              })
            )
          }
        />
      </Units>
      <ErrorComponent errorState={radiusError} />
    </div>
  )
}

const PointRadiusLatLonDms = props => {
  const {
    dmsLat,
    dmsLon,
    dmsLatDirection,
    dmsLonDirection,
    radius,
    radiusUnits,
    setState,
  } = props
  const [dmsError, setDmsError] = useState(initialErrorStateWithDefault)
  const [radiusError, setRadiusError] = useState(initialErrorState)
  const latitudeDirections = [Direction.North, Direction.South]
  const longitudeDirections = [Direction.East, Direction.West]

  useEffect(
    () => {
      if (props.drawing) {
        setDmsError(initialErrorStateWithDefault)
        setRadiusError(initialErrorState)
      }
    },
    [props.dmsLat, props.dmsLon, props.radius]
  )

  function validateDms(key, type, value) {
    const { error, message, defaultValue } = validateGeo(key, value)
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
    <div>
      <DmsLatitude
        label="Latitude"
        value={dmsLat}
        onChange={(value, type) => validateDms('dmsLat', type, value)}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dmsLatDirection}
          onChange={value => setState({ ['dmsLatDirection']: value })}
        />
      </DmsLatitude>
      <DmsLongitude
        label="Longitude"
        value={dmsLon}
        onChange={(value, type) => validateDms('dmsLon', type, value)}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dmsLonDirection}
          onChange={value => setState({ ['dmsLonDirection']: value })}
        />
      </DmsLongitude>
      <ErrorComponent errorState={dmsError} />
      <Units
        value={radiusUnits}
        onChange={value => setState({ ['radiusUnits']: value })}
      >
        <TextField
          label="Radius"
          type="number"
          value={String(radius)}
          onChange={value => {
            setRadiusError(validateGeo('radius', value))
            setState({ ['radius']: value })
          }}
        />
      </Units>
      <ErrorComponent errorState={radiusError} />
    </div>
  )
}

const PointRadiusUsngMgrs = props => {
  const { usng, radius, radiusUnits, setState } = props
  const [usngError, setUsngError] = useState(initialErrorState)
  const [radiusError, setRadiusError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setUsngError(initialErrorState)
        setRadiusError(initialErrorState)
      }
    },
    [props.usng, props.radius]
  )

  return (
    <div>
      <TextField
        label="USNG / MGRS"
        value={usng}
        onChange={value => setState({ ['usng']: value })}
        onBlur={() => setUsngError(validateGeo('usng', usng))}
      />
      <ErrorComponent errorState={usngError} />
      <Units
        value={radiusUnits}
        onChange={value => setState({ ['radiusUnits']: value })}
      >
        <TextField
          label="Radius"
          type="number"
          value={String(radius)}
          onChange={value => {
            setRadiusError(validateGeo('radius', value))
            setState({ ['radius']: value })
          }}
        />
      </Units>
      <ErrorComponent errorState={radiusError} />
    </div>
  )
}

const PointRadiusUtmUps = props => {
  const {
    utmUpsEasting,
    utmUpsNorthing,
    utmUpsZone,
    utmUpsHemisphere,
    radius,
    radiusUnits,
    setState,
  } = props
  const [utmError, setUtmError] = useState(initialErrorState)
  const [radiusError, setRadiusError] = useState(initialErrorState)

  useEffect(
    () => {
      if (props.drawing) {
        setUtmError(initialErrorState)
        setRadiusError(initialErrorState)
      }
    },
    [
      props.utmUpsEasting,
      props.utmUpsNorthing,
      props.utmUpsZone,
      props.utmUpsHemisphere,
      props.radius,
    ]
  )

  return (
    <div>
      <TextField
        label="Easting"
        value={
          utmUpsEasting !== undefined ? String(utmUpsEasting) : utmUpsEasting
        }
        onChange={value => setState({ ['utmUpsEasting']: value })}
        onBlur={() =>
          setUtmError(
            validateGeo('utmUpsEasting', {
              utmUpsEasting,
              utmUpsNorthing,
              zoneNumber: utmUpsZone,
              hemisphere: utmUpsHemisphere,
            })
          )
        }
        addon="m"
      />
      <TextField
        label="Northing"
        value={
          utmUpsNorthing !== undefined ? String(utmUpsNorthing) : utmUpsNorthing
        }
        onChange={value => setState({ ['utmUpsNorthing']: value })}
        onBlur={() =>
          setUtmError(
            validateGeo('utmUpsNorthing', {
              utmUpsEasting,
              utmUpsNorthing,
              zoneNumber: utmUpsZone,
              hemisphere: utmUpsHemisphere,
            })
          )
        }
        addon="m"
      />
      <Zone
        value={utmUpsZone}
        onChange={value => setState({ ['utmUpsZone']: value })}
        onBlur={() =>
          setUtmError(
            validateGeo('utmUpsZone', {
              utmUpsEasting,
              utmUpsNorthing,
              zoneNumber: utmUpsZone,
              hemisphere: utmUpsHemisphere,
            })
          )
        }
      />
      <Hemisphere
        value={utmUpsHemisphere}
        onChange={value => setState({ ['utmUpsHemisphere']: value })}
        onBlur={() =>
          setUtmError(
            validateGeo('utmUpsHemisphere', {
              utmUpsEasting,
              utmUpsNorthing,
              zoneNumber: utmUpsZone,
              hemisphere: utmUpsHemisphere,
            })
          )
        }
      />
      <ErrorComponent errorState={utmError} />
      <Units
        value={radiusUnits}
        onChange={value => setState({ ['radiusUnits']: value })}
      >
        <TextField
          label="Radius"
          type="number"
          value={String(radius)}
          onChange={value => {
            setRadiusError(validateGeo('radius', value))
            setState({ ['radius']: value })
          }}
        />
      </Units>
      <ErrorComponent errorState={radiusError} />
    </div>
  )
}

const PointRadius = props => {
  const { setState, locationType } = props

  const inputs = {
    dd: PointRadiusLatLonDd,
    dms: PointRadiusLatLonDms,
    usng: PointRadiusUsngMgrs,
    utmUps: PointRadiusUtmUps,
  }

  const Component = inputs[locationType] || null

  return (
    <div>
      <Radio
        value={locationType}
        onChange={value => setState({ ['locationType']: value })}
      >
        <RadioItem value="dd">Lat / Lon (DD)</RadioItem>
        <RadioItem value="dms">Lat / Lon (DMS)</RadioItem>
        <RadioItem value="usng">USNG / MGRS</RadioItem>
        <RadioItem value="utmUps">UTM / UPS</RadioItem>
      </Radio>
      <MinimumSpacing />
      <div className="input-location">
        {Component !== null ? <Component {...props} /> : null}
      </div>
    </div>
  )
}

module.exports = PointRadius
