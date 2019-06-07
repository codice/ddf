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
const { DmsLatitude, DmsLongitude } = require('./coordinates')
const { validateDmsPoint, errorMessages } = require('../utils')
const { dmsPoint } = require('../models')
const DirectionInput = require('./direction')
const { Direction } = require('../utils/dms-utils')

const latitudeDirections = [Direction.North, Direction.South]
const longitudeDirections = [Direction.East, Direction.West]

const Point = props => {
  const { dms, setState } = props
  return (
    <Group>
      <DmsLatitude
        value={dms.point.latitude.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.point.latitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dms.point.latitude.direction}
          onChange={setState(
            (draft, value) => (draft.dms.point.latitude.direction = value)
          )}
        />
      </DmsLatitude>
      <DmsLongitude
        value={dms.point.longitude.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.point.longitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dms.point.longitude.direction}
          onChange={setState(
            (draft, value) => (draft.dms.point.longitude.direction = value)
          )}
        />
      </DmsLongitude>
    </Group>
  )
}

const Circle = props => {
  const { dms, setState } = props
  return (
    <div>
      <Group>
        <DmsLatitude
          value={dms.circle.point.latitude.coordinate}
          onChange={setState(
            (draft, value) =>
              (draft.dms.circle.point.latitude.coordinate = value)
          )}
        >
          <DirectionInput
            options={latitudeDirections}
            value={dms.circle.point.latitude.direction}
            onChange={setState(
              (draft, value) =>
                (draft.dms.circle.point.latitude.direction = value)
            )}
          />
        </DmsLatitude>
        <DmsLongitude
          value={dms.circle.point.longitude.coordinate}
          onChange={setState(
            (draft, value) =>
              (draft.dms.circle.point.longitude.coordinate = value)
          )}
        >
          <DirectionInput
            options={longitudeDirections}
            value={dms.circle.point.longitude.direction}
            onChange={setState(
              (draft, value) =>
                (draft.dms.circle.point.longitude.direction = value)
            )}
          />
        </DmsLongitude>
      </Group>
      <Units
        value={dms.circle.units}
        onChange={setState((draft, value) => (draft.dms.circle.units = value))}
      >
        <TextField
          label="Radius"
          type="number"
          value={dms.circle.radius}
          onChange={setState(
            (draft, value) => (draft.dms.circle.radius = value)
          )}
        />
      </Units>
    </div>
  )
}

const Line = props => {
  const { dms, setState } = props
  const points = dms.line.list.map((entry, index) => (
    <Group key={index}>
      <DmsLatitude
        value={dms.line.list[index].latitude.coordinate}
        onChange={setState(
          (draft, value) =>
            (draft.dms.line.list[index].latitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dms.line.list[index].latitude.direction}
          onChange={setState(
            (draft, value) =>
              (draft.dms.line.list[index].latitude.direction = value)
          )}
        />
      </DmsLatitude>
      <DmsLongitude
        value={dms.line.list[index].longitude.coordinate}
        onChange={setState(
          (draft, value) =>
            (draft.dms.line.list[index].longitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dms.line.list[index].longitude.direction}
          onChange={setState(
            (draft, value) =>
              (draft.dms.line.list[index].longitude.direction = value)
          )}
        />
      </DmsLongitude>
    </Group>
  ))

  return (
    <ListEditor
      list={dms.line.list}
      defaultItem={dmsPoint}
      onChange={setState((draft, value) => (draft.dms.line.list = value))}
    >
      {points}
    </ListEditor>
  )
}

const Polygon = props => {
  const { dms, setState } = props
  const points = dms.polygon.list.map((entry, index) => (
    <Group key={index}>
      <DmsLatitude
        value={dms.polygon.list[index].latitude.coordinate}
        onChange={setState(
          (draft, value) =>
            (draft.dms.polygon.list[index].latitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dms.polygon.list[index].latitude.direction}
          onChange={setState(
            (draft, value) =>
              (draft.dms.polygon.list[index].latitude.direction = value)
          )}
        />
      </DmsLatitude>
      <DmsLongitude
        value={dms.polygon.list[index].longitude.coordinate}
        onChange={setState(
          (draft, value) =>
            (draft.dms.polygon.list[index].longitude.coordinate = value)
        )}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dms.polygon.list[index].longitude.direction}
          onChange={setState(
            (draft, value) =>
              (draft.dms.polygon.list[index].longitude.direction = value)
          )}
        />
      </DmsLongitude>
    </Group>
  ))

  return (
    <ListEditor
      list={dms.polygon.list}
      defaultItem={dmsPoint}
      onChange={setState((draft, value) => (draft.dms.polygon.list = value))}
    >
      {points}
    </ListEditor>
  )
}

const BoundingBox = props => {
  const { dms, setState } = props
  return (
    <div>
      <DmsLatitude
        label="South"
        value={dms.boundingbox.south.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.boundingbox.south.coordinate = value)
        )}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dms.boundingbox.south.direction}
          onChange={setState(
            (draft, value) => (draft.dms.boundingbox.south.direction = value)
          )}
        />
      </DmsLatitude>
      <DmsLatitude
        label="North"
        value={dms.boundingbox.north.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.boundingbox.north.coordinate = value)
        )}
      >
        <DirectionInput
          options={latitudeDirections}
          value={dms.boundingbox.north.direction}
          onChange={setState(
            (draft, value) => (draft.dms.boundingbox.north.direction = value)
          )}
        />
      </DmsLatitude>
      <DmsLongitude
        label="West"
        value={dms.boundingbox.west.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.boundingbox.west.coordinate = value)
        )}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dms.boundingbox.west.direction}
          onChange={setState(
            (draft, value) => (draft.dms.boundingbox.west.direction = value)
          )}
        />
      </DmsLongitude>
      <DmsLongitude
        label="East"
        value={dms.boundingbox.east.coordinate}
        onChange={setState(
          (draft, value) => (draft.dms.boundingbox.east.coordinate = value)
        )}
      >
        <DirectionInput
          options={longitudeDirections}
          value={dms.boundingbox.east.direction}
          onChange={setState(
            (draft, value) => (draft.dms.boundingbox.east.direction = value)
          )}
        />
      </DmsLongitude>
    </div>
  )
}

const LatLongDMS = props => {
  const { dms, setState } = props

  const inputs = {
    point: Point,
    line: Line,
    circle: Circle,
    polygon: Polygon,
    boundingbox: BoundingBox,
  }

  const Component = inputs[dms.shape] || null

  return (
    <div>
      <Radio
        value={dms.shape}
        onChange={setState((draft, value) => (draft.dms.shape = value))}
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

module.exports = LatLongDMS
