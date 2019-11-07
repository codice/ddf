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
const usngs = require('usng.js')
const { Radio, RadioItem } = require('../radio')
const TextField = require('../text-field')

const { Units, Zone, Hemisphere, MinimumSpacing } = require('./common')

const {
  DmsLatitude,
  DmsLongitude,
} = require('../../component/location-new/geo-components/coordinates.js')
const DirectionInput = require('../../component/location-new/geo-components/direction.js')
const { Direction } = require('../../component/location-new/utils/dms-utils.js')
import styled from 'styled-components'

const ErrorBlock = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  background: ${({ theme }) => theme.negativeColor};
`

const WarningIcon = styled.span`
  padding: ${({ theme }) => theme.minimumSpacing};
`

const PointRadiusLatLon = props => {
  const { lat, lon, radius, radiusUnits, cursor } = props
  return (
    <div>
      <TextField
        type="number"
        label="Latitude"
        value={lat}
        onChange={cursor('lat')}
        onBlur={props.callback}
        addon="°"
      />
      <TextField
        type="number"
        label="Longitude"
        value={lon}
        onChange={cursor('lon')}
        addon="°"
      />
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField
          type="number"
          min="0"
          label="Radius"
          value={radius}
          onChange={cursor('radius')}
        />
      </Units>
    </div>
  )
}

const converter = new usngs.Converter()

const PointRadiusUsngMgrs = props => {
  const { usng, radius, radiusUnits, cursor } = props
  let error = false
  try {
    const result = converter.USNGtoLL(usng, true)
    error = isNaN(result.lat) || isNaN(result.lon)
  } catch (err) {
    error = true
  }
  return (
    <div>
      <TextField label="USNG / MGRS" value={usng} onChange={cursor('usng')} />
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField label="Radius" value={radius} onChange={cursor('radius')} />
      </Units>
      {error ? (
        <ErrorBlock>
          <WarningIcon className="fa fa-warning" />
          <span>Invalid USNG / MGRS coordinates</span>
        </ErrorBlock>
      ) : null}
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
    cursor,
  } = props
  return (
    <div>
      <TextField
        label="Easting"
        value={utmUpsEasting}
        onChange={cursor('utmUpsEasting')}
        addon="m"
      />
      <TextField
        label="Northing"
        value={utmUpsNorthing}
        onChange={cursor('utmUpsNorthing')}
        addon="m"
      />
      <Zone value={utmUpsZone} onChange={cursor('utmUpsZone')} />
      <Hemisphere
        value={utmUpsHemisphere}
        onChange={cursor('utmUpsHemisphere')}
      />
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField label="Radius" value={radius} onChange={cursor('radius')} />
      </Units>
    </div>
  )
}

const PointRadiusDms = props => {
  const {
    dmsLat,
    dmsLon,
    dmsLatDirection,
    dmsLonDirection,
    radius,
    radiusUnits,
    cursor,
  } = props
  const latitudeDirections = [Direction.North, Direction.South]
  const longitudeDirections = [Direction.East, Direction.West]

  return (
    <div>
      <DmsLatitude label="Latitude" value={dmsLat} onChange={cursor('dmsLat')}>
        <DirectionInput
          options={latitudeDirections}
          value={dmsLatDirection}
          onChange={cursor('dmsLatDirection')}
        />
      </DmsLatitude>
      <DmsLongitude
        label="Longitude"
        value={dmsLon}
        onChange={cursor('dmsLon')}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dmsLonDirection}
          onChange={cursor('dmsLonDirection')}
        />
      </DmsLongitude>
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField
          label="Radius"
          type="number"
          value={radius}
          onChange={cursor('radius')}
        />
      </Units>
    </div>
  )
}

const PointRadius = props => {
  const { cursor, locationType } = props

  const inputs = {
    latlon: PointRadiusLatLon,
    dms: PointRadiusDms,
    usng: PointRadiusUsngMgrs,
    utmUps: PointRadiusUtmUps,
  }

  const Component = inputs[locationType] || null

  return (
    <div>
      <Radio value={locationType} onChange={cursor('locationType')}>
        <RadioItem value="latlon">Lat / Lon (DD)</RadioItem>
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
