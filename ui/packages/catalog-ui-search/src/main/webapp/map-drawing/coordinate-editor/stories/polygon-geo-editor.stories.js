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
import * as React from 'react'
import * as ol from 'openlayers'
import {
  storiesOf,
  action,
  select,
  array,
  text,
  number,
  boolean,
} from '../../../react-component/storybook'
import { makePolygonGeo, METERS } from '../../geometry'
import { PolygonGeoEditor } from '../'
import { coordinateUnitList, lengthUnitList } from '../../storybook-helpers'

const stories = storiesOf(
  'map-drawing/coordinate-editor/PolygonGeoEditor',
  module
)

coordinateUnitList.forEach(coordinateUnit => {
  stories.add(coordinateUnit, () => {
    const coordinatesText = text(
      'coordinates',
      `
      [
        [10, 15],
        [10.5, 15.8],
        [11, 16],
        [10, 14.925]
      ]
    `
    )
    let coordinates = [[10, 15], [10.5, 15.8], [11, 16], [10, 14.925]]
    try {
      coordinates = JSON.parse(coordinatesText)
    } catch (e) {
      console.log(e)
    }
    const buffer = number('buffer', 0)
    const bufferUnit = select('buffer unit', lengthUnitList, METERS)
    const geometry = makePolygonGeo('id', coordinates, buffer, bufferUnit)
    return (
      <PolygonGeoEditor
        geo={geometry}
        coordinateUnit={coordinateUnit}
        onUpdateGeo={action('onUpdateGeo')}
      />
    )
  })
})
