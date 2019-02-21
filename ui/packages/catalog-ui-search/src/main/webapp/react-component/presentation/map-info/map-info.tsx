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
import * as React from 'react'
import styled from '../../styles/styled-components'
import { hot } from 'react-hot-loader'

//const Common = require('../../../../webapp/js/Common')
//const metacardDefinitions = require('../../../component/singletons/metacard-definitions')
const mtgeo = require('mt-geo')
const usngs = require('usng.js')

const converter = new usngs.Converter()
const usngPrecision = 6

type Coordinates = {
  lat: number
  lon: number
}

type Props = {
  format: string
} & Coordinates

// interface Attribute {
//   name: string
//   value: string
// }

// function leftPad(numToPad: number, size: number): string {
//   var sign = Math.sign(numToPad) === -1 ? '-' : ''
//   return new Array(sign === '-' ? size - 1 : size)
//     .concat([numToPad])
//     .join(' ')
//     .slice(-size)
// }

// const formatAttribute = ({ name, value }: Attribute): string => {
//   const isDate = metacardDefinitions.metacardTypes[name].type === 'DATE'
//   return `${name.toUpperCase()}: ${
//     isDate ? Common.getHumanReadableDateTime(value) : value
//   }`
// }

const formatCoordinate = ({ lat, lon, format }: Props): any => {
  const withFormat = {
    degrees: () => `${mtgeo.toLat(lat)} ${mtgeo.toLon(lon)}`,
    decimal: () => `${lat} ${lon}`,
    mgrs: () =>
      lat > 84 || lat < -80
        ? 'In UPS Space'
        : converter.LLtoUSNG(lat, lon, usngPrecision),
    utm: () => converter.LLtoUTMUPS(lat, lon),
  } 
  if (!(format in withFormat)) {
    throw `Unrecognized coordinate format value [${format}]`
  }
  if (typeof lat === 'undefined' || typeof lon === 'undefined') {
    throw `formatCoordinate: invalid lat/lon`
  }

  return (withFormat as any)[format]()
}

// decimalComponent(lat, lon) {
//   return (
//     <span>
//       {`${leftPad(Math.floor(lat), 3, ' ')}.${Math.abs(lat % 1)
//         .toFixed(6)
//         .toString()
//         .slice(2)} ${leftPad(Math.floor(lon), 4, ' ')}.${Math.abs(lon % 1)
//         .toFixed(6)
//         .toString()
//         .slice(2)}`}
//     </span>
//   )
// },
// getDisplayComponent: function() {
//   const coordinateFormat = getCoordinateFormat()
//   const lat = this.model.get('mouseLat')
//   const lon = this.model.get('mouseLon')
//   if (typeof lat === 'undefined' || typeof lon === 'undefined') {
//     return null
//   }
//   switch (coordinateFormat) {
//     case 'degrees':
//       return <span>{mtgeo.toLat(lat) + ' ' + mtgeo.toLon(lon)}</span>
//     case 'decimal':
//       return this.decimalComponent(lat, lon)
//     case 'mgrs':
//       // TODO: Move leaking defensive check knowledge to usng library (DDF-4335)
//       return lat > 84 || lat < -80 ? (
//         'In UPS Space'
//       ) : (
//         <span>{converter.LLtoUSNG(lat, lon, usngPrecision)}</span>
//       )
//     case 'utm':
//       return <span>{converter.LLtoUTMUPS(lat, lon)}</span>
//   }
//   throw 'Unrecognized coordinate format value [' + coordinateFormat + ']'
// },
const Root = styled.div`
  font-family: 'Inconsolata', 'Lucida Console', monospace;
  background: ${props => props.theme.backgroundModal};
  display: block;
  width: auto;
  height: auto;
  font-size: ${props => props.theme.minimumFontSize};
  position: absolute;
  left: 0px;
  bottom: 0px;
  text-align: left;
  padding: ${props => props.theme.minimumSpacing};
  max-width: 50%;

  &.info-coordinates {
    display: inline-block;
    white-space: pre;
  }
  &.info-feature {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    display: none;
  }
`

const render = (props: Props) => {
  const coordinate = formatCoordinate(props)
  return (
    <Root>
      <div className="info-coordinates">
        <span>{coordinate}</span>
      </div>
    </Root>
  )
}

export default hot(module)(render)
