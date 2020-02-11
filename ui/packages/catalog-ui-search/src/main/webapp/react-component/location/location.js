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

const CustomElements = require('../../js/CustomElements.js')

const Button = require('../button')
const Dropdown = require('../dropdown')
const { Menu, MenuItem } = require('../menu')
import styled from 'styled-components'

const Line = require('./line')
const Polygon = require('./polygon')
const PointRadius = require('./point-radius')
const BoundingBox = require('./bounding-box')
const Keyword = require('./keyword')
const plugin = require('plugins/location')

const inputs = plugin({
  line: {
    label: 'Line',
    Component: Line,
  },
  poly: {
    label: 'Polygon',
    Component: Polygon,
  },
  circle: {
    label: 'Point-Radius',
    Component: PointRadius,
  },
  bbox: {
    label: 'Bounding Box',
    Component: BoundingBox,
  },
  keyword: {
    label: 'Keyword',
    Component: ({ setState, keywordValue, ...props }) => {
      return (
        <Keyword
          {...props}
          value={keywordValue}
          setState={({ value, ...data }) => {
            setState({ keywordValue: value, ...data })
          }}
          setBufferState={(key, value) => setState({ [key]: value })}
        />
      )
    },
  },
})

const drawTypes = ['line', 'poly', 'circle', 'bbox']

const Form = ({ children }) => (
  <div className="form-group clearfix">{children}</div>
)

const DrawButton = ({ onDraw }) => (
  <Button className="location-draw is-primary" onClick={onDraw}>
    <span className="fa fa-globe" />
    <span>Draw</span>
  </Button>
)

const Root = styled.div`
  height: ${props => (props.isOpen ? 'auto' : props.theme.minimumButtonSize)};
`

const Component = CustomElements.registerReact('location')
const LocationInput = props => {
  const { mode, setState } = props
  const input = inputs[mode] || {}
  const { Component: Input = null } = input
  return (
    <Root isOpen={input.label !== undefined}>
      <Component>
        <Dropdown label={input.label || 'Select Location Option'}>
          <Menu value={mode} onChange={value => setState({ ['mode']: value })}>
            {Object.keys(inputs).map(key => (
              <MenuItem key={key} value={key}>
                {inputs[key].label}
              </MenuItem>
            ))}
          </Menu>
        </Dropdown>
        <Form>
          {Input !== null ? <Input {...props} /> : null}
          {drawTypes.includes(mode) ? (
            <DrawButton onDraw={props.onDraw} />
          ) : null}
        </Form>
      </Component>
    </Root>
  )
}

module.exports = ({ state, setState, options }) => (
  <LocationInput {...state} onDraw={options.onDraw} setState={setState} />
)
