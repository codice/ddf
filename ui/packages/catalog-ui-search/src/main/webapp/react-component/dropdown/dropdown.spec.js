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
const React = require('react')

const Enzyme = require('enzyme')
const Adapter = require('enzyme-adapter-react-16')

const { expect } = require('chai')

Enzyme.configure({ adapter: new Adapter() })

const Dropdown = require('./dropdown')

const { shallow } = Enzyme

describe('<Dropdown />', () => {
  const Mock = () => null

  it('should render closed', () => {
    const wrapper = shallow(
      <Dropdown>
        <Mock />
      </Dropdown>
    )
    expect(wrapper.find(Mock).length).to.equal(0)
  })

  it('should render label', () => {
    const wrapper = shallow(
      <Dropdown label="test">
        <Mock />
      </Dropdown>
    )
    expect(wrapper.find({ children: 'test' })).to.have.length(1)
  })
})
