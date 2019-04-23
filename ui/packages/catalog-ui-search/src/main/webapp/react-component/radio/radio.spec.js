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

const { Radio, RadioItem } = require('./radio')

const { shallow, mount } = Enzyme

describe('<Radio />', () => {
  it('should render without children', () => {
    shallow(<Radio />)
  })

  it('should select the right child', () => {
    const wrapper = mount(
      <Radio value="two">
        <RadioItem value="one" />
        <RadioItem value="two" />
        <RadioItem value="three" />
      </Radio>
    )
    expect(wrapper.find({ selected: true }).prop('value')).to.equal('two')
  })

  it('should select child three', done => {
    const onChange = value => {
      expect(value).to.equal('three')
      done()
    }

    const wrapper = mount(
      <Radio onChange={onChange}>
        <RadioItem value="one" />
        <RadioItem value="two" />
        <RadioItem value="three" />
      </Radio>
    )

    wrapper.find({ value: 'three' }).prop('onClick')()
  })
})
