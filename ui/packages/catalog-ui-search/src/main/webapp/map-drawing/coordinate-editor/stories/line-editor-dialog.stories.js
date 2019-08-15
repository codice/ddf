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
import { LineEditorDialog } from '../'

const stories = storiesOf(
  'map-drawing/coordinate-editor/LineEditorDialog',
  module
)

const makeGeometry = () => makeEmptyGeometry('id', 'Line')

stories.add('basic', () => {
  const geo = makeGeometry()
  return <LineEditorDialog geo={geo} onOk={action('onOk')} />
})