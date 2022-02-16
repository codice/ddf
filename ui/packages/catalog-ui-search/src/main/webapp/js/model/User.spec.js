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
import User from './User'

describe('User Model', () => {
  describe('Preferences', () => {
    it('get() converts a CQL resultFilter to a filter tree', () => {
      const preferences = new User.Preferences({
        resultFilter: '("anyText" ILIKE \'foo\') OR ("anyText" ILIKE \'bar\')',
      })
      const resultFilter = preferences.get('resultFilter')
      expect(resultFilter).to.be.an('object')
      expect(resultFilter).to.have.property('type', 'OR')
      expect(resultFilter).to.have.deep.property('filters', [
        { type: 'ILIKE', property: 'anyText', value: 'foo' },
        { type: 'ILIKE', property: 'anyText', value: 'bar' },
      ])
    })
    it('get() converts a CQL resultFilter containing NOT to a simplified filter tree', () => {
      const preferences = new User.Preferences({
        resultFilter:
          'NOT (("anyText" ILIKE \'foo\') OR ("anyText" ILIKE \'bar\'))',
      })
      const resultFilter = preferences.get('resultFilter')
      expect(resultFilter).to.be.an('object')
      expect(resultFilter).to.have.property('type', 'NOT OR')
      expect(resultFilter).to.have.deep.property('filters', [
        { type: 'ILIKE', property: 'anyText', value: 'foo' },
        { type: 'ILIKE', property: 'anyText', value: 'bar' },
      ])
    })
    it('get() returns a filter tree resultFilter as-is', () => {
      const originalFilter = {
        type: 'OR',
        filters: [
          { type: 'ILIKE', property: 'anyText', value: 'foo' },
          { type: 'ILIKE', property: 'anyText', value: 'bar' },
        ],
      }
      const preferences = new User.Preferences({
        resultFilter: originalFilter,
      })
      const resultFilter = preferences.get('resultFilter')
      expect(resultFilter).to.deep.equal(originalFilter)
    })
  })
})
