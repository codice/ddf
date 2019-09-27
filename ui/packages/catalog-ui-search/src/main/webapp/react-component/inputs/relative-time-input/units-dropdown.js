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

import React, { useEffect, useState } from 'react'
import { Menu, MenuItem } from '../../menu'
import Dropdown from '../../dropdown'

const suggestions = [
  { label: 'Minutes', value: 'm' },
  { label: 'Hours', value: 'h' },
  { label: 'Days', value: 'd' },
  { label: 'Months', value: 'M' },
  { label: 'Years', value: 'y' },
]

const UnitsDropdown = props => {
  const [value, setValue] = useState(
    suggestions.find(suggestion => suggestion.value === props.value) ||
      suggestions[0]
  )

  useEffect(
    () => {
      props.onChange(value.value)
    },
    [value.value]
  )

  return (
    <Dropdown label={value.label}>
      <Menu value={value.label} onChange={setValue}>
        {suggestions.map(suggestion => {
          return (
            <MenuItem key={suggestion.value} value={suggestion}>
              {suggestion.label}
            </MenuItem>
          )
        })}
      </Menu>
    </Dropdown>
  )
}

export default UnitsDropdown
