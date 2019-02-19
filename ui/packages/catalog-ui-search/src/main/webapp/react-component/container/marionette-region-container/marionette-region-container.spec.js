import React from 'react'
import { expect } from 'chai'
import { shallow, mount } from 'enzyme'

import MarionetteRegionContainer from './index'

const Marionette = require('backbone.marionette')

describe('<MarionetteRegionContainer />', () => {
  it('renders a single div', () => {
    const wrapper = mount(<MarionetteRegionContainer />)
    expect(wrapper.find('div')).to.have.length(1)
  })

  it('renders a marionette view', done => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    const TestView = Marionette.ItemView.extend({
      template: '<h1></h1>',
      onRender() {
        done()
        div.remove()
      },
    })
    const wrapper = mount(<MarionetteRegionContainer view={TestView} />, {
      attachTo: div,
    })
  })

  it('handles switching views quickly', done => {
    const div = document.createElement('div')
    document.body.appendChild(div)
    const TestView = Marionette.ItemView.extend({
      template: '<h1></h1>',
      onRender() {
        done()
        div.remove()
      },
    })
    const TestView2 = Marionette.ItemView.extend({
      template: '<h1></h1>',
      onRender() {
        done()
        div.remove()
      },
    })
    const wrapper = mount(<MarionetteRegionContainer view={TestView} />, {
      attachTo: div,
    })
    wrapper.setProps({ view: TestView2 })
  })
})
