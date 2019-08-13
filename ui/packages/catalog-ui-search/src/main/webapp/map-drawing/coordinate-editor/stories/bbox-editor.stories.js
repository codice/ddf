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
import { makeEmptyGeometry } from '../../geometry'
import { BBoxEditor } from '../'
import { coordinateUnitList } from '../../storybook-helpers'

const stories = storiesOf('map-drawing/coordinate-editor/BBoxEditor', module)

coordinateUnitList.forEach(unit => {
  stories.add(unit, () => {
    const extent = array('extent (4 numbers)', [0, 0, 50, 50])
    return (
      <BBoxEditor setExtent={action('setExtent')} extent={extent} unit={unit} />
    )
  })
})
