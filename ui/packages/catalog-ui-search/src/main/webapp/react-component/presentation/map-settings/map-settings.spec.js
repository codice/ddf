import React from 'react'
import { expect } from 'chai'
import { shallow, mount } from 'enzyme'
import {ExampleCoordinates} from './map-settings'


describe('<MapSettings />', () => {
  const mapSettings = {
    selected: 'mgrs',
    example: '4Q FL 23009 12331',
  }

  it('displays the correct example', () => {
    
    const wrapper = mount(<ExampleCoordinates {...mapSettings} />)
    
    expect(
      wrapper.containsMatchingElement(<span>4Q FL 23009 12331</span>)
    ).to.equal(true)
  })
})
