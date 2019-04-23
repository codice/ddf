const BaseLine = require('./base.line')

const options = {
  label: 'Polygon',
  geometryKey: 'polygon',
  unitKey: 'polygonBufferUnits',
  widthKey: 'polygonBufferWidth',
}

const Polygon = props => <BaseLine {...props} {...options} />

module.exports = Polygon
