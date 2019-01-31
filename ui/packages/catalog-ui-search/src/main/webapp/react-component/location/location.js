const React = require('react')

const CustomElements = require('../../js/CustomElements.js')

const Button = require('../button')
const Dropdown = require('../dropdown')
const Json = require('../json')
const { Menu, MenuItem } = require('../menu')
import styled from '../styles/styled-components/styled-components'

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
    Component: Keyword,
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

const DropdownPadding = styled.div`
  padding: ${props => props.theme.minimumSpacing};
`

const Component = CustomElements.registerReact('location')

const LocationInput = props => {
  const { mode, setState, cursor } = props
  const input = inputs[mode] || {}
  const { Component: Input = null } = input

  return (
    <Component>
      <Json value={props} onChange={value => setState(value)} />
      <DropdownPadding>
        <Dropdown label={input.label || 'Select Location Option'}>
          <Menu value={mode} onChange={cursor('mode')}>
            {Object.keys(inputs).map(key => (
              <MenuItem key={key} value={key}>
                {inputs[key].label}
              </MenuItem>
            ))}
          </Menu>
        </Dropdown>
      </DropdownPadding>
      <Form>
        {Input !== null ? <Input {...props} /> : null}
        {drawTypes.includes(mode) ? <DrawButton onDraw={props.onDraw} /> : null}
      </Form>
    </Component>
  )
}

const ddValidators = {
  lat: value => value <= 90 && value >= -90,
  lon: value => value <= 180 && value >= -180,
  north: value => value <= 90 && value >= -90,
  west: value => value <= 180 && value >= -180,
  south: value => value <= 90 && value >= -90,
  east: value => value <= 180 && value >= -180,
}

module.exports = ({ state, setState, options }) => (
  <LocationInput
    {...state}
    onDraw={options.onDraw}
    setState={setState}
    cursor={key => value => {
      const validateCoords = ddValidators[key]
      if (typeof validateCoords === 'function' && !validateCoords(value)) {
        return
      }
      setState(key, value)
    }}
  />
)
