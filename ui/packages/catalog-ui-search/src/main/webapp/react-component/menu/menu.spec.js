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

const { Menu, MenuItem } = require('./menu')

const { shallow } = Enzyme

describe('<Menu />', () => {
  it('should not throw an error with no children', () => {
    const wrapper = shallow(<Menu />)
  })

  it('should render the correct number of <MenuItem />s', () => {
    const wrapper = shallow(
      <Menu value="two">
        <MenuItem value="one" />
        <MenuItem value="two" />
        <MenuItem value="three" />
      </Menu>
    )
    expect(wrapper.find('MenuItem').length).to.equal(3)
  })

  it('should have the correct <MenuItem /> selected', () => {
    const wrapper = shallow(
      <Menu value="two">
        <MenuItem value="one" />
        <MenuItem value="two" />
        <MenuItem value="three" />
      </Menu>
    )
    expect(wrapper.find({ selected: true }).prop('value')).to.equal('two')
  })

  it('should select the right <MenuItem /> on click', done => {
    const onChange = value => {
      expect(value).to.equal('one')
      done()
    }
    const wrapper = shallow(
      <Menu value="two" onChange={onChange}>
        <MenuItem value="one" />
        <MenuItem value="two" />
        <MenuItem value="three" />
      </Menu>
    )
    wrapper.find({ value: 'one' }).prop('onClick')()
  })

  const table = [
    {
      events: [],
      state: 'two',
    },
    {
      events: ['ArrowDown'],
      state: 'three',
    },
    {
      events: ['ArrowUp'],
      state: 'one',
    },
    {
      events: ['ArrowDown', 'ArrowDown'],
      state: 'one',
    },
    {
      events: ['ArrowDown', 'ArrowDown', 'ArrowDown'],
      state: 'two',
    },
  ]

  const mockEvent = code => ({ code, preventDefault: () => {} })

  table.forEach(({ events, state }) => {
    it(`should equal value='${state}' after ${JSON.stringify(
      events
    )}`, done => {
      const onChange = value => {
        expect(value).to.equal(state)
        done()
      }

      const wrapper = shallow(
        <Menu value="two" onChange={onChange}>
          <MenuItem value="one" />
          <MenuItem value="two" />
          <MenuItem value="three" />
        </Menu>
      )

      const listener = wrapper.find('DocumentListener').prop('listener')

      events.forEach(event => {
        listener(mockEvent(event))
      })

      listener(mockEvent('Enter'))
    })
  })

  it('should activate <MenuItem /> on hover', () => {
    const wrapper = shallow(
      <Menu value="two">
        <MenuItem value="one" />
        <MenuItem value="two" />
        <MenuItem value="three" />
      </Menu>
    )
    expect(wrapper.state('active')).to.equal('two')
    wrapper.find({ value: 'one' }).prop('onHover')()
    expect(wrapper.state('active')).to.equal('one')
  })

  it('should support multi', done => {
    const onChange = value => {
      expect(value).to.deep.equal(['one', 'two'])
      done()
    }
    const wrapper = shallow(
      <Menu multi value={['one']} onChange={onChange}>
        <MenuItem value="one" />
        <MenuItem value="two" />
        <MenuItem value="three" />
      </Menu>
    )
    const selected = wrapper.find({ value: 'one' }).prop('selected')
    expect(selected).to.equal(true)
    wrapper.find({ value: 'two' }).prop('onClick')()
  })
})
