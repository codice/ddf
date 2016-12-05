import React from 'react'

import { connect } from 'react-redux'
import { getConfig } from '../reducer'
import { editConfig } from '../actions'

import TextField from 'material-ui/TextField'

import { RadioButton, RadioButtonGroup } from 'material-ui/RadioButton'
import MenuItem from 'material-ui/MenuItem'
import SelectField from 'material-ui/SelectField'
import AutoComplete from 'material-ui/AutoComplete'

import { green400, orange500 } from 'material-ui/styles/colors'

const mapStateToProps = (state, { id }) => getConfig(state, id)

const mapDispatchToProps = (dispatch, { id }) => ({
  onEdit: (value) => dispatch(editConfig(id, value))
})

const messageStyles = {
  SUCCESS: { color: green400 },
  WARNING: { color: orange500 }
}

const InputView = ({ value = '', label, onEdit, message = {}, ...rest }) => {
  const { type, message: text } = message

  return (
    <TextField
      fullWidth
      errorText={text}
      errorStyle={messageStyles[type]}
      value={value}
      floatingLabelText={label}
      onChange={(e) => onEdit(e.target.value)}
      {...rest} />
  )
}

const Input = connect(mapStateToProps, mapDispatchToProps)(InputView)

const Password = ({ label = 'Password', ...rest }) => (
  <Input type='password' label={label} {...rest} />
)

const Hostname = ({ label = 'Hostname', ...rest }) => (
  <Input label={label} {...rest} />
)

const InputAuto = ({ value, options = [], type = 'text', error, onEdit, label, ...rest }) => (
  <AutoComplete
    fullWidth
    openOnFocus
    dataSource={options.map((value) => ({ text: String(value), value }))}
    filter={AutoComplete.noFilter}
    type={type}
    errorText={error}
    floatingLabelText={label}
    searchText={String(value)}
    onNewRequest={({ value }) => { onEdit(value) }}
    onUpdateInput={(value) => { onEdit(type === 'number' ? Number(value) : value) }}
    {...rest} />
)

const PortView = ({ value = 0, label = 'Port', ...rest }) => (
  <InputAuto type='number' value={value} label={label} {...rest} />
)

const Port = connect(mapStateToProps, mapDispatchToProps)(PortView)

const SelectView = ({ value = '', options = [], label = 'Select', onEdit, error, ...rest }) => (
  <SelectField
    fullWidth
    errorText={error}
    value={options.indexOf(value)}
    onChange={(e, i) => onEdit(options[i])}
    floatingLabelText={label}
    {...rest}>
    {options.map((d, i) =>
      <MenuItem key={i} value={i} primaryText={d} />)}
  </SelectField>
)

const Select = connect(mapStateToProps, mapDispatchToProps)(SelectView)

const RadioSelectionView = ({value, options = [], onEdit, ...rest}) => (
  <RadioButtonGroup selectedValue={value} onChange={(e, value) => onEdit(value)} {...rest}>
    {options.map((item, i) => <RadioButton key={i} value={item.value} label={item.label} />)}
  </RadioButtonGroup>
)

const RadioSelection = connect(mapStateToProps, mapDispatchToProps)(RadioSelectionView)

export {
  Input,
  Password,
  Hostname,
  Port,
  Select,
  RadioSelection
}
