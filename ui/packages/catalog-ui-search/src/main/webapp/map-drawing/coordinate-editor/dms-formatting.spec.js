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
import * as formatting from './dms-formatting'

describe('dms-formatting', () => {
  const positiveDMSList = [
    {
      degree: 45,
      minute: 30,
      second: 0.01,
    },
    {
      degree: 78,
      minute: 7,
      second: 0,
    },
    {
      degree: 0,
      minute: 0,
      second: 0.1,
    },
    {
      degree: 1,
      minute: 39,
      second: 37.86,
    },
  ]
  const negativeDMSList = [
    {
      degree: -45,
      minute: -30,
      second: -0.01,
    },
    {
      degree: -45,
      minute: 30,
      second: -0.01,
    },
    {
      degree: 45,
      minute: -30,
      second: -0.01,
    },
    {
      degree: -45,
      minute: -30,
      second: 0.01,
    },
    {
      degree: -45,
      minute: 30,
      second: 0.01,
    },
    {
      degree: 45,
      minute: -30,
      second: 0.01,
    },
    {
      degree: 45,
      minute: 30,
      second: -0.01,
    },
    {
      degree: -9,
      minute: 56,
      second: 49.95,
    },
    {
      degree: -38,
      minute: 52,
      second: 56.93,
    },
    {
      degree: -83,
      minute: 22,
      second: 15.37,
    },
  ]
  const consistentNegativeDMSList = [
    {
      degree: -45,
      minute: 30,
      second: 0.01,
    },
    {
      degree: -9,
      minute: 56,
      second: 49.95,
    },
    {
      degree: -38,
      minute: 52,
      second: 56.93,
    },
    {
      degree: -83,
      minute: 22,
      second: 15.37,
    },
  ]
  const positiveDecimalList = [45.5, 78.11667, 0.00002777778, 1.660517]
  const negativeDecimalList = [-45.5, -9.947208, -38.88248, -83.37094]
  describe('dmsSign', () => {
    it('negative', () => {
      negativeDMSList.forEach(dms => {
        const sign = formatting.dmsSign(dms)
        expect(sign).to.equal(-1)
      })
    })
    it('positive', () => {
      positiveDMSList.forEach(dms => {
        const sign = formatting.dmsSign(dms)
        expect(sign).to.equal(1)
      })
      const sign = formatting.dmsSign({
        degree: 0,
        minute: 0,
        second: 0,
      })
      expect(sign).to.equal(1)
    })
  })
  describe('setSign', () => {
    it('set negative', () => {
      positiveDMSList.forEach(dms => {
        const negative = formatting.dmsSetSign(dms, -1)
        const sign = formatting.dmsSign(negative)
        expect(sign).to.equal(-1)
      })
      negativeDMSList.forEach(dms => {
        const negative = formatting.dmsSetSign(dms, -1)
        const sign = formatting.dmsSign(negative)
        expect(sign).to.equal(-1)
      })
    })
    it('set positive', () => {
      positiveDMSList.forEach(dms => {
        const positive = formatting.dmsSetSign(dms, 1)
        const sign = formatting.dmsSign(positive)
        expect(sign).to.equal(1)
      })
      negativeDMSList.forEach(dms => {
        const positive = formatting.dmsSetSign(dms, 1)
        const sign = formatting.dmsSign(positive)
        expect(sign).to.equal(1)
      })
    })
  })
  describe('decimalToDMS', () => {
    it('negative', () => {
      negativeDecimalList.forEach((decimal, i) => {
        const expected = consistentNegativeDMSList[i]
        const result = formatting.decimalToDMS(decimal)
        expect(result.degree).to.be.equal(expected.degree)
        expect(result.minute).to.be.equal(expected.minute)
        expect(result.second).to.be.closeTo(expected.second, 0.02)
      })
    })
    it('positive', () => {
      positiveDecimalList.forEach((decimal, i) => {
        const expected = positiveDMSList[i]
        const result = formatting.decimalToDMS(decimal)
        expect(result.degree).to.be.equal(expected.degree)
        expect(result.minute).to.be.equal(expected.minute)
        expect(result.second).to.be.closeTo(expected.second, 0.02)
      })
    })
  })
  describe('dmsToDecimal', () => {
    it('negative', () => {
      consistentNegativeDMSList.forEach((dms, i) => {
        const expected = negativeDecimalList[i]
        const result = formatting.dmsToDecimal(dms)
        expect(result).to.be.closeTo(expected, 0.0001)
      })
    })
    it('positive', () => {
      positiveDMSList.forEach((dms, i) => {
        const expected = positiveDecimalList[i]
        const result = formatting.dmsToDecimal(dms)
        expect(result).to.be.closeTo(expected, 0.0001)
      })
    })
  })
  describe('dmsValueToString', () => {
    it('North', () => {
      const expected = '30\xB0 15\' 11.3" N'
      const result = formatting.dmsValueToString(
        {
          degree: 30,
          minute: 15,
          second: 11.25,
        },
        false
      )
      expect(result).to.equal(expected)
    })
    it('East', () => {
      const expected = '30\xB0 15\' 11.3" E'
      const result = formatting.dmsValueToString(
        {
          degree: 30,
          minute: 15,
          second: 11.25,
        },
        true
      )
      expect(result).to.equal(expected)
    })
    it('South', () => {
      const expected = '30\xB0 15\' 11.3" S'
      const result = formatting.dmsValueToString(
        {
          degree: -30,
          minute: 15,
          second: 11.25,
        },
        false
      )
      expect(result).to.equal(expected)
    })
    it('West', () => {
      const expected = '30\xB0 15\' 11.3" W'
      const result = formatting.dmsValueToString(
        {
          degree: -30,
          minute: 15,
          second: 11.25,
        },
        true
      )
      expect(result).to.equal(expected)
    })
    it('no rounding', () => {
      const expected = '87\xB0 45\' 53.0" N'
      const result = formatting.dmsValueToString(
        {
          degree: 87,
          minute: 45,
          second: 53,
        },
        false
      )
      expect(result).to.equal(expected)
    })
  })
})
