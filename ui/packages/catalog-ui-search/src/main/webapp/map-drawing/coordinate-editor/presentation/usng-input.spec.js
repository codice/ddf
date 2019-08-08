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
import React from 'react'
import { expect } from 'chai'
import { shallow } from 'enzyme'
import USNGInput from './usng-input'

describe('<USNGInput />', () => {
  describe('renders', () => {
    it('default', () => {
      const wrapper = shallow(<USNGInput value={0} onChange={() => {}} />)
      expect(wrapper.exists()).to.equal(true)
    })
  })
  describe('formatting', () => {
    it('default', () => {
      const wrapper = shallow(<USNGInput value="18SUJ22850705" />)
      expect(wrapper.find('TextInput').prop('value')).to.equal('18SUJ22850705')
    })
  })
  describe('onChange', () => {
    it('default', done => {
      const wrapper = shallow(
        <USNGInput
          value=""
          onChange={value => {
            expect(value).to.equal('18SUJ22850705')
            done()
          }}
        />
      )
      wrapper.find('TextInput').prop('onChange')({
        currentTarget: {
          value: '18SUJ22850705',
        },
      })
      wrapper.find('TextInput').prop('onBlur')()
    })
  })
})
