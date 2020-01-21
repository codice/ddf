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
const dmsRegex = new RegExp('^(.*)°(.*)\'(.*\\.?.*)"$')
const TextField = require('../../../react-component/text-field/index.js')
const MaskedTextField = require('../inputs/masked-text-field')
const { latitudeDMSMask, longitudeDMSMask } = require('./masks')

const Coordinate = props => {
  const { placeholder, value, onChange, children, ...otherProps } = props
  return (
    <div className="coordinate">
      <TextField
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        {...otherProps}
      />
      {children}
    </div>
  )
}

const MaskedCoordinate = props => {
  const { placeholder, mask, value, onChange, children, ...otherProps } = props
  return (
    <div className="coordinate">
      <MaskedTextField
        placeholder={placeholder}
        mask={mask}
        value={value}
        onChange={onChange}
        {...otherProps}
      />
      {children}
    </div>
  )
}

const DmsLatitude = props => {
  return (
    <MaskedCoordinate
      placeholder="dd°mm'ss.sss&quot;"
      mask={latitudeDMSMask}
      placeholderChar="_"
      {...props}
      onBlur={event => {
        props.onChange(pad(props.value), event.type)
      }}
    />
  )
}

const DmsLongitude = props => {
  return (
    <MaskedCoordinate
      placeholder="ddd°mm'ss.sss&quot;"
      mask={longitudeDMSMask}
      placeholderChar="_"
      {...props}
      onBlur={event => {
        props.onChange(pad(props.value), event.type)
      }}
    />
  )
}

const DdLatitude = props => {
  return (
    <Coordinate
      placeholder="latitude"
      type="number"
      step="any"
      min={-90}
      max={90}
      addon="°"
      {...props}
    />
  )
}

const DdLongitude = props => {
  return (
    <Coordinate
      placeholder="longitude"
      type="number"
      step="any"
      min={-180}
      max={180}
      addon="°"
      {...props}
    />
  )
}

const UsngCoordinate = props => {
  return (
    <div className="coordinate">
      <TextField label="Grid" {...props} />
    </div>
  )
}

const pad = (coordinate = '') => {
  const matches = dmsRegex.exec(coordinate)
  if (!matches) {
    return coordinate
  }
  let deg = matches[1]
  let min = matches[2]
  let sec = matches[3]
  deg = padComponent(deg)
  min = padComponent(min)
  sec = padComponent(sec)
  return deg + '°' + min + "'" + sec + '"'
}

const padComponent = (numString = '') => {
  while (numString.includes('_')) {
    if (numString.includes('.')) {
      numString = numString.replace('_', '0')
    } else {
      numString = numString.replace('_', '')
      numString = '0' + numString
    }
  }
  return numString
}

module.exports = {
  DmsLatitude,
  DmsLongitude,
  DdLatitude,
  DdLongitude,
  UsngCoordinate,
}
