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

class BaseLine extends React.Component {
  constructor(props) {
    super(props)
    const { geometryKey } = props
    const value = JSON.stringify(props[geometryKey])
    this.state = { value }
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
    )
  }
}

module.exports = BaseLine
