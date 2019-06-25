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
import React from 'react'
import { mount } from 'enzyme'
import {
  mock as mockJquery,
  unmock as unmockJquery,
} from '../../../test/mock-api/mock-jquery'
import {
  mock as mockProperties,
  unmock as unmockProperties,
} from '../../../test/mock-api/mock-properties'
let MapSettings

const checkDropdown = wrapper => {
  const options = wrapper.childAt(0).props().children[0].props.options
  expect(options[0].value).to.be.equals('degrees')
  expect(options[1].value).to.be.equals('decimal')
  expect(options[2].value).to.be.equals('mgrs')
  expect(options[3].value).to.be.equals('utm')
}

describe('Test <MapSettings> container component', () => {
  before(() => {
    mockJquery()
    mockProperties()
    MapSettings = require('./map-settings').testComponent
  })
  after(() => {
    unmockJquery()
    unmockProperties()
  })

  it('Test <MapSettings> no choice is selected', () => {
    const wrapper = mount(<MapSettings />)
    const selectedValue = wrapper.childAt(0).props().children[0].props.value
    expect(selectedValue).to.be.undefined
    checkDropdown(wrapper)
    wrapper.unmount()
  })
  it('Test <MapSettings> MGRS is selected', () => {
    const wrapper = mount(<MapSettings selected="mgrs" />)
    const selectedValue = wrapper.childAt(0).props().children[0].props.value
    expect(selectedValue).to.be.equal('mgrs')
    checkDropdown(wrapper)
    wrapper.unmount()
  })
})
