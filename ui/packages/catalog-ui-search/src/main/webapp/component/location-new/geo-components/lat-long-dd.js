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

const Group = require('../../../react-component/group/index.js')
const { Radio, RadioItem } = require('../../../react-component/radio/index.js')
const TextField = require('../../../react-component/text-field/index.js')
const { Units } = require('../../../react-component/location/common.js')
const ListEditor = require('../inputs/list-editor')
const { DdLatitude, DdLongitude } = require('./coordinates')
const { validateDdPoint } = require('../utils')
const { ddPoint } = require('../models')
const DistanceUtils = require('../../../js/DistanceUtils')

const minimumDifference = 0.0001

const Point = props => {
  const { dd, setState } = props
  return (
    <Group>
      <DdLatitude
        value={DistanceUtils.coordinateRound(dd.point.latitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.point.latitude = DistanceUtils.coordinateRound(value))
        )}
      />
      <DdLongitude
        value={DistanceUtils.coordinateRound(dd.point.longitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.point.longitude = DistanceUtils.coordinateRound(value))
        )}
      />
    </Group>
  )
}

const Circle = props => {
  const { dd, setState } = props
  return (
    <div>
      <Group>
        <DdLatitude
          value={DistanceUtils.coordinateRound(dd.circle.point.latitude)}
          onChange={setState(
            (draft, value) =>
              (draft.dd.circle.point.latitude = DistanceUtils.coordinateRound(
                value
              ))
          )}
        />
        <DdLongitude
          value={DistanceUtils.coordinateRound(dd.circle.point.longitude)}
          onChange={setState(
            (draft, value) =>
              (draft.dd.circle.point.longitude = DistanceUtils.coordinateRound(
                value
              ))
          )}
        />
      </Group>
      <Units
        value={dd.circle.units}
        onChange={setState((draft, value) => (draft.dd.circle.units = value))}
      >
        <TextField
          label="Radius"
          type="number"
          value={DistanceUtils.coordinateRound(dd.circle.radius)}
          onChange={setState(
            (draft, value) =>
              (draft.dd.circle.radius = DistanceUtils.coordinateRound(value))
          )}
        />
      </Units>
    </div>
  )
}

const Line = props => {
  const { dd, setState } = props
  const points = dd.line.list.map((entry, index) => (
    <Group key={index}>
      <DdLatitude
        value={DistanceUtils.coordinateRound(dd.line.list[index].latitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.line.list[index].latitude = DistanceUtils.coordinateRound(
              value
            ))
        )}
      />
      <DdLongitude
        value={DistanceUtils.coordinateRound(dd.line.list[index].longitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.line.list[
              index
            ].longitude = DistanceUtils.coordinateRound(value))
        )}
      />
    </Group>
  ))

  return (
    <ListEditor
      list={dd.line.list}
      defaultItem={ddPoint}
      onChange={setState((draft, value) => (draft.dd.line.list = value))}
    >
      {points}
    </ListEditor>
  )
}

const Polygon = props => {
  const { dd, setState } = props
  const points = dd.polygon.list.map((entry, index) => (
    <Group key={index}>
      <DdLatitude
        value={DistanceUtils.coordinateRound(dd.polygon.list[index].latitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.polygon.list[
              index
            ].latitude = DistanceUtils.coordinateRound(value))
        )}
      />
      <DdLongitude
        value={DistanceUtils.coordinateRound(dd.polygon.list[index].longitude)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.polygon.list[
              index
            ].longitude = DistanceUtils.coordinateRound(value))
        )}
      />
    </Group>
  ))

  return (
    <ListEditor
      list={dd.polygon.list}
      defaultItem={ddPoint}
      onChange={setState((draft, value) => (draft.dd.polygon.list = value))}
    >
      {points}
    </ListEditor>
  )
}

const BoundingBox = props => {
  const { dd, setState } = props
  return (
    <div>
      <DdLatitude
        label="South"
        value={DistanceUtils.coordinateRound(dd.boundingbox.south)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.boundingbox.south = DistanceUtils.coordinateRound(value))
        )}
      />
      <DdLatitude
        label="North"
        value={DistanceUtils.coordinateRound(dd.boundingbox.north)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.boundingbox.north = DistanceUtils.coordinateRound(value))
        )}
      />
      <DdLongitude
        label="West"
        value={DistanceUtils.coordinateRound(dd.boundingbox.west)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.boundingbox.west = DistanceUtils.coordinateRound(value))
        )}
      />
      <DdLongitude
        label="East"
        value={DistanceUtils.coordinateRound(dd.boundingbox.east)}
        onChange={setState(
          (draft, value) =>
            (draft.dd.boundingbox.east = DistanceUtils.coordinateRound(value))
        )}
      />
    </div>
  )
}

const LatLongDD = props => {
  const { dd, setState } = props

  const inputs = {
    point: Point,
    line: Line,
    circle: Circle,
    polygon: Polygon,
    boundingbox: BoundingBox,
  }

  const Component = inputs[dd.shape] || null

  return (
    <div>
      <Radio
        value={dd.shape}
        onChange={setState((draft, value) => (draft.dd.shape = value))}
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

module.exports = LatLongDD
