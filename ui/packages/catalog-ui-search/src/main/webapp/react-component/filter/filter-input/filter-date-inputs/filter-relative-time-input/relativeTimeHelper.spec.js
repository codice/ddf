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
import { deserialize, serialize } from './relativeTimeHelper'
import { expect } from 'chai'

describe('deserialize', () => {
  it('deserializes minutes', () => {
    expect(deserialize('RELATIVE(PT10M)')).to.deep.equal({
      last: 10,
      unit: 'm',
    })
  })
  it('deserializes days', () => {
    expect(deserialize('RELATIVE(P2.5D)')).to.deep.equal({
      last: 2.5,
      unit: 'd',
    })
  })
  it('deserializes minutes', () => {
    expect(deserialize('RELATIVE(PT1H)')).to.deep.equal({ last: 1, unit: 'h' })
  })
  it('deserializes minutes', () => {
    expect(deserialize('RELATIVE(P0.5M)')).to.deep.equal({
      last: 0.5,
      unit: 'M',
    })
  })
  it('deserializes minutes', () => {
    expect(deserialize('RELATIVE(P5Y)')).to.deep.equal({ last: 5, unit: 'y' })
  })
})

describe('serialize', () => {
  it('serializes minutes', () => {
    expect(serialize({ last: 10, unit: 'm' })).to.equal('RELATIVE(PT10M)')
  })

  it('serializes days', () => {
    expect(serialize({ last: 2.5, unit: 'd' })).to.equal('RELATIVE(P2.5D)')
  })

  it('serializes hours', () => {
    expect(serialize({ last: 1, unit: 'h' })).to.equal('RELATIVE(PT1H)')
  })

  it('serializes months', () => {
    expect(serialize({ last: 0.5, unit: 'M' })).to.equal('RELATIVE(P0.5M)')
  })

  it('serialize years', () => {
    expect(serialize({ last: 5, unit: 'y' })).to.equal('RELATIVE(P5Y)')
  })
})
