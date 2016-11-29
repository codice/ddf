import React from 'react'

import MenuItem from 'material-ui/MenuItem'
import SelectField from 'material-ui/SelectField'
import TextField from 'material-ui/TextField'
import RaisedButton from 'material-ui/RaisedButton'
import IconButton from 'material-ui/IconButton'
import { List } from 'material-ui/List'
import { RadioButton, RadioButtonGroup } from 'material-ui/RadioButton'

import AutoComplete from 'material-ui/AutoComplete'

import { Card, CardActions, CardHeader, CardText } from 'material-ui/Card'
import Flexbox from 'flexbox-react'

import { submit, edit } from '../actions'
import { connect } from 'react-redux'

import { isSubmitting } from '../reducer'

import CheckIcon from 'material-ui/svg-icons/action/check-circle'
import InfoIcon from 'material-ui/svg-icons/action/info'
import CloseIcon from 'material-ui/svg-icons/navigation/cancel'

import { green500, red500, yellow500 } from 'material-ui/styles/colors'

var Component

const PortInput = ({ disabled, path, value, error, defaults = [], onEdit, description }) => (
  <div>
    <AutoComplete
      style={{display: 'inline-block'}}
      dataSource={defaults.map((value) => ({ text: String(value), value: value }))}
      openOnFocus
      filter={AutoComplete.noFilter}
      type='number'
      errorText={error}
      disabled={disabled}
      floatingLabelText='Port'
      searchText={String(value || defaults[0])}
      onNewRequest={({ value }) => { onEdit(path, value) }}
      onUpdateInput={(value) => { onEdit(path, Number(value)) }} />
    <DescriptionIcon description={description} />
  </div>
)

const StringEnumInput = ({ disabled, path, value, label, error, defaults = [], onEdit, description }) => (
  <div>
    <SelectField
      style={{display: 'inline-block'}}
      value={value || defaults[0] || ''}
      disabled={disabled}
      errorText={error}
      floatingLabelText={label}
      // menuStyle={{transform:"translateY(20px)"}}
      onChange={(e, i, v) => onEdit(path, v)}>
      {defaults.map((d, i) => <MenuItem key={i} value={d} primaryText={d} />)}
    </SelectField>
    <DescriptionIcon description={description} />
  </div>
)

const HostNameInput = ({ disabled, path, value, error, defaults, onEdit, description }) => (
  <div>
    <TextField
      style={{display: 'inline-block'}}
      disabled={disabled}
      errorText={error}
      value={value || defaults[0] || ''}
      floatingLabelText='Hostname'
      onChange={(e) => onEdit(path, e.target.value)} />
    <DescriptionIcon description={description} />
  </div>
)

const PasswordInput = ({ disabled, path, label, error, defaults = [], value, onEdit, description }) => (
  <div>
    <TextField
      style={{display: 'inline-block'}}
      disabled={disabled}
      errorText={error}
      value={value || defaults[0] || ''}
      floatingLabelText={label}
      type='password'
      onChange={(e) => onEdit(path, e.target.value)} />
    <DescriptionIcon description={description} />
  </div>
)

const StringInput = ({ disabled, path, label, error, defaults = [], value, onEdit, description }) => (
  <div>
    <TextField
      style={{display: 'inline-block'}}
      disabled={disabled}
      errorText={error}
      value={value || defaults[0] || ''}
      floatingLabelText={label}
      onChange={(e) => onEdit(path, e.target.value)} />
    <DescriptionIcon description={description} />
  </div>
)

const LdapQueryInput = ({ disabled, path, label, error, defaults = [], value, onEdit, description }) => (
  <div>
    <TextField
      style={{display: 'inline-block'}}
      disabled={disabled}
      errorText={error}
      value={value || defaults[0] || ''}
      floatingLabelText={label}
      onChange={(e) => onEdit(path, e.target.value)} />
    <DescriptionIcon description={description} />
  </div>
)

const HotGray = 'rgba(0, 0, 0, 0.541176)'
const PrettyGoodGray = 'rgba(0, 0, 0, 0.70)'

const Info = ({id, label, value}) => (
  <div>
    <p id={id} style={{ fontSize: '18px', position: 'relative', textAlign: 'center', height: '100%', color: PrettyGoodGray }}>{label}</p>
    <p id={id} style={{ fontSize: '14px', position: 'relative', height: '100%', textAlign: 'center', color: HotGray }}>{value}</p>
  </div>
)

const Panel = ({ disabled, id, path = [], label, description, children = [] }) => (
  <div>
    <CardHeader style={{ textAlign: 'center', padding: '0px 0px 0px 90px' }} title={label} subtitle={description} subtitleStyle={{marginTop: 7}} />
    <CardActions>
      <Flexbox justifyContent='center'>
        <div>{children.map((c, i) =>
          <Component disabled={disabled} key={i} {...c} path={[ ...path, 'children', i ]} />)}</div>
      </Flexbox>
    </CardActions>
  </div>
)

const DescriptionIcon = ({ description }) => {
  if (description) {
    return (<IconButton style={{display: 'inline-block'}} tooltip={description} touch tooltipPosition='top-center'><InfoIcon /></IconButton>)
  } else {
    return null
  }
}

const ContextPanel = ({ disabled, id, path = [], label, description, children = [] }) => (
  <div>
    <Card>
      <CardHeader style={{ textAlign: 'center', padding: '0px 0px 0px 90px' }} title={label} subtitle={description} titleStyle={{marginTop: 7}} subtitleStyle={{marginTop: 7}} />
      <CardActions>
        <Flexbox flexDirection='column' justifyContent='center'>{children.map((c, i) =>
          <Component disabled={disabled} key={i} {...c} path={[ ...path, 'children', i ]} />)}
        </Flexbox>
      </CardActions>
    </Card>
  </div>
)

const RadioButtons = ({ id, disabled, path, value, options = {}, onEdit, defaultOption = {} }) => (
  <div style={{display: 'inline-block'}}>
    <RadioButtonGroup name={id || 'RadioButtonGroup'} onChange={(e, value) => onEdit(path, JSON.parse(value))}>
      {Object.keys(options).map((itemKey) =>
        <RadioButton disabled={disabled} style={{whiteSpace: 'nowrap', padding: '3px', fontSize: '16px'}} id={itemKey} key={itemKey} value={JSON.stringify(options[itemKey])} label={itemKey} />)}
    </RadioButtonGroup>
  </div>
)

const StatusPage = ({ succeeded }) => {
  if (succeeded) {
    return (
      <CheckIcon style={{width: '400px', height: '400px'}} color={green500} />
    )
  }
  return (
    <CloseIcon style={{width: '400px', height: '400px'}} color={red500} />
  )
}

// can be refactored to use findIndex, but don't want to shim it yet
const findComponentIndex = (options, value) => {
  for (var i = 0; i < options.length; i++) {
    if (value === options[i].label) return i
  }
  return 0
}

const Selector = ({ id, path = [], value, label, description, options = [] }) => {
  const i = findComponentIndex(options, value)
  return (
    <div>
      <Component
        path={path}
        type='STRING_ENUM'
        value={value || options[0].label}
        defaults={options.map((o) => o.label)} />
      <Component path={[ ...path, 'options', i, 'component' ]}
        {...options[i].component} />
    </div>
  )
}

const ButtonAction = ({ path, label, disabled, onSubmit, ...rest }) => (
  <Flexbox justifyContent='center'>
    <RaisedButton
      style={{ margin: '10px' }}
      onClick={() => { onSubmit(rest) }}
      label={label}
      primary
      disabled={disabled} />
  </Flexbox>
)

const ErrorInfo = ({ label }) => (
  <div style={{ background: red500, color: 'white', padding: 5, borderRadius: 3, textAlign: 'center', maxWidth: 300 }}>
    {label}
  </div>
)

const TestInfo = ({ label }) => (
  <div style={{ background: yellow500, padding: 5, borderRadius: 3, textAlign: 'center' }}>
    {label}
  </div>
)

const SuccessInfo = ({ label }) => (
  <div style={{ background: green500, color: 'white', padding: 5, borderRadius: 3, textAlign: 'center' }}>
    {label}
  </div>
)

const SourceInfo = ({ id, label, value }) => (
  <div style={{ fontSize: '16px', lineHeight: '24px', width: '100%', display: 'inline-block', position: 'relative', height: '200ms' }}>
    <label for={id} style={{ position: 'absolute', lineHeight: '22px', top: '30px', transform: 'scale(0.75) translate(0px, -28px)', transformOrigin: 'left top 0px' }}>{label}</label>
    <p id={id} style={{ position: 'relative', height: '100%', margin: '28px 0px 7px', whiteSpace: 'nowrap' }}>{value}</p>
  </div>
)

const ListComponent = ({ disabled, path, id, label, description, children = [] }) => (
  <div style={{ height: '300px', width: '100%', overflow: 'auto' }}>
    <List>
      {children.map((c, i) =>
        <Component path={[ ...path, 'List-items', i, 'component' ]} disabled={disabled} key={i} {...c} />)}
    </List>
  </div>
)

const ListItemComponent = ({ key, path, disabled, id, label, value, onEdit, attributes }) => (
  <Card key={key}>
    <CardHeader
      title={attributes.name ? attributes.name : attributes.cn ? 'cn: ' + attributes.cn : attributes.sn ? 'sn: ' + attributes.sn : attributes.dc ? 'dc: ' + attributes.dc : attributes.ou ? 'ou: ' + attributes.ou : 'undefined'}
      actAsExpander
      showExpandableButton
    />
    <CardText expandable style={{background: '#f2f2f2'}}>
      {Object.keys(attributes).map((itemKey, i) =>
        <SourceInfo key={i} label={itemKey} value={attributes[itemKey]} />)}
    </CardText>
  </Card>
)

const ButtonBox = ({ disabled, id, path = [], label, description, children = [] }) => (
  <div>
    <CardActions>
      <Flexbox justifyContent='center' flexDirection='row'>
        {children.map((c, i) =>
          <Component disabled={disabled} key={i} {...c} path={[ ...path, 'children', i ]} />)}
      </Flexbox>
    </CardActions>
  </div>
)

const inputs = {
  PORT: PortInput,
  HOSTNAME: HostNameInput,
  STRING_ENUM: StringEnumInput,
  STRING: StringInput,
  PASSWORD: PasswordInput,
  INFO: Info,
  BASE_CONTAINER: Panel,
  CONTEXT_PANEL: ContextPanel,
  SELECTOR: Selector,
  BUTTON_ACTION: ButtonAction,
  RADIO_BUTTONS: RadioButtons,
  ERROR_INFO: ErrorInfo,
  STATUS_PAGE: StatusPage,
  LDAP_QUERY: LdapQueryInput,
  SOURCE_INFO: SourceInfo,
  LIST: ListComponent,
  LIST_ITEM: ListItemComponent,
  BUTTON_BOX: ButtonBox,
  TEST_INFO: TestInfo,
  TEST_SUCCESS: SuccessInfo,
  TEST_FAIL: ErrorInfo

}

const StatelessComponent = ({ type, ...args }) => {
  if (type === undefined) return null

  const found = inputs[type]
  if (found !== undefined) {
    return <div>{React.createElement(found, { ...args })}</div>
  } else {
    return <div>Unknown input type {JSON.stringify(type)}.</div>
  }
}

const mapStateToProps = (state, ownProps) => ({ disabled: ownProps.disabled || isSubmitting(state) })

Component = connect(mapStateToProps, { onEdit: edit, onSubmit: submit })(StatelessComponent)

export default Component

