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
