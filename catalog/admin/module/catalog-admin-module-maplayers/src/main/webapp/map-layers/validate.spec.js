import { expect } from 'chai'
import { fromJS } from 'immutable'

import {
  validate,
  validateStructure,
  validateJson
} from './reducer'

import urls from './urls'

describe('validate providers', () => {
  describe('alpha', () => {
    it('should be a valid alpha', () => {
      const values = [0, 0.25, 0.5, 0.75, 1]
      values.forEach((alpha) => {
        const providers = fromJS([ { layer: { alpha } } ])
        expect(validate(providers).getIn([0, 'alpha'])).to.equal(undefined)
      })
    })
    it('should not be a valid alpha', () => {
      const values = [-1, {}, [], '', 1.1, true, false]
      values.forEach((alpha) => {
        const providers = fromJS([ { layer: { alpha } } ])
        expect(validate(providers).getIn([0, 'alpha'])).to.not.equal(undefined)
      })
    })
  })
  describe('type', () => {
    it('should be a valid type', () => {
      const values = ['OSM', 'WMS', 'GE']
      values.forEach((type) => {
        const providers = fromJS([ { layer: { type } } ])
        expect(validate(providers).getIn([0, 'type'])).to.equal(undefined)
      })
    })
    it('should not be a valid type', () => {
      const values = [-1, {}, [], '', 1.1, true, false, 'A', 'B', 'C']
      values.forEach((type) => {
        const providers = fromJS([ { layer: { type } } ])
        expect(validate(providers).getIn([0, 'type'])).to.not.equal(undefined)
      })
    })
  })
  describe('url', () => {
    it('should be a valid url', () => {
      urls.valid.forEach((url) => {
        const providers = fromJS([ { layer: { url } } ])
        expect(validate(providers).getIn([0, 'url']))
          .to.equal(undefined, `${url} should be valid`)
      })
    })
    it('should not be a valid url', () => {
      const values = [-1, {}, [], '', 1.1, true, false, 'A', 'B', 'C']
      values.concat(urls.invalid).forEach((url) => {
        const providers = fromJS([ { layer: { url } } ])
        expect(validate(providers).getIn([0, 'url']))
          .to.not.equal(undefined, `${url} should be invalid`)
      })
    })
  })
  describe('json', () => {
    it('should be valid json', () => {
      const values = [
        0,
        true,
        {},
        []
      ].map(JSON.stringify)

      values.forEach((value) => {
        expect(validateJson(value))
          .to.equal(undefined, `${value} should be valid json`)
      })
    })
    it('should not be valid json', () => {
      const values = [
        'asdf',
        '[1, 2, 3,]'
      ]

      values.forEach((value) => {
        expect(validateJson(value))
          .to.not.equal(undefined, `${value} should not be valid json`)
      })
    })
  })
  describe('providers', () => {
    it('should be a valid provider', () => {
      const values = [
        [{}, {}, {}],
        [{ url: 'hello, world' }]
      ]
      values.forEach((value) => {
        expect(validateStructure(value))
          .to.equal(undefined, `${value} should be a valid provider`)
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
        'hello, world'
      ]
      values.forEach((value) => {
        expect(validateStructure(value))
          .to.not.equal(undefined, `${value} should not be a valid provider`)
      })
    })
  })
})
