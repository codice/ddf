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
import sinon from 'sinon'
import {
  mock as mockJquery,
  unmock as unmockJquery,
} from '../test/mock-api/mock-jquery'
import {
  mock as mockProperties,
  unmock as unmockProperties,
} from '../test/mock-api/mock-properties'

describe('Common', () => {
  before(() => {
    mockJquery()
    mockProperties()
  })
  after(() => {
    unmockJquery()
    unmockProperties()
  })
  describe('wrapCoordinatesFromGeometry', () => {
    const olUtils = require('./OpenLayersGeometryUtils')
    class MockGeometry {
      constructor(props) {
        this.props = props
      }
      getType() {
        return this.props.type
      }
      getCoordinates() {
        return this.props.coordinates
      }
      getCenter() {
        return this.props.center
      }
      setCenter() {
        throw new Error()
      }
      setCoordinates() {
        throw new Error()
      }
    }
    const sandbox = sinon.createSandbox()
    afterEach(() => {
      sandbox.restore()
    })
    it('LineString wraps coordinates', () => {
      const coordinates = [[210, 50], [0, 0], [-240, -15]].map(
        olUtils.lonLatToMapCoordinate
      )
      const line = new MockGeometry({
        type: 'LineString',
        coordinates,
      })
      const stub = sandbox.stub(line, 'setCoordinates')
      olUtils.wrapCoordinatesFromGeometry(line)
      const calls = stub.getCalls()
      const results = calls[0].args[0].map(olUtils.mapCoordinateToLonLat)
      expect(results[0][0]).to.be.closeTo(-150, 0.001)
      expect(results[0][1]).to.be.closeTo(50, 0.001)
      expect(results[1][0]).to.be.closeTo(-0, 0.001)
      expect(results[1][1]).to.be.closeTo(0, 0.001)
      expect(results[2][0]).to.be.closeTo(120, 0.001)
      expect(results[2][1]).to.be.closeTo(-15, 0.001)
      expect(results.length).to.equal(3)
    })
    it('Polygon wraps coordinates', () => {
      const coordinates = [[210, 50], [0, 0], [-240, -15], [210, 50]].map(
        olUtils.lonLatToMapCoordinate
      )
      const line = new MockGeometry({
        type: 'Polygon',
        coordinates: [coordinates],
      })
      const stub = sandbox.stub(line, 'setCoordinates')
      olUtils.wrapCoordinatesFromGeometry(line)
      const calls = stub.getCalls()
      const results = calls[0].args[0][0].map(olUtils.mapCoordinateToLonLat)
      expect(results[0][0]).to.be.closeTo(-150, 0.001)
      expect(results[0][1]).to.be.closeTo(50, 0.001)
      expect(results[1][0]).to.be.closeTo(-0, 0.001)
      expect(results[1][1]).to.be.closeTo(0, 0.001)
      expect(results[2][0]).to.be.closeTo(120, 0.001)
      expect(results[2][1]).to.be.closeTo(-15, 0.001)
      expect(results[3][0]).to.equal(results[0][0])
      expect(results[3][1]).to.equal(results[0][1])
      expect(results.length).to.equal(4)
    })
    it('Circle wraps coordinates', () => {
      const coordinates = olUtils.lonLatToMapCoordinate([210, 50])
      const line = new MockGeometry({
        type: 'Circle',
        center: coordinates,
      })
      const stub = sandbox.stub(line, 'setCenter')
      olUtils.wrapCoordinatesFromGeometry(line)
      const calls = stub.getCalls()
      const results = olUtils.mapCoordinateToLonLat(calls[0].args[0])
      expect(results[0]).to.be.closeTo(-150, 0.001)
      expect(results[1]).to.be.closeTo(50, 0.001)
    })
  })
})
