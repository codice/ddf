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

const { Radio, RadioItem } = require('../../../react-component/radio/index.js')
const TextField = require('../../../react-component/text-field/index.js')
const { Units } = require('../../../react-component/location/common.js')
const ListEditor = require('../inputs/list-editor')
const { UsngCoordinate } = require('./coordinates')
const { validateUsngGrid, errorMessages } = require('../utils')

const Point = props => {
  const { usng, setState } = props
  return (
    <UsngCoordinate
      value={usng.point}
      onChange={setState((draft, value) => (draft.usng.point = value))}
    />
  )
}

const Circle = props => {
  const { usng, setState } = props
  return (
    <div>
      <UsngCoordinate
        value={usng.circle.point}
        onChange={setState((draft, value) => (draft.usng.circle.point = value))}
      />
      <Units
        value={usng.circle.units}
        onChange={setState((draft, value) => (draft.usng.circle.units = value))}
      >
        <TextField
          label="Radius"
          type="number"
          value={usng.circle.radius}
          onChange={setState(
            (draft, value) => (draft.usng.circle.radius = value)
          )}
        />
      </Units>
    </div>
  )
}

const Line = props => {
  const { usng, setState } = props
  const grids = usng.line.list.map((entry, index) => (
    <UsngCoordinate
      value={usng.line.list[index]}
      onChange={setState(
        (draft, value) => (draft.usng.line.list[index] = value)
      )}
      key={index}
    />
  ))

  return (
    <ListEditor
      list={usng.line.list}
      defaultItem=""
      onChange={setState((draft, value) => (draft.usng.line.list = value))}
    >
      {grids}
    </ListEditor>
  )
}

const Polygon = props => {
  const { usng, setState } = props
  const grids = usng.polygon.list.map((entry, index) => (
    <UsngCoordinate
      value={usng.polygon.list[index]}
      onChange={setState(
        (draft, value) => (draft.usng.polygon.list[index] = value)
      )}
      key={index}
    />
  ))

  return (
    <ListEditor
      list={usng.polygon.list}
      defaultItem=""
      onChange={setState((draft, value) => (draft.usng.polygon.list = value))}
    >
      {grids}
    </ListEditor>
  )
}

const BoundingBox = props => {
  const { usng, setState } = props
  return (
    <UsngCoordinate
      value={usng.boundingbox}
      onChange={setState((draft, value) => (draft.usng.boundingbox = value))}
    />
  )
}

const USNG = props => {
  const { usng, setState } = props

  const inputs = {
    point: Point,
    circle: Circle,
    line: Line,
    polygon: Polygon,
    boundingbox: BoundingBox,
  }

  const Component = inputs[usng.shape] || null

  return (
    <div>
      <Radio
        value={usng.shape}
        onChange={setState((draft, value) => (draft.usng.shape = value))}
      >
        <RadioItem value="point">Point</RadioItem>
        <RadioItem value="circle">Circle</RadioItem>
        <RadioItem value="line">Line</RadioItem>
        <RadioItem value="polygon">Polygon</RadioItem>
        <RadioItem value="boundingbox">Bounding Box</RadioItem>
      </Radio>
      <div className="input-location">
        {Component !== null ? <Component {...props} /> : null}
      </div>
    </div>
  )
}

module.exports = USNG
