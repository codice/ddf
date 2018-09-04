const React = require('react')

const { Units } = require('./common')
const TextField = require('../text-field')

const BaseLine = props => {
  const { label, cursor, geometryKey, unitKey, widthKey } = props

  return (
    <div className="input-location">
      <TextField
        label={label}
        value={JSON.stringify(props[geometryKey])}
        parse={JSON.parse}
        onChange={cursor(geometryKey)}
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

module.exports = BaseLine
