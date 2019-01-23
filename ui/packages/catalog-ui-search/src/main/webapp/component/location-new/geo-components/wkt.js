const React = require('react')
const TextField = require('../../../react-component/text-field/index.js')
const { Units } = require('../../../react-component/location/common.js')

const { validateWkt, roundWktCoords } = require('../utils')

const WKT = props => {
  const { wkt, setState } = props

  return (
    <div className="input-location">
      <TextField
        value={roundWktCoords(wkt)}
        onChange={setState((draft, value) =>
          roundWktCoords((draft.wkt = value))
        )}
      />
    </div>
  )
}

module.exports = WKT
