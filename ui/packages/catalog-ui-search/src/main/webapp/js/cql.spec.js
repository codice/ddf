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
import cql from './cql'

describe('tokenize', () => {
  it('test relative function parsing', () => {
    const cqlString = `(("created" = 'RELATIVE(P0DT0H5M)') OR ("modified" = 'RELATIVE(P0DT0H5M)') OR ("effective" = 'RELATIVE(P0DT0H5M)') OR ("metacard.created" = 'RELATIVE(P0DT0H5M)') OR ("metacard.modified" = 'RELATIVE(P0DT0H5M)'))`
    const result = cql.simplify(cql.read(cqlString))
    const filters = result.filters

    expect(filters).to.be.an('array')
    expect(
      filters,
      'Result does not have the proper number of filters'
    ).to.have.lengthOf(5)

    filters.forEach(e => {
      switch (e.property) {
        case 'metacard.modified':
        case 'metacard.created':
        case 'modified':
        case 'created':
        case 'effective':
          expect(e.value, 'Unexpected filter value.').to.equal(
            'RELATIVE(P0DT0H5M)'
          )
          expect(e.type, 'Unexpected filter operator.').to.equal('=')
          break
        default:
          expect.fail(0, 1, 'Unexpected filters present')
      }
    })
  }),
    describe('filter functions', () => {
      const cqlString = `proximity('anyText',3,'cat dog')`
      const cqlFilter = {
        filterFunctionName: 'proximity',
        type: 'FILTER_FUNCTION',
        params: ['anyText', 3, 'cat dog'],
      }

      it('parses a filter function', () => {
        const filter = cql.simplify(cql.read(cqlString))
        expect(filter).to.deep.include(cqlFilter)
      })

      it('throws when parsing an unknown filter function', () => {
        const cqlString = `abcdefg('anyText', 3, 'cat dog')`

        let doParse = function() {
          cql.read(cqlString)
        }
        expect(doParse).to.throw('Unsupported filter function: abcdefg')
      })

      it('parses a filter function as part of a comparison', () => {
        const cqlString = `(proximity('anyText',3,'cat dog') = true)`
        const cqlOuterFilter = {
          property: cqlFilter,
          type: '=',
          value: true,
        }
        const filter = cql.simplify(cql.read(cqlString))
        expect(filter).to.deep.include(cqlOuterFilter)
      })

      it('parses nested filter functions', () => {
        const nestedCqlString = `proximity('anyText',5,${cqlString})`
        const filter = cql.simplify(cql.read(nestedCqlString))
        const cqlOuterFilter = {
          filterFunctionName: 'proximity',
          type: 'FILTER_FUNCTION',
          params: ['anyText', 5, cqlFilter],
        }
        expect(filter).to.deep.include(cqlOuterFilter)
      })

      it('parses a function with no parameters', () => {
        const cqlString = `(proximity('anyText', pi(),'cat dog'))`

        const filter = cql.simplify(cql.read(cqlString))
        expect(filter).to.deep.include({
          filterFunctionName: 'proximity',
          type: 'FILTER_FUNCTION',
          params: [
            'anyText',
            {
              filterFunctionName: 'pi',
              type: 'FILTER_FUNCTION',
              params: [],
            },
            'cat dog',
          ],
        })
      })

      it('serializes a filter function', () => {
        expect(cql.write(cqlFilter)).to.equal(cqlString)
      })
    })

  describe('CQL and UserQL translation functions', () => {
    it('parses a CQL query into a UserQL String', () => {
      const result = cql.read(
        "anyText ILIKE 'this % is \\% a \\_ test _ \\* \\?'"
      )
      expect(result.value).equals('this * is % a _ test ? \\* \\?')
    })

    it('parses a UserQL string into a CQL query', () => {
      const filter = {
        property: 'anyText',
        type: 'ILIKE',
        value: 'this * is % a _ test ? \\* \\?',
      }
      const result = cql.write(filter)
      expect(result).equals(
        '"anyText" ILIKE \'this % is \\% a \\_ test _ \\* \\?\''
      )
    })

    it('parses multiple CQL characters in a row into UserQL', () => {
      const result = cql.read("anyText ILIKE '%%\\%\\%\\_\\___\\*\\*\\?\\?'")
      expect(result.value).equals('**%%__??\\*\\*\\?\\?')
    })

    it('parses multiple UserQL characters in a row into CQL', () => {
      const filter = {
        property: 'anyText',
        type: 'ILIKE',
        value: '**%%__??\\*\\*\\?\\?',
      }
      const result = cql.write(filter)
      expect(result).equals('"anyText" ILIKE \'%%\\%\\%\\_\\___\\*\\*\\?\\?\'')
    })

    it('parses single quote property name', () => {
      const result = cql.read("'ext.test-attribute' = 'this is a test'")
      expect(result.property).equals('ext.test-attribute')
    })
  })
})
