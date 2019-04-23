const BaseLine = require('./base.line')

const options = {
  label: 'Line',
  geometryKey: 'line',
  unitKey: 'lineUnits',
  widthKey: 'lineWidth',
}
const Line = props => <BaseLine {...props} {...options} />

module.exports = Line
