const React = require('react')
const BaseLine = require('./base.line')

const options = {
  label: 'MultiPolygon',
  geometryKey: 'multipolygon',
  unitKey: 'polygonBufferUnits',
  widthKey: 'polygonBufferWidth',
}

const MultiPolygon = props => <BaseLine {...props} {...options} />

module.exports = MultiPolygon
