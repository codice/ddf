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
import { expect } from 'chai'
import ProjectedExtent from './projected-extent'

describe('Extent', () => {
  const USER_PROJECTION = 'EPSG:4326'
  const MAP_PROJECTION = 'EPSG:3857'
  describe('getMapCoordinates', () => {
    it('source coordinates in user projection are converted', () => {
      const extent = new ProjectedExtent(
        USER_PROJECTION,
        MAP_PROJECTION,
        [1, 2, 3, 4],
        false
      )
      expect(extent.getMapCoordinates().length).to.equal(4)
      expect(extent.getMapCoordinates()[0]).to.not.equal(1)
      expect(extent.getMapCoordinates()[1]).to.not.equal(2)
      expect(extent.getMapCoordinates()[2]).to.not.equal(3)
      expect(extent.getMapCoordinates()[3]).to.not.equal(4)
    })
    it('source coordinates in map projection are not converted', () => {
      const extent = new ProjectedExtent(
        USER_PROJECTION,
        MAP_PROJECTION,
        [1, 2, 3, 4],
        true
      )
      expect(extent.getMapCoordinates()).to.deep.equal([1, 2, 3, 4])
    })
  })
  describe('getUserCoordinates', () => {
    it('source coordinates in user projection are not converted', () => {
      const extent = new ProjectedExtent(
        USER_PROJECTION,
        MAP_PROJECTION,
        [1, 2, 3, 4],
        false
      )
      expect(extent.getUserCoordinates()).to.deep.equal([1, 2, 3, 4])
    })
    it('source coordinates in map projection are converted', () => {
      const extent = new ProjectedExtent(
        USER_PROJECTION,
        MAP_PROJECTION,
        [1, 2, 3, 4],
        true
      )
      expect(extent.getUserCoordinates().length).to.equal(4)
      expect(extent.getUserCoordinates()[0]).to.not.equal(1)
      expect(extent.getUserCoordinates()[1]).to.not.equal(2)
      expect(extent.getUserCoordinates()[2]).to.not.equal(3)
      expect(extent.getUserCoordinates()[3]).to.not.equal(4)
    })
  })
})
