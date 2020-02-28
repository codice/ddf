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
  ErrorComponent,
  validateGeo,
  initialErrorState,
} from '../utils/validation'
const { Units } = require('./common')
const TextField = require('../text-field')
const _ = require('underscore')

const coordinatePairRegex = /-?\d{1,3}(\.\d*)?\s-?\d{1,3}(\.\d*)?/g

function buildWktString(coordinates) {
  return '[[' + coordinates.join('],[') + ']]'
}

function convertWktString(value) {
  if (value.includes('MULTI')) {
    return convertMultiWkt(value.includes('POLYGON'), value)
  } else if (value.includes('POLYGON') && value.endsWith('))')) {
    return convertWkt(value, 4)
  } else if (value.includes('LINESTRING') && value.endsWith(')')) {
    return convertWkt(value, 2)
  }
  return value
}

function convertWkt(value, numCoords) {
  const coordinatePairs = value.match(coordinatePairRegex)
  if (!coordinatePairs || coordinatePairs.length < numCoords) {
    return value
  }
  const coordinates = coordinatePairs.map(coord => coord.replace(' ', ','))
  return buildWktString(coordinates)
}

function convertMultiWkt(isPolygon, value) {
  if (isPolygon && !value.endsWith(')))')) {
    return value
  } else if (!value.endsWith('))')) {
    return value
  }
  const splitter = isPolygon ? '))' : ')'
  const numPoints = isPolygon ? 4 : 2
  let shapes = value
    .split(splitter)
    .map(shape => shape.match(coordinatePairRegex))
  shapes = shapes
    .filter(shape => shape !== null && shape.length >= numPoints)
    .map(shape => shape.map(coordinatePair => coordinatePair.replace(' ', ',')))
  return shapes.length === 0
    ? value
    : shapes.length === 1
      ? buildWktString(shapes[0])
      : '[' + shapes.map(shapeCoords => buildWktString(shapeCoords)) + ']'
}

function getPolygonValue(currentValue, value) {
  // if current value's 1st coord is different
  // from value's first coord, then delete value's last coord
  try {
    const parsedValue = JSON.parse(value)
    const parsedCurrentValue = JSON.parse(currentValue)
    if (
      Array.isArray(parsedValue) &&
      Array.isArray(parsedCurrentValue) &&
      !_.isEqual(parsedValue[0], parsedCurrentValue[0])
    ) {
      parsedValue.splice(-1, 1)
      return JSON.stringify(parsedValue)
    } else {
      return value
    }
  } catch (e) {
    return value
  }
}

const BaseLine = props => {
  const {
    label,
    geometryKey,
    setState,
    setBufferState,
    unitKey,
    widthKey,
    mode,
    polyType,
  } = props
  const [currentValue, setCurrentValue] = useState(
    JSON.stringify(props[geometryKey])
  )
  const [baseLineError, setBaseLineError] = useState(initialErrorState)
  const [bufferError, setBufferError] = useState(initialErrorState)

  useEffect(
    () => {
      const { geometryKey, lineWidth } = props
      setCurrentValue(
        typeof props[geometryKey] === 'string'
          ? props[geometryKey]
          : JSON.stringify(props[geometryKey])
      )
      if (props.drawing) {
        if (
          geometryKey.includes('line') &&
          (lineWidth === undefined || Number(lineWidth) <= 0)
        ) {
          setState({ [widthKey]: 1 })
          setBufferError(initialErrorState)
        }
        setBaseLineError(initialErrorState)
      }
    },
    [props.polygon, props.line]
  )

  return (
    <div>
      <div className="input-location">
        <TextField
          label={label}
          value={currentValue}
          onChange={value => {
            value = convertWktString(value.trim())
            if (geometryKey.includes('poly')) {
              value = getPolygonValue(currentValue, value)
            }
            setCurrentValue(value)
            try {
              setState({ [geometryKey]: JSON.parse(value) })
            } catch (e) {
              // Set state with invalid value to trigger error messaging
              setState({ [geometryKey]: value })
            }
          }}
          onBlur={() =>
            setBaseLineError(validateGeo(mode || polyType, currentValue))
          }
        />
        <ErrorComponent errorState={baseLineError} />
        <Units
          value={props[unitKey]}
          onChange={value => {
            typeof setBufferState === 'function'
              ? setBufferState(unitKey, value)
              : setState({ [unitKey]: value })
            if (widthKey === 'lineWidth' || 'bufferWidth') {
              setBufferError(
                validateGeo(widthKey, {
                  value: props[widthKey],
                  units: value,
                })
              )
            }
          }}
        >
          <TextField
            type="number"
            label="Buffer width"
            value={String(props[widthKey])}
            onChange={value => {
              typeof setBufferState === 'function'
                ? setBufferState(widthKey, value)
                : setState({ [widthKey]: value })
            }}
            onBlur={e => {
              setBufferError(
                validateGeo(widthKey, {
                  value: e.target.value,
                  units: props[unitKey],
                })
              )
            }}
          />
        </Units>
        <ErrorComponent errorState={bufferError} />
      </div>
    </div>
  )
}

module.exports = BaseLine
