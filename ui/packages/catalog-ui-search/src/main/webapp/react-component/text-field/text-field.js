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

const Group = require('../group')

const CustomElements = require('../../js/CustomElements.js')
const Component = CustomElements.registerReact('text-field')

const TextField = props => {
  const { label, addon, value, type = 'text', onChange, ...rest } = props
  return (
    <Component>
      <Group>
        {label !== undefined ? (
          <span className="input-group-addon">
            {label}
            &nbsp;
          </span>
        ) : null}
        <input
          value={value !== undefined ? value : ''}
          type={type}
          onChange={e => {
            onChange(e.target.value)
          }}
          {...rest}
        />
        {addon !== undefined ? (
          <label className="input-group-addon">{addon}</label>
        ) : null}
      </Group>
    </Component>
  )
}

module.exports = TextField
