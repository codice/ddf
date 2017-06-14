import { expect } from 'chai'
import { fromJS } from 'immutable'

import { validate } from './reducer'

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
})
