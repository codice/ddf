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
