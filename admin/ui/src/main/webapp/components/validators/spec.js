import { expect } from 'chai'

import * as validators from './'

describe('validators', () => {
  describe('port', () => {
    it('should return undefined for valid ports', () => {
      [0, 1024, 3000, 8080, 65535]
        .map((n) => String(n))
        .forEach((port) => {
          expect(validators.port(port)).to.be.undefined
        })
    })
    it('should return error for invalid inputs', () => {
      ['hello', 'null', '{}', '[]'].forEach((port) => {
        expect(validators.port(port)).to.equal('not a valid port')
      })
    })
  })

  describe('hostname', () => {
    it('should return undefined for valid hostnames', () => {
      ['google.com', 'example.org'].forEach((hostname) => {
        expect(validators.hostname(hostname)).to.be.undefined
      })
    })
    it('should return error for invalid hostname', () => {
      ['@', '.'].forEach((hostname) => {
        expect(validators.hostname(hostname))
        .to.equal('value is not a fully qualified domain name')
      })
    })
  })
})
