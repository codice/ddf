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
import { expect } from 'chai'
import { Map, fromJS } from 'immutable'
import { buffer, config, validate, convertLayout } from './reducer'

import standardLayout from './test-data/layout-standard'
import reactLayout from './test-data/layout-react'
import defaultConfig from './test-data/config'

describe('buffer', () => {
  it('should be a valid JSON config buffer', () => {
    const state = Map({ buffer: JSON.stringify(standardLayout) })
    expect(validate(state)).to.equal(undefined)
  })
  it('should not be a valid JSON config buffer', () => {
    const state = Map({ buffer: '[invalid: {json config: "string"}]' })
    expect(validate(state)).to.not.equal(undefined)
  })
  it('should return the initial state', () => {
    expect(buffer(undefined, {})).to.equal(Map())
  })
  it('should handle SET_BUFFER', () => {
    const state = Map({ buffer: JSON.stringify(standardLayout, null, 2) })
    expect(
      buffer(undefined, {
        type: 'default-layout/SET_BUFFER',
        value: JSON.stringify(standardLayout),
      }).get('buffer')
    ).to.equal(state.get('buffer'))
  })
  it('should handle RESET', () => {
    const configPath = ['value', 'configurations', 0, 'properties']
    const conf = fromJS(defaultConfig).getIn(configPath)
    const defaultLayout = JSON.parse(conf.get('defaultLayout'))
    expect(
      buffer(undefined, {
        type: 'default-layout/RESET',
        value: conf,
      }).get('buffer')
    ).to.equal(JSON.stringify(defaultLayout, null, 2))
  })
})

describe('config', () => {
  it('should be correctly converted to a react friendly format', () => {
    const comPath = [0, 'content', 0, 'content', 0, 'content', 0]
    const layout = fromJS(convertLayout(JSON.stringify(standardLayout), true))
    const component = layout.getIn(comPath)
    expect(component.get('componentName')).to.equal('lm-react-component')
  })
  it('should be correctly converted to a normal format from react', () => {
    const comPath = [0, 'content', 0, 'content', 0, 'content', 0]
    const layout = fromJS(convertLayout(JSON.stringify(reactLayout), false))
    const component = layout.getIn(comPath)
    expect(component.get('componentName')).to.not.equal('lm-react-component')
    expect(component.get('isClosable')).to.equal(undefined)
    expect(component.get('reorderEnabled')).to.equal(undefined)
  })
  it('should return the initial state', () => {
    expect(config(undefined, {})).to.equal(Map())
  })
  it('should handle SET_CONFIG', () => {
    const configPath = ['value', 'configurations', 0, 'properties']
    const conf = fromJS(defaultConfig).getIn(configPath)
    expect(
      config(undefined, {
        type: 'default-layout/SET_CONFIG',
        value: conf,
      }).get('defaultLayout')
    ).to.equal(conf.get('defaultLayout'))
  })
})
