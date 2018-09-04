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

const range = n => {
  const out = []
  for (let i = 0; i < n; i++) {
    out.push(i + 1)
  }
  return out
}

const Zone = ({ value, onChange }) => (
  <Group>
    <Label>Zone</Label>
    <Dropdown label={value}>
      <Menu value={value} onChange={onChange}>
        {range(60).map(zone => (
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
