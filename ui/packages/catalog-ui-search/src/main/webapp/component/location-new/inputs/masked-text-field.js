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
const Group = require('../../../react-component/group/index.js')
const MaskedInput = require('react-text-mask').default
const CustomElements = require('../../../js/CustomElements.js')
const Component = CustomElements.registerReact('text-field')

class MaskedTextField extends React.Component {
  prevEvent = undefined

  padEndWithZeros(value) {
    // This function is called for each field (east, west, south, north) multiple times.
    // Sometimes the event variable is defined, other times it's undefined.
    // We must capture the event in a variable when it's defined
    // in order to leverage it in checks below even when it's undefined.
    if (event) {
      this.prevEvent = event
    }
    if (
      value === undefined ||
      !value.includes('.') ||
      (((event && event.type === 'input') || this.prevEvent.type === 'input') &&
        this.prevEvent.target.id === this.props.label)
    ) {
      return value
    }
    const dmsCoordinateParts = value.toString().split("'")
    if (dmsCoordinateParts.length < 2) {
      return value
    }
    const decimalParts = dmsCoordinateParts[1].toString().split('.')
    if (decimalParts.length < 2) {
      return value
    }
    let decimal = decimalParts[1].replace('"', '')
    if (!decimal.endsWith('_')) {
      return value
    }
    const indexOfUnderscore = decimal.indexOf('_')
    const decimalLength = decimal.length
    decimal = decimal.substring(0, indexOfUnderscore)
    return (
      dmsCoordinateParts[0] +
      "'" +
      decimalParts[0] +
      '.' +
      decimal.padEnd(decimalLength, '0') +
      '"'
    )
  }

  render() {
    // eslint-disable-next-line no-unused-vars
    const { label, addon, onChange, value, ...args } = this.props
    return (
      <Component>
        <Group>
          {label != null ? (
            <span className="input-group-addon">
              {label}
              &nbsp;
            </span>
          ) : null}
          <MaskedInput
            value={value}
            keepCharPositions
            onChange={e => {
              this.props.onChange(e.target.value)
            }}
            pipe={value => this.padEndWithZeros(value)}
            render={(setRef, { defaultValue, ...props }) => {
              return (
                <input
                  id={label}
                  ref={ref => {
                    setRef(ref)
                    this.ref = ref
                  }}
                  value={defaultValue || ''}
                  {...props}
                />
              )
            }}
            {...args}
          />
          {addon != null ? (
            <label className="input-group-addon">{addon}</label>
          ) : null}
        </Group>
      </Component>
    )
  }
}

module.exports = MaskedTextField
