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
import { fromJS } from 'immutable'

import { validate, validateStructure, validateJson } from './reducer'

import urls from './urls'

describe('validate providers', () => {
  describe('name', () => {
    it('should be a valid name', () => {
      const values = ['name', 'hello world', 'this is a name', 'hello_world-1']
      values.forEach(name => {
        const providers = fromJS([{ layer: { name } }])
        expect(validate(providers).getIn([0, 'name'])).to.equal(undefined)
      })
    })
    it('should not be a valid name', () => {
      const values = [undefined, '!name2@& !*%', '']
      values.forEach(name => {
        const providers = fromJS([{ layer: { name } }])
        expect(validate(providers).getIn([0, 'name'])).to.not.equal(undefined)
      })
    })
  })
  describe('alpha', () => {
    it('should be a valid alpha', () => {
      const values = [0, 0.25, 0.5, 0.75, 1]
      values.forEach(alpha => {
        const providers = fromJS([{ layer: { alpha } }])
        expect(validate(providers).getIn([0, 'alpha'])).to.equal(undefined)
      })
    })
    it('should not be a valid alpha', () => {
      const values = [-1, {}, [], '', 1.1, true, false]
      values.forEach(alpha => {
        const providers = fromJS([{ layer: { alpha } }])
        expect(validate(providers).getIn([0, 'alpha'])).to.not.equal(undefined)
      })
    })
  })
  describe('show', () => {
    it('should be a valid show flag', () => {
      const values = [true, false]
      values.forEach(show => {
        const providers = fromJS([{ layer: { show, order: 0 }, buffer: '[]' }])
        expect(validate(providers).getIn([0, 'buffer'])).to.equal(undefined)
      })
    })
    it('should not be a valid show flag', () => {
      const values = [undefined]
      values.forEach(show => {
        const providers = fromJS([{ layer: { show, order: 0 } }])
        expect(validate(providers).getIn([0, 'buffer'])).to.not.equal(undefined)
      })
    })
  })
  describe('type', () => {
    it('should be a valid type', () => {
      const values = ['OSM', 'WMS', 'GE']
      values.forEach(type => {
        const providers = fromJS([{ layer: { type } }])
        expect(validate(providers).getIn([0, 'type'])).to.equal(undefined)
      })
    })
    it('should not be a valid type', () => {
      const values = [-1, {}, [], '', 1.1, true, false, 'A', 'B', 'C']
      values.forEach(type => {
        const providers = fromJS([{ layer: { type } }])
        expect(validate(providers).getIn([0, 'type'])).to.not.equal(undefined)
      })
    })
  })
  describe('url', () => {
    it('should be a valid url', () => {
      urls.valid.forEach(url => {
        const providers = fromJS([{ layer: { url } }])
        expect(validate(providers).getIn([0, 'url'])).to.equal(
          undefined,
          `${url} should be valid`
        )
      })
    })
    it('should not be a valid url', () => {
      const values = [-1, {}, [], '', 1.1, true, false, 'A', 'B', 'C']
      values.concat(urls.invalid).forEach(url => {
        const providers = fromJS([{ layer: { url } }])
        expect(validate(providers).getIn([0, 'url'])).to.not.equal(
          undefined,
          `${url} should be invalid`
        )
      })
    })
  })
  describe('json', () => {
    it('should be valid json', () => {
      const values = [0, true, {}, []].map(JSON.stringify)

      values.forEach(value => {
        expect(validateJson(value)).to.equal(
          undefined,
          `${value} should be valid json`
        )
      })
    })
    it('should not be valid json', () => {
      const values = ['asdf', '[1, 2, 3,]']

      values.forEach(value => {
        expect(validateJson(value)).to.not.equal(
          undefined,
          `${value} should not be valid json`
        )
      })
    })
  })
  describe('providers', () => {
    it('should be a valid provider', () => {
      const values = [[{}, {}, {}], [{ url: 'hello, world' }]]
      values.forEach(value => {
        expect(validateStructure(value)).to.equal(
          undefined,
          `${value} should be a valid provider`
        )
      })
    })
    it('should not be a valid provider', () => {
      const values = [
        null,
        0,
        true,
        false,
        {},
        [1, 2, 3],
        [1, {}],
        'hello, world',
      ]
      values.forEach(value => {
        expect(validateStructure(value)).to.not.equal(
          undefined,
          `${value} should not be a valid provider`
        )
      })
    })
  })
  describe('order', () => {
    it('should be a valid order', () => {
      const values = [0, 1, 2, 3, 4]
      const providers = fromJS(values.map(order => ({ layer: { order } })))
      values.forEach(order => {
        expect(validate(providers).getIn([order, 'order'])).to.equal(undefined)
      })
    })
    it('should be out of bounds order', () => {
      const values = [0, 1, 2, 4]
      const providers = fromJS(values.map(order => ({ layer: { order } })))
      values.slice(0, values.length - 2).forEach(order => {
        expect(validate(providers).getIn([0, 'order'])).to.equal(undefined)
      })
      expect(
        validate(providers).getIn([values.length - 1, 'order'])
      ).to.not.equal(undefined)
    })
    it('should not be a valid order', () => {
      const values = [-1, {}, [], 1.1, true, false, 2]
      values.forEach(order => {
        const providers = fromJS([{ layer: { order } }])
        expect(validate(providers).getIn([0, 'order'])).to.not.equal(undefined)
      })
    })
  })
  describe('withCredentials', () => {
    it('should be a valid trusted value', () => {
      const values = [true, false]
      values.forEach(withCredentials => {
        const providers = fromJS([{ layer: { withCredentials } }])
        expect(validate(providers).getIn([0, 'withCredentials'])).to.equal(
          undefined
        )
      })
    })
    it('should not be a valid trusted value', () => {
      const values = ['hello', undefined, null]
      values.forEach(withCredentials => {
        const providers = fromJS([{ layer: { withCredentials } }])
        expect(validate(providers).getIn([0, 'withCredentials'])).to.not.equal(
          undefined
        )
      })
    })
  })
})
