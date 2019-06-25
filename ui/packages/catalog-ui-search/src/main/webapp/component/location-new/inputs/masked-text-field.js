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
const { validateInput } = require('../utils/dms-utils')

class MaskedTextField extends React.Component {
  render() {
    const { label, addon, onChange, value = '', ...args } = this.props
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
            render={(setRef, { defaultValue, ...props }) => {
              return (
                <input
                  ref={ref => {
                    setRef(ref)
                    this.ref = ref
                  }}
                  value={defaultValue}
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
