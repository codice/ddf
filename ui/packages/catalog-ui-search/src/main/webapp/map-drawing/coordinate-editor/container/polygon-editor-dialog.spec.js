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
import React from 'react'
import { expect } from 'chai'
import { shallow } from 'enzyme'
import { makeEmptyGeometry } from '../../geometry'
import PolygonEditorDialog from './polygon-editor-dialog'

describe('<PolygonEditorDialog />', () => {
  let startGeo
  beforeEach(() => {
    startGeo = makeEmptyGeometry('', 'Line')
    startGeo.geometry.coordinates = [
      [[10, 12], [30, 50], [45, 34], [32, 24], [10, 12]],
    ]
    startGeo.bbox = [10, 12, 45, 50]
  })
  it('render', () => {
    const wrapper = shallow(
      <PolygonEditorDialog geo={startGeo} onOk={() => {}} />
    )
    expect(wrapper.exists()).to.equal(true)
  })
})
