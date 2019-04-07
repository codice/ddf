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
import sinon from 'sinon'
import React from 'react'

describe('Test <MapSettings> component', () => {
  let testTarget = undefined

  beforeEach(function() {
    this.xhr = sinon.useFakeXMLHttpRequest()
    this.requests = []
    this.xhr.onCreate = function(xhr) {
      this.requests.push(xhr)
    }.bind(this)
    this.testTarget = require('./map-settings')
  })

  afterEach(function() {
    this.xhr.restore()
    this.testTarget = undefined
  })

  it('Test <MapSettings> default rendering', () => {
    expect(this.testTarget.selected).to.equal('mgrs')
  })
})
