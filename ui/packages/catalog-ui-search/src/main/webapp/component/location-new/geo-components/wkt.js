const React = require('react')
const TextField = require('../../../react-component/text-field/index.js')
const { Units } = require('../../../react-component/location/common.js')

const { validateWkt } = require('../utils')

const WKT = props => {
  const { wkt, setState } = props

  return (
    <div className="input-location">
      <TextField
        value={wkt}
        onChange={setState(validateInput((draft, value) => (draft.wkt = value)))}
      />
    </div>
  )
}

function validateInput(draft, value) {
  let result = validateWkt(value)
  if (result.valid) {
    return value
  } else {
    console.log(value)
    return value
  }
}
module.exports = WKT
