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

const TextField = require('./text-field')

const { mount } = Enzyme

describe('<TextField />', () => {
  it('<input /> should have the right value', () => {
    const wrapper = mount(<TextField value="test" />)
    expect(wrapper.find('input').prop('value')).to.equal('test')
  })

  it('should update input on change', done => {
    const onChange = value => {
      expect(value).to.equal('test')
      done()
    }
    const wrapper = mount(<TextField onChange={onChange} />)
    wrapper.find('input').prop('onChange')({ target: { value: 'test' } })
  })
})
