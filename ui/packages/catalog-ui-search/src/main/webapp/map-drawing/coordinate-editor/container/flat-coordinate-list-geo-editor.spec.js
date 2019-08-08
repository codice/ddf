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
import { FlatCoordinateListGeoEditor } from './flat-coordinate-list-geo-editor'

describe('<FlatCoordinateListGeoEditor />', () => {
  it('render', () => {
    const startGeo = makeEmptyGeometry('', 'Polygon')
    startGeo.geometry.coordinates = [
      [10, 12],
      [30, 50],
      [45, 34],
      [32, 24],
      [10, 12],
    ]
    startGeo.bbox = [10, 12, 45, 50]
    startGeo.properties.buffer = 1
    startGeo.properties.bufferUnit = 'meters'
    const wrapper = shallow(
      <FlatCoordinateListGeoEditor
        geo={startGeo}
        coordinateUnit={LAT_LON}
        onUpdateGeo={geo => geo}
        getCoordinatesFromGeo={() => [[10, 12], [30, 50], [45, 34], [32, 24]]}
        updateGeoCoordinates={() => {}}
      />
    )
    expect(wrapper.find('FlatCoordinateListEditor').prop('buffer')).to.equal(1)
    expect(
      wrapper.find('FlatCoordinateListEditor').prop('bufferUnit')
    ).to.equal('meters')
    expect(
      wrapper.find('FlatCoordinateListEditor').prop('coordinateList')
    ).to.deep.equal([[10, 12], [30, 50], [45, 34], [32, 24]])
    expect(
      wrapper.find('FlatCoordinateListEditor').prop('coordinateUnit')
    ).to.equal(LAT_LON)
    expect(wrapper.find('FlatCoordinateListEditor').prop('lat')).to.equal(12)
    expect(wrapper.find('FlatCoordinateListEditor').prop('lon')).to.equal(10)
    expect(wrapper.exists()).to.equal(true)
  })
  describe('set properties', () => {
    const startCoordinates = [[10, 12], [30, 50], [45, 34], [32, 24]]
    const getWrapper = (expected, onUpdateGeo) => {
      const startGeo = makeEmptyGeometry('', 'Line')
      startGeo.geometry.coordinates = startCoordinates
      startGeo.bbox = [10, 12, 45, 50]
      return shallow(
        <FlatCoordinateListGeoEditor
          getCoordinatesFromGeo={() => startCoordinates}
          updateGeoCoordinates={(geo, coordinates) => {
            expect(coordinates).to.deep.equal(expected)
            return geo
          }}
          geo={startGeo}
          coordinateUnit={LAT_LON}
          onUpdateGeo={onUpdateGeo}
        />
      )
    }
    it('setBuffer', done => {
      const wrapper = getWrapper(startCoordinates, geo => {
        expect(geo.properties.buffer).to.equal(12.7)
        done()
      })
      wrapper.find('FlatCoordinateListEditor').prop('setBuffer')(12.7)
    })
    it('setUnit', done => {
      const wrapper = getWrapper(startCoordinates, geo => {
        expect(geo.properties.bufferUnit).to.equal('nautical miles')
        done()
      })
      wrapper.find('FlatCoordinateListEditor').prop('setUnit')('nautical miles')
    })
    describe('edit coordinates', () => {
      const testSetCoordinateFactory = action => (
        done,
        editIndex,
        expected
      ) => {
        const updateGeo = geo => {
          done()
          return geo
        }
        const wrapper = getWrapper(expected, updateGeo)
        wrapper.setState({ editIndex })
        wrapper.find('FlatCoordinateListEditor').prop(action)(15.8, 13.6)
      }
      describe('setCoordinate', () => {
        const testSetCoordinate = testSetCoordinateFactory('setCoordinate')
        it('first coordinate', done => {
          testSetCoordinate(done, 0, [
            [13.6, 15.8],
            [30, 50],
            [45, 34],
            [32, 24],
          ])
        })
        it('middle coordinate', done => {
          testSetCoordinate(done, 1, [
            [10, 12],
            [13.6, 15.8],
            [45, 34],
            [32, 24],
          ])
        })
        it('last coordinate', done => {
          testSetCoordinate(done, 3, [
            [10, 12],
            [30, 50],
            [45, 34],
            [13.6, 15.8],
          ])
        })
      })
      describe('addCoordinate', () => {
        const testSetCoordinate = testSetCoordinateFactory('addCoordinate')
        it('first coordinate', done => {
          testSetCoordinate(done, 0, [
            [10, 12],
            [0, 0],
            [30, 50],
            [45, 34],
            [32, 24],
          ])
        })
        it('middle coordinate', done => {
          testSetCoordinate(done, 1, [
            [10, 12],
            [30, 50],
            [0, 0],
            [45, 34],
            [32, 24],
          ])
        })
        it('last coordinate', done => {
          testSetCoordinate(done, 3, [
            [10, 12],
            [30, 50],
            [45, 34],
            [32, 24],
            [0, 0],
          ])
        })
      })
      describe('deleteCoordinate', () => {
        const testSetCoordinate = testSetCoordinateFactory('deleteCoordinate')
        it('first coordinate', done => {
          testSetCoordinate(done, 0, [[30, 50], [45, 34], [32, 24]])
        })
        it('middle coordinate', done => {
          testSetCoordinate(done, 1, [[10, 12], [45, 34], [32, 24]])
        })
        it('last coordinate', done => {
          testSetCoordinate(done, 3, [[10, 12], [30, 50], [45, 34]])
        })
      })
    })
  })
})
