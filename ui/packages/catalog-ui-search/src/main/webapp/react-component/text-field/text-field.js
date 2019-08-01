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
import React from 'react'
import PropTypes from 'prop-types'

import styled from '../styles/styled-components'
import { readableColor, rgba } from 'polished'

import Group from '../group'

const foreground = props => {
  if (props.theme.backgroundDropdown !== undefined) {
    return readableColor(props.theme.backgroundDropdown)
  }
}

const background = props => {
  if (props.theme.backgroundDropdown !== undefined) {
    return rgba(readableColor(props.theme.backgroundDropdown), 0.1)
  }
}

const Input = styled.input`
  padding: 0px ${props => props.theme.minimumSpacing};
  display: inline-block;
  width: 100%;
  box-sizing: border-box;
  white-space: nowrap;
  height: ${props => props.theme.minimumButtonSize};
  line-height: ${props => props.theme.minimumButtonSize};
  border-radius: ${props => props.theme.borderRadius};
  outline: none;
  color: ${foreground};
  border: 2px solid ${background};
  background-color: ${background};
`

const TextField = props => {
  const { label, addon, value, type = 'text', onChange, ...rest } = props
  return (
    <Group>
      {label !== undefined ? (
        <span className="input-group-addon">
          {label}
          &nbsp;
        </span>
      ) : null}
      <Input
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
  )
}

TextField.propTypes = {
  /** The current input value. */
  value: PropTypes.string,
  /** Value change handler. */
  onChange: PropTypes.func,
}

module.exports = TextField
