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

const Keyword = require('./keyword')

const { shallow } = Enzyme

describe('<Keyword />', () => {
  it('should fetch features on select', done => {
    const coordinates = [1, 2, 3]
    const fetch = async url => {
      expect(url).to.equal('./internal/geofeature?id=0')
      return {
        async json() {
          return {
            geometry: {
              type: 'Polygon',
              coordinates: [coordinates],
            },
          }
        },
      }
    }
    const setState = ({ polygon }) => {
      expect(wrapper.state('loading')).to.equal(false)
      expect(polygon).to.equal(coordinates)
      done()
    }
    const wrapper = shallow(<Keyword fetch={fetch} setState={setState} />)
    wrapper.find('AutoComplete').prop('onChange')({
      id: '0',
      name: 'test',
    })
    expect(wrapper.state('loading')).to.equal(true)
  })
  it('should inform user on failure to fetch geofeature', done => {
    const onError = () => {
      wrapper.update()
      expect(wrapper.text()).include('Geo feature endpoint unavailable')
      done()
    }
    const fetch = async () => {
      throw new Error('unavailable')
    }
    const wrapper = shallow(<Keyword fetch={fetch} onError={onError} />)
    wrapper.find('AutoComplete').prop('onChange')({
      id: '0',
      name: 'test',
    })
  })
})
