import React from 'react'
import { expect } from 'chai'
import { shallow, mount } from 'enzyme'
import { testComponent as ExampleCoordinates } from './example-coordinates'

describe('<ExampleCoordinates />', () => {
  const props = {
    selected: 'mgrs',
    examples: { mgrs: '4Q FL 23009 12331' },
  }

  it('renders', () => {
    const wrapper = mount(<ExampleCoordinates selected="foo" />)
    expect(wrapper.find({ selected: 'foo' }).length).to.equal(1)
  })

  it('displays empty example for unknown coordinate type', () => {
    const wrapper = mount(<ExampleCoordinates selected="foo" />)
    expect(wrapper.containsMatchingElement(<span />)).to.equal(true)
  })

  it('displays the correct example', () => {
    const wrapper = mount(<ExampleCoordinates {...props} />)
    expect(
      wrapper.containsMatchingElement(<span>4Q FL 23009 12331</span>)
    ).to.equal(true)
  })
})
