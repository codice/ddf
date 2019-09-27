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
import { deserialize, serialize } from './betweenDateHelper'
import moment from 'moment-timezone'

describe('deserialize', () => {
  it('handles empty string', () => {
    expect(deserialize('')).to.deep.equal({ from: '', to: '' })
  })
  it('divides from and to', () => {
    expect(deserialize('from/to')).to.deep.equal({ from: 'from', to: 'to' })
  })
})

describe('serialize', () => {
  it('serializes properly', () => {
    // May 4, 2019, 00:00:00 GMT -0600 to May 5, 2019, 00:00:00 GMT -0600
    expect(
      serialize({ from: moment(1556949600000), to: moment(1557036000000) })
    ).to.equal('2019-05-04T06:00:00.000Z/2019-05-05T06:00:00.000Z')
  })

  it('fixes order of dates', () => {
    expect(
      serialize({ from: moment(1557036000000), to: moment(1556949600000) })
    ).to.equal('2019-05-04T06:00:00.000Z/2019-05-05T06:00:00.000Z')
  })
})
