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
import moment from 'moment-timezone'
import { formatDate, parseInput } from './dateHelper'

describe('formatDate', () => {
  it('formats invalid to blank', () => {
    const date = moment('')
    expect(formatDate(date)).to.equal('')
  })
  it('formats valid date', () => {
    // May 4, 2019, 12:00:00 GMT +0600
    const date = moment(new Date(1556949600000))
    expect(
      formatDate(date, 'Etc/GMT-6', 'YYYY-MM-DD[T]HH:mm:ss.SSS Z')
    ).to.equal('2019-05-04T12:00:00.000 +06:00')
  })
})

describe('parseInput', () => {
  it('returns invalid date for blank', () => {
    expect(parseInput('').isValid()).to.equal(false)
  })

  it('converts input to correct timezone ', () => {
    expect(
      parseInput(
        '2019-05-04T00:00:00',
        'Etc/GMT+6',
        'YYYY-MM-DD[T]HH:mm:ss.SSS Z'
      ).valueOf()
    ).to.equal(1556949600000)
  })

  it('converts another timezone to current timezone', () => {
    expect(
      parseInput(
        '2019-05-04T12:00:00.000 +06:00',
        'Etc/GMT+6',
        'YYYY-MM-DD[T]HH:mm:ss.SSS Z'
      ).valueOf()
    ).to.equal(1556949600000)
  })

  it('uses fallback for invalid, nonblank input', () => {
    const fallback = moment(new Date(1556949600000))
    expect(
      parseInput(
        'Lorem Ipsum',
        'Etc/GMT-6',
        'YYYY-MM-DD[T]HH:mm:ss.SSS Z',
        fallback
      ).valueOf()
    ).to.equal(fallback.valueOf())
  })
})
