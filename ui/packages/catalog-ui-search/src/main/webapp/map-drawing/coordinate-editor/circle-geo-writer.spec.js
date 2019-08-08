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
import { makeEmptyGeometry } from '../geometry'
import { updateCircleGeo } from './circle-geo-writer'

describe('updateCircleGeo', () => {
  it('circle', () => {
    const startGeo = makeEmptyGeometry('', 'Point Radius')
    startGeo.geometry.coordinates = [10, 50]
    startGeo.bbox = [10, 50, 10, 50]
    const geo = updateCircleGeo(startGeo, 12.7, 18.5, 128, 'kilometers')
    expect(geo.geometry.coordinates).to.deep.equal([18.5, 12.7])
    expect(geo.properties.buffer).to.equal(128)
    expect(geo.properties.bufferUnit).to.equal('kilometers')
    expect(geo.bbox[0]).to.be.below(18.5)
    expect(geo.bbox[1]).to.be.below(12.7)
    expect(geo.bbox[2]).to.be.above(18.5)
    expect(geo.bbox[3]).to.be.above(12.7)
  })
  it('zero radius circle/point', () => {
    const startGeo = makeEmptyGeometry('', 'Point')
    startGeo.geometry.coordinates = [10, 50]
    startGeo.bbox = [10, 50, 10, 50]
    const geo = updateCircleGeo(startGeo, 12.7, 18.5, 0, 'meters')
    expect(geo.geometry.coordinates).to.deep.equal([18.5, 12.7])
    expect(geo.properties.buffer).to.equal(0)
    expect(geo.properties.bufferUnit).to.equal('meters')
    expect(geo.bbox[0]).to.equal(18.5)
    expect(geo.bbox[1]).to.equal(12.7)
    expect(geo.bbox[2]).to.equal(18.5)
    expect(geo.bbox[3]).to.equal(12.7)
  })
})
