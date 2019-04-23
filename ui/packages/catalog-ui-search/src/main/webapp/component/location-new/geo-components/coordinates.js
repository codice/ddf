const TextField = require('../../../react-component/text-field/index.js')
const MaskedTextField = require('../inputs/masked-text-field')
const { latitudeDMSMask, longitudeDMSMask } = require('./masks')

const Coordinate = props => {
  const { placeholder, value, onChange, children, ...otherProps } = props
  return (
    <div className="coordinate">
      <TextField
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        {...otherProps}
      />
      {children}
    </div>
  )
}

const MaskedCoordinate = props => {
  const { placeholder, mask, value, onChange, children, ...otherProps } = props
  return (
    <div className="coordinate">
      <MaskedTextField
        placeholder={placeholder}
        mask={mask}
        value={value}
        onChange={onChange}
        {...otherProps}
      />
      {children}
    </div>
  )
}

const DmsLatitude = props => {
  return (
    <MaskedCoordinate
      placeholder="dd°mm'ss.s&quot;"
      mask={latitudeDMSMask}
      {...props}
    />
  )
}

const DmsLongitude = props => {
  return (
    <MaskedCoordinate
      placeholder="ddd°mm'ss.s&quot;"
      mask={longitudeDMSMask}
      {...props}
    />
  )
}

const DdLatitude = props => {
  return (
    <Coordinate
      placeholder="latitude"
      type="number"
      step="any"
      min={-90}
      max={90}
      addon="°"
      {...props}
    />
  )
}

const DdLongitude = props => {
  return (
    <Coordinate
      placeholder="longitude"
      type="number"
      step="any"
      min={-180}
      max={180}
      addon="°"
      {...props}
    />
  )
}

const UsngCoordinate = props => {
  return (
    <div className="coordinate">
      <TextField label="Grid" {...props} />
    </div>
  )
}

module.exports = {
  DmsLatitude,
  DmsLongitude,
  DdLatitude,
  DdLongitude,
  UsngCoordinate,
}
