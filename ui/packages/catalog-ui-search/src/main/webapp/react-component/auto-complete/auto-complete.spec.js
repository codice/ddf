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

const AutoComplete = require('./auto-complete')

const { shallow } = Enzyme

describe('<AutoComplete />', () => {
  it('should change value on select', done => {
    const onChange = value => {
      expect(value).to.equal('test')
      done()
    }
    const wrapper = shallow(<AutoComplete onChange={onChange} />)
    wrapper.find('Menu').prop('onChange')('test')
  })

  it('should display all suggestions', () => {
    const wrapper = shallow(<AutoComplete />)
    wrapper.setState({
      suggestions: [
        { id: 'one', name: 'one' },
        { id: 'two', name: 'two' },
        { id: 'three', name: 'three' },
      ],
    })
    expect(wrapper.find('MenuItem').length).to.equal(3)
  })

  it('should fetch suggestions on user input', done => {
    const suggester = async input => {
      expect(input).to.equal('test')
      expect(wrapper.state('loading')).to.equal(true)
      done()
    }
    const wrapper = shallow(<AutoComplete suggester={suggester} debounce={0} />)
    expect(wrapper.state('loading')).to.equal(false)
    wrapper.find('TextField').prop('onChange')('test')
  })

  it('should inform user when endpoint is unavailable', done => {
    const onError = e => {
      const { loading, error } = wrapper.state()
      expect(loading).to.equal(false)
      expect(error).to.equal('Endpoint unavailable')
      done()
    }
    const suggester = async input => {
      throw new Error('unavailable')
    }
    const wrapper = shallow(
      <AutoComplete suggester={suggester} debounce={0} onError={onError} />
    )
    wrapper.find('TextField').prop('onChange')('test')
  })
})
