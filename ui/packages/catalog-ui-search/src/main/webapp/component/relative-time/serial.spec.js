const expect = require('chai').expect
import { serialize, deserialize } from './serial'

const inputs = new Map([
  [undefined, undefined],
  [{ last: 1, unit: 'y' }, 'RELATIVE(P1Y)'],
  [{ last: 1, unit: 'M' }, 'RELATIVE(P1M)'],
  [{ last: 1, unit: 'd' }, 'RELATIVE(P1D)'],
  [{ last: 1, unit: 'h' }, 'RELATIVE(PT1H)'],
  [{ last: 1, unit: 'm' }, 'RELATIVE(PT1M)'],
  [{ last: 1.1, unit: 'm' }, 'RELATIVE(PT1.1M)'],
  [{ last: 1.1, unit: 'm' }, 'RELATIVE(PT1.1M)'],
  [{ last: 123.45678, unit: 'm' }, 'RELATIVE(PT123.45678M)'],
])

describe('Parse relative time', () => {
  it('serialize', () => {
    for (let [input, expected] of inputs) {
      expect(serialize(input)).to.deep.equal(expected)
    }
  })
  it('deserialize', () => {
    for (let [expected, input] of inputs) {
      expect(deserialize(input)).to.deep.equal(expected)
    }
    expect(deserialize('RELATIVE()')).to.equal(undefined)
    expect(deserialize('RELATIVE(PT)')).to.equal(undefined)
    expect(deserialize('RELATIVE(PTM)')).to.equal(undefined)
    expect(deserialize('(PTM)')).to.equal(undefined)
  })
})
