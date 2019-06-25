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
const { getLargestBbox } = require('./gazetteer')
const testData = require('./gazetteer-france-test-data.json')

describe('getLargestBbox', () => {
  const expectedAnswer = {
    maxX: 7.8125,
    minX: -5.1953125,
    maxY: 50.77891890432069,
    minY: 43.37399002495726,
  }
  it(
    'Largest bounding box for France should equal  ' +
      JSON.stringify(expectedAnswer),
    () => {
      const result = getLargestBbox(testData[0].geojson.coordinates, true)
      console.log(result)
      expect(result).to.deep.equal(expectedAnswer)
    }
  )
})
