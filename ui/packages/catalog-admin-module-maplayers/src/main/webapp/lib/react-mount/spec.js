import React from 'react'

import Mount from './'

import { mount } from 'enzyme'

describe('<Mount />', () => {
  it('should fire on when mounted', (done) => {
    mount(<Mount on={() => done()} />)
  })

  it('should fire off when unmounted', (done) => {
    const div = document.createElement('div')
    const wrapper = mount(<Mount off={() => done()} />, { attachTo: div })
    wrapper.detach()
  })
})
