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
import { shallow, mount } from 'enzyme'

import MarionetteRegionContainer from './index'

const Marionette = require('marionette')

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
