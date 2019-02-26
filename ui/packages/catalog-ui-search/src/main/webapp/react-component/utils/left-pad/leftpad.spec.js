/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

import { expect } from 'chai'
import leftPad from './left-pad'

describe('left-pad', () => {
  it('positive latitude, no decimal', () => {
    expect(leftPad(88, 3)).to.equal(' 88')
  })
  it('positive longitude, no decimal', () => {
    expect(leftPad(179, 4)).to.equal(' 179')
  })
  it('negative latitude, no decimal', () => {
    expect(leftPad(-88, 3)).to.equal('-88')
  })
  it('negative longitude, no decimal', () => {
    expect(leftPad(-179, 4)).to.equal('-179')
  })
  it('positive latitude, with decimal', () => {
    expect(leftPad(88.209234, 3)).to.equal(' 88')
  })
  it('positive longitude, with decimal', () => {
    expect(leftPad(179.209234, 4)).to.equal(' 179')
  })
  it('negative latitude, with decimal', () => {
    expect(leftPad(-88.209234, 3)).to.equal('-88')
  })
  it('negative longitude, with decimal', () => {
    expect(leftPad(-179.209234, 4)).to.equal('-179')
  })
})
