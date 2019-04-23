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
import SplitButton from '.'

describe('<SplitButton />', () => {
  const createButton = () => (
    <SplitButton title="Menu" onSelect={() => {}}>
      {{
        label: (
          <span>
            <b>H</b>
            ome
          </span>
        ),
        menu: (
          <ul className="menu">
            <li>
              <button>One</button>
            </li>
            <li>
              <button>Two</button>
            </li>
          </ul>
        ),
      }}
    </SplitButton>
  )
  it('renders a single div', () => {
    const wrapper = mount(createButton())
    expect(wrapper.find('div.is-split-button')).to.have.length(1)
    expect(wrapper.find('button.toggle')).to.have.length(1)
  })
  it('toggles menu', () => {
    const wrapper = mount(createButton())
    expect(wrapper.find('.menu')).to.have.length(0)
    wrapper.find('button.toggle').simulate('click')
    expect(wrapper.find('.menu')).to.have.length(1)
  })
})
