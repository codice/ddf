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
import { deserializeDistance, serialize } from './nearFilterHelper'

describe('deserializeDistance', () => {
  it('deserializes distance', () => {
    expect(deserializeDistance({ value: 'value', distance: '4' })).to.equal('4')
  })
  it('defaults distance to 2', () => {
    expect(deserializeDistance('value')).to.equal('2')
  })
})

describe('serialize', () => {
  it('prevents negative non-positive distance', () => {
    expect(serialize('value', -5)).to.deep.equal({
      value: 'value',
      distance: '1',
    })
  })
})
