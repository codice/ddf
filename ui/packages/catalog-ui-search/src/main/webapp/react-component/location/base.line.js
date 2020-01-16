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

const { Units } = require('./common')
const TextField = require('../text-field')
import styled from 'styled-components'

const coordinatePairRegex = /-?\d{1,3}(\.\d*)?\s-?\d{1,3}(\.\d*)?/g

const Invalid = styled.div`
  background-color: ${props => props.theme.negativeColor};
  height: 100%;
  display: block;
  overflow: hidden;
  color: white;
`

class BaseLine extends React.Component {
  invalidMessage = ''
  constructor(props) {
    super(props)
    const { geometryKey } = props
    const value = JSON.stringify(props[geometryKey])
    this.state = { value, isValid: true }
    this.state.isValid = true
    this.is2DArray = this.is2DArray.bind(this)
    this.validateListOfPoints = this.validateListOfPoints.bind(this)
    this.isValidPolygon = this.isValidPolygon.bind(this)
    this.isValidInput = this.isValidInput.bind(this)
    this.removeErrorBox = this.removeErrorBox.bind(this)
  }
  componentWillReceiveProps(props) {
    if (document.activeElement !== this.ref) {
      const { geometryKey } = props
      const value = JSON.stringify(props[geometryKey])
      this.setState({ value })
    }
  }
  render() {
    const props = this.props
    const { label, cursor, geometryKey, unitKey, widthKey } = props
    return (
      <React.Fragment>
        <div className="input-location">
          <TextField
            label={label}
            value={this.state.value}
            onChange={value => {
              value = value.trim()
              if (value.includes('MULTI')) {
                value = this.convertMultiWkt(value.includes('POLYGON'), value)
              } else if (value.includes('POLYGON') && value.endsWith('))')) {
                value = this.convertWkt(value, 4)
              } else if (value.includes('LINESTRING') && value.endsWith(')')) {
                value = this.convertWkt(value, 2)
              }
              this.setState({ value })
              const fn = cursor(geometryKey)
              try {
                fn(JSON.parse(value))
              } catch (e) {
                // do nothing
              }
            }}
            onBlur={() => this.isValidInput(this.state.value)}
            onFocus={value => {
              this.setState({ isValid: true })
            }}
          />
          <Units value={props[unitKey]} onChange={cursor(unitKey)}>
            <TextField
              type="number"
              label="Buffer width"
              min={0.000001}
              value={String(props[widthKey])}
              onChange={cursor(widthKey)}
            />
          </Units>
        </div>
        {this.state.isValid ? (
          ''
        ) : (
          <Invalid>
            &nbsp;
            <span className="fa fa-exclamation-triangle" />
            &nbsp; {this.invalidMessage} &nbsp; &nbsp;
            <span className="fa fa-times" onClick={this.removeErrorBox} />
          </Invalid>
        )}
      </React.Fragment>
    )
  }
  removeErrorBox() {
    this.setState({ isValid: true })
  }
  isValidInput(value) {
    this.invalidMessage = ''
    this.setState({ value, isValid: this.isValidPolygon(value) })
  }
  is2DArray(coordinates) {
    try {
      const parsedCoords = JSON.parse(coordinates)
      return Array.isArray(parsedCoords) && Array.isArray(parsedCoords[0])
    } catch (e) {
      return false
    }
  }
  validatePoint(point) {
    if (
      point.length !== 2 ||
      Number.isNaN(Number.parseFloat(point[0])) ||
      Number.isNaN(Number.parseFloat(point[1])) ||
      point[0] > 180 ||
      point[0] < -180 ||
      point[1] > 90 ||
      point[1] < -90
    ) {
      return JSON.stringify(point) + ' is not a valid point.'
    }
    return ''
  }
  validateListOfPoints(coordinates) {
    let message = ''
    const isLine = this.props.mode.includes('line')
    const numPoints = isLine ? 2 : 4
    if (
      !this.props.mode.includes('multi') &&
      !coordinates.some(coords => coords.length > 2) &&
      coordinates.length < numPoints
    ) {
      message = `Minimum of ${numPoints} points needed for ${
        isLine ? 'Line' : 'Polygon'
      }`
    }
    coordinates.forEach(coordinate => {
      if (coordinate.length > 2) {
        coordinate.forEach(coord => {
          if (this.validatePoint(coord)) {
            message = this.validatePoint(coord)
          }
        })
      } else {
        if (this.props.mode.includes('multi')) {
          message = `Switch to ${isLine ? 'Line' : 'Polygon'}`
        } else if (this.validatePoint(coordinate)) {
          message = this.validatePoint(coordinate)
        }
      }
    })
    if (message !== '') {
      this.invalidMessage = message
      throw 'Invalid coordinates.'
    }
  }
  isValidPolygon(coordinates) {
    if (!this.is2DArray(coordinates)) {
      this.invalidMessage = 'Not an acceptable value.'
      return false
    }
    try {
      this.validateListOfPoints(JSON.parse(coordinates))
      return true
    } catch (e) {
      return false
    }
  }
  buildWktString(coordinates) {
    return '[[' + coordinates.join('],[') + ']]'
  }
  convertWkt(value, numCoords) {
    const coordinatePairs = value.match(coordinatePairRegex)
    if (!coordinatePairs || coordinatePairs.length < numCoords) {
      return value
    }
    const coordinates = coordinatePairs.map(coord => coord.replace(' ', ','))
    return this.buildWktString(coordinates)
  }
  convertMultiWkt(isPolygon, value) {
    if (isPolygon && !value.endsWith(')))')) {
      return value
    } else if (!value.endsWith('))')) {
      return value
    }
    const splitter = isPolygon ? '))' : ')'
    const numPoints = isPolygon ? 4 : 2
    const shapes = value
      .split(splitter)
      .map(shape => shape.match(coordinatePairRegex))
      .filter(shape => shape !== null && shape.length >= numPoints)
      .map(shape =>
        shape.map(coordinatePair => coordinatePair.replace(' ', ','))
      )
    return shapes.length === 0
      ? value
      : shapes.length === 1
        ? this.buildWktString(shapes[0])
        : '[' +
          shapes.map(shapeCoords => this.buildWktString(shapeCoords)) +
          ']'
  }
}

module.exports = BaseLine
