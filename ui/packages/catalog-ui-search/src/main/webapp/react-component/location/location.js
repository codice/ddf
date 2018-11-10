const React = require('react')

const CustomElements = require('../../js/CustomElements.js')

const Button = require('../button')
const Json = require('../json')
const { Radio, RadioItem } = require('../radio')

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

const Component = CustomElements.registerReact('location')

const LocationInput = props => {
  const { mode, setState, cursor } = props
  const input = inputs[mode] || {}
  const { Component: Input = null } = input

  return (
    <Component>
      <Json value={props} onChange={value => setState(value)} />
      <Radio value={mode} onChange={cursor('mode')}>
        {Object.keys(inputs).map(key => (
          <RadioItem key={key} value={key}>
            {inputs[key].label}
          </RadioItem>
        ))}
      </Radio>
      <Form>
        {Input !== null ? <Input {...props} /> : null}
        {drawTypes.includes(mode) ? <DrawButton onDraw={props.onDraw} /> : null}
      </Form>
    </Component>
  )
}

module.exports = ({ state, setState, options }) => (
  <LocationInput
    {...state}
    onDraw={options.onDraw}
    setState={setState}
    cursor={key => value => setState(key, value)}
  />
)
