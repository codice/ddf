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
import wrapNum from './wrap-num'

describe('wrap-num', () => {
  it('overflow +1/-1', () => {
    expect(wrapNum(181, -180, 180)).to.equal(-179)
    expect(wrapNum(-181, -180, 180)).to.equal(179)
  })
  it('overflow +/-a lot', () => {
    expect(wrapNum(64.25 + 180 * 7, -180, 180)).to.equal(-180 + 64.25)
    expect(wrapNum(-64.25 - 180 * 7, -180, 180)).to.equal(180 - 64.25)
  })
  it('no overflow mid', () => {
    expect(wrapNum(-179, -180, 180)).to.equal(-179)
    expect(wrapNum(179, -180, 180)).to.equal(179)
    expect(wrapNum(0, -180, 180)).to.equal(0)
    expect(wrapNum(5, -180, 180)).to.equal(5)
    expect(wrapNum(-15, -180, 180)).to.equal(-15)
  })
  it('max should map to min', () => {
    expect(wrapNum(180, -180, 180)).to.equal(-180)
  })
  it('min should remain min', () => {
    expect(wrapNum(-180, -180, 180)).to.equal(-180)
  })
})
