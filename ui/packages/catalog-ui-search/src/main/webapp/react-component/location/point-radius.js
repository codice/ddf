const React = require('react')

const { Radio, RadioItem } = require('../radio')
const TextField = require('../text-field')

const { Units, Zone, Hemisphere } = require('./common')

const {
  DmsLatitude,
  DmsLongitude,
} = require('../../component/location-new/geo-components/coordinates.js')
const DirectionInput = require('../../component/location-new/geo-components/direction.js')
const { Direction } = require('../../component/location-new/utils/dms-utils.js')

const PointRadiusLatLon = props => {
  const { lat, lon, radius, radiusUnits, cursor } = props
  return (
    <div>
      <TextField
        type="number"
        label="Latitude"
        value={lat === 0 ? '' : lat}
        onChange={cursor('lat')}
        onBlur={props.callback}
        addon="°"
      />
      <TextField
        type="number"
        label="Longitude"
        value={lon === 0 ? '' : lon}
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
      <div className="input-location">
        {Component !== null ? <Component {...props} /> : null}
      </div>
    </div>
  )
}

module.exports = PointRadius
