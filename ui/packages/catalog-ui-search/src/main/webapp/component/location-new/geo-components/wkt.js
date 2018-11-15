const React = require('react')
const TextField = require('../../../react-component/text-field/index.js')
const { Units } = require('../../../react-component/location/common.js')

const WKT = props => {
  const { wkt, setState } = props

  return (
    <div className="input-location">
      <TextField
        value={wkt}
        onChange={setState((draft, value) => (draft.wkt = value))}
      />
    </div>
  )
}

module.exports = WKT
