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

const { Menu, MenuItem } = require('../menu')
const Dropdown = require('../dropdown')
const Group = require('../group')

const Label = require('./label')

const Units = ({ value, onChange, children }) => (
  <Group>
    {children}
    <span className="input-group-btn">
      <Dropdown label={value}>
        <Menu value={value} onChange={onChange}>
          <MenuItem value="meters" />
          <MenuItem value="kilometers" />
          <MenuItem value="feet" />
          <MenuItem value="yards" />
          <MenuItem value="miles" />
          <MenuItem value="nautical miles" />
        </Menu>
      </Dropdown>
    </span>
  </Group>
)

const range = [...Array(61).keys()]
const Zone = ({ value, onChange }) => (
  <Group>
    <Label>Zone</Label>
    <Dropdown label={value}>
      <Menu value={value} onChange={onChange}>
        {range.map(zone => (
          <MenuItem key={zone} value={zone} />
        ))}
      </Menu>
    </Dropdown>
  </Group>
)

const Hemisphere = ({ value, onChange }) => (
  <Group>
    <Label>Hemisphere</Label>
    <Dropdown label={value}>
      <Menu value={value} onChange={onChange}>
        <MenuItem value="Northern" />
        <MenuItem value="Southern" />
      </Menu>
    </Dropdown>
  </Group>
)

module.exports = {
  Units,
  Zone,
  Hemisphere,
}
