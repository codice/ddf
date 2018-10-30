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
