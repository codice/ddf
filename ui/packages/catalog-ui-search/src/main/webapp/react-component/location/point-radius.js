const React = require('react')

const { Radio, RadioItem } = require('../radio')
const TextField = require('../text-field')

const { Units, Zone, Hemisphere } = require('./common')

const {
  DmsLatitude,
  DmsLongitude,
} = require('component/location-new/geo-components/coordinates')
const DirectionInput = require('component/location-new/geo-components/direction')
const { Direction } = require('component/location-new/utils/dms-utils')

const PointRadiusLatLon = props => {
  const { lat, lon, radius, radiusUnits, cursor } = props
  return (
    <div>
      <TextField
        label="Latitude"
        value={lat}
        onChange={cursor('lat')}
        addon="°"
      />
      <TextField
        label="Longitude"
        value={lon}
        onChange={cursor('lon')}
        addon="°"
      />
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField label="Radius" value={radius} onChange={cursor('radius')} />
      </Units>
    </div>
  )
}

const PointRadiusUsngMgrs = props => {
  const { usng, radius, radiusUnits, cursor } = props
  return (
    <div>
      <TextField label="USNG / MGRS" value={usng} onChange={cursor('usng')} />
      <Units value={radiusUnits} onChange={cursor('radiusUnits')}>
        <TextField label="Radius" value={radius} onChange={cursor('radius')} />
      </Units>
    </div>
  )
}

const PointRadiusUtm = props => {
  const {
    utmEasting,
    utmNorthing,
    utmZone,
    utmHemisphere,
    radius,
    radiusUnits,
    cursor,
  } = props
  return (
    <div>
      <TextField
        label="Easting"
        value={utmEasting}
        onChange={cursor('utmEasting')}
        addon="m"
      />
      <TextField
        label="Northing"
        value={utmNorthing}
        onChange={cursor('utmNorthing')}
        addon="m"
      />
      <Zone value={utmZone} onChange={cursor('utmZone')} />
      <Hemisphere value={utmHemisphere} onChange={cursor('utmHemisphere')} />
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
    utm: PointRadiusUtm,
  }

  const Component = inputs[locationType] || null

  return (
    <div>
      <Radio value={locationType} onChange={cursor('locationType')}>
        <RadioItem value="latlon">Lat / Lon (DD)</RadioItem>
        <RadioItem value="dms">Lat / Lon (DMS)</RadioItem>
        <RadioItem value="usng">USNG / MGRS</RadioItem>
        <RadioItem value="utm">UTM</RadioItem>
      </Radio>
      <div className="input-location">
        {Component !== null ? <Component {...props} /> : null}
      </div>
    </div>
  )
}

module.exports = PointRadius
