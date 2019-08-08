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
import { LAT_LON, makeEmptyGeometry } from '../../geometry'
import BBoxEditorDialog, {
  updateGeoWithExtentBBox,
  finalizeGeo,
} from './bbox-editor-dialog'

describe('bboxEditorDialog', () => {
  let startGeo
  beforeEach(() => {
    startGeo = makeEmptyGeometry('', 'Polygon')
    startGeo.geometry.coordinates = [[[0, 0], [0, 0], [0, 0], [0, 0], [0, 0]]]
    startGeo.bbox = [0, 0, 0, 0]
  })
  describe('<BBoxEditorDialog />', () => {
    it('render', () => {
      const wrapper = shallow(
        <BBoxEditorDialog
          geo={startGeo}
          coordinateUnit={LAT_LON}
          onUpdateGeo={() => {}}
        />
      )
      expect(wrapper.exists()).to.equal(true)
    })
  })
  describe('updateGeoWithExtentBBox', () => {
    it('default', () => {
      const updated = updateGeoWithExtentBBox(startGeo, [-5, -10, 5, 10])
      expect(updated.bbox).to.deep.equal([-5, -10, 5, 10])
      expect(updated.geometry.coordinates).to.deep.equal([
        [[-5, -10], [-5, 10], [5, 10], [5, -10], [-5, -10]],
      ])
    })
  })
  describe('finalizeGeo', () => {
    it('reversed coordinates', () => {
      const geo = updateGeoWithExtentBBox(startGeo, [5, 10, -5, -10])
      const updated = finalizeGeo(geo)
      expect(updated.bbox).to.deep.equal([-5, -10, 5, 10])
      expect(updated.geometry.coordinates).to.deep.equal([
        [[-5, -10], [-5, 10], [5, 10], [5, -10], [-5, -10]],
      ])
    })
    it('half reversed coordinates', () => {
      const geo = updateGeoWithExtentBBox(startGeo, [-5, 10, 5, -10])
      const updated = finalizeGeo(geo)
      expect(updated.bbox).to.deep.equal([-5, -10, 5, 10])
      expect(updated.geometry.coordinates).to.deep.equal([
        [[-5, -10], [-5, 10], [5, 10], [5, -10], [-5, -10]],
      ])
    })
  })
})
