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
import styled from '../styles/styled-components'

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
              this.setState({ value })
              const fn = cursor(geometryKey)
              try {
                fn(JSON.parse(value))
              } catch (e) {}
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
              value={props[widthKey]}
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
  validateListOfPoints(coordinates) {
    let message = ''
    if (this.props.mode === 'poly' && coordinates.length < 4) {
      message = 'Minimum of 4 points needed for polygon'
    } else if (this.props.mode === 'line' && coordinates.length < 2) {
      message = 'Minimum of 2 points needed for line'
    }
    coordinates.forEach(point => {
      if (
        point.length !== 2 ||
        (Number.isNaN(Number.parseFloat(point[0])) &&
          Number.isNaN(Number.parseFloat(point[1])))
      ) {
        message = JSON.stringify(point) + ' is not a valid point.'
      } else {
        if (
          point[0] > 180 ||
          point[0] < -180 ||
          point[1] > 90 ||
          point[1] < -90
        ) {
          message = JSON.stringify(point) + ' is not a valid point.'
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
}

module.exports = BaseLine
