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
