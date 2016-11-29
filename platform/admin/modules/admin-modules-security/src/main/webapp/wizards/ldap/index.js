import React from 'react'

import { connect } from 'react-redux'
import { getProbeValue, getStep, isSubmitting, getMessages } from '../../reducer'

import { setDefaults } from '../../actions'

import Mount from '../../components/mount'

import Paper from 'material-ui/Paper'
import RaisedButton from 'material-ui/RaisedButton'
import FlatButton from 'material-ui/FlatButton'
import CircularProgress from 'material-ui/CircularProgress'

import Flexbox from 'flexbox-react'

import { List, ListItem } from 'material-ui/List'

import * as styles from './styles.less'

import { testConfig, probe, next, back } from './actions'

import {
  Input,
  Password,
  Hostname,
  Port,
  Select
} from '../inputs'

const Title = ({ children }) => <h1 className={styles.title}>{children}</h1>
const Description = ({ children }) => <p className={styles.description}>{children}</p>

const BackView = ({ onBack, disabled }) => (
  <FlatButton disabled={disabled} secondary label='back' onClick={onBack} />
)

const Back = connect(null, { onBack: back })(BackView)

const Next = connect(null, (dispatch, { id, url }) => ({
  next: () => dispatch(testConfig(id, url))
}))(
  ({ next, disabled }) => <RaisedButton label='Next' disabled={disabled} primary onClick={next} />
)

const Save = connect(null, (dispatch, { id, url }) => ({
  saveConfig: () => dispatch(testConfig(id, url))
}))(({ saveConfig }) => (
  <RaisedButton label='Save' primary onClick={saveConfig} />
))

const BeginView = ({ onBegin, disabled }) => <RaisedButton disabled={disabled} primary label='begin' onClick={onBegin} />

const Begin = connect(null, { onBegin: next })(BeginView)

const Message = ({ type, message }) => (
  <div className={type === 'FAILURE' ? styles.error : styles.success}>{message}</div>
)

const StageView = (props) => {
  const {
    children,
    submitting = false,
    messages = [],
    defaults = {},

    setDefaults
  } = props

  return (
    <Mount on={() => setDefaults(defaults)}>
      <Paper className={styles.main}>
        {submitting
          ? <div className={styles.submitting}>
            <Flexbox justifyContent='center' alignItems='center' width='100%'>
              <CircularProgress size={60} thickness={7} />
            </Flexbox>
          </div>
          : null}
        <div className={styles.container}>
          {children}
          {messages.map((msg, i) => <Message key={i} {...msg} />)}
        </div>
      </Paper>
    </Mount>
  )
}

const Stage = connect((state, { id }) => ({
  messages: getMessages(state, id),
  submitting: isSubmitting(state, id)
}), { setDefaults, testConfig })(StageView)

const StageControls = ({ children, style = {}, ...rest }) => (
  <Flexbox style={{marginTop: 20, ...style}} justifyContent='space-between' {...rest}>
    {children}
  </Flexbox>
)

const NetworkSettings = ({ id, disabled }) => (
  <Stage id={id} defaults={{ port: 1389, encryptionMethod: 'Use LDAPS', hostName: 'localhost' }}>
    <Title>LDAP Network Settings</Title>
    <Description>
      Lets start with the network configurations of your LDAP store.
    </Description>

    <Hostname id='hostName' disabled={disabled} />
    <Port id='port' disabled={disabled} options={[389, 636]} />
    <Select id='encryptionMethod'
      label='Encryption Method'
      disabled={disabled}
      options={[ 'No Encryption', 'Use LDAPS', 'Use StartTLS' ]} />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapConnection' />
    </StageControls>
  </Stage>
)

const BindSettings = ({ id, disabled }) => (
  <Stage id={id} defaults={{ bindUserDn: 'cn=admin', bindUserPassword: 'secret' }}>
    <Title>LDAP Bind User Settings</Title>
    <Description>
      Now that we've figured out the network environment, we need to
      bind a user to the LDAP Store to retrieve additional information.
    </Description>

    <Input id='bindUserDn' disabled={disabled} label='Bind User DN' />
    <Password id='bindUserPassword' disabled={disabled} label='Bind User Password' />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapBind' />
    </StageControls>
  </Stage>
)

const QueryResult = (props) => {
  const { name, uid, cn, ou } = props

  return (
    <ListItem
      primaryText={ou || cn || uid || name}
      nestedItems={[
        <pre key={1}>{JSON.stringify(props, null, 2)}</pre>
      ]}
      primaryTogglesNestedList
    />
  )
}

const QueryView = ({ probe, probeValue = [], id, disabled }) => (
  <Stage id={id} defaults={{ query: 'objectClass=*', queryBase: 'dc=example,dc=com', baseUserDn: 'ou=users,dc=example,dc=com', baseGroupDn: 'ou=groups,dc=example,dc=com', userNameAttribute: 'uid' }}>

    <Title>LDAP Query</Title>

    <Description>
      The search fields below can be used to execute searches to help
      find the Base User DN, Group User DN and User name attribute
      required to setup LDAP.
    </Description>

    <Input id='query' disabled={disabled} label='Query' />
    <Input id='queryBase' disabled={disabled} label='Query Base DN' />

    <div style={{textAlign: 'right', marginTop: 20}} >
      <FlatButton disabled={disabled} secondary label='run query' onClick={() => probe('/admin/wizard/probe/ldap/ldapQuery')} />
    </div>

    {probeValue.length === 0
      ? null
      : <div className={styles.queryWindow}>
        <h2 className={styles.title}>Query Results</h2>
        <List>
          {probeValue.map((v, i) => <QueryResult key={i} {...v} />)}
        </List>
      </div>}

    <Input id='baseUserDn' disabled={disabled} label='Base User DN' />
    <Input id='baseGroupDn' disabled={disabled} label='Base Group DN' />
    <Input id='userNameAttribute' disabled={disabled} label='User Name Attribute' />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapDirStruct' />
    </StageControls>
  </Stage>
)

const Query = connect(
  (state) => ({ probeValue: getProbeValue(state) }),
  { probe }
)(QueryView)

const Confirm = ({ id }) => (
  <Stage id={id}>
    <Title>LDAP Confirm</Title>

    <Description>
      All of the values have been successfully verified. Would you like to
      save the LDAP configuration?
    </Description>

    <StageControls>
      <Back />
      <Save id={id} url='/admin/wizard/persist/ldap' />
    </StageControls>
  </Stage>
)

const StepperView = ({ children, step, submitting }) => (
  <div>
    {children.slice(0, step + 1).map((el, key) =>
      React.cloneElement(el, { key, disabled: key !== step }))}
  </div>
)

const Stepper = connect((state) => ({
  submitting: isSubmitting(state)
}))(StepperView)

const IntroductionStage = ({ disabled }) => (
  <Stage>
    <Title>Welcome to the LDAP Configuration Wizard</Title>
    <Description>
      This guide will walk through setting up the LDAP as an
      authentication source for users. To begin, make sure you
      have the hostname and port of the LDAP you plan to
      configure.
    </Description>

    <StageControls justifyContent='center'>
      <Begin disabled={disabled} />
    </StageControls>
  </Stage>
)

const LdapWizardView = ({ step }) => (
  <Stepper step={step}>
    <IntroductionStage />
    <NetworkSettings id='network-settings' />
    <BindSettings id='bind-settings' />
    <Query id='ldap-query' />
    <Confirm id='ldap-save' />
  </Stepper>
)

const LdapWizard = connect((state) => ({
  step: getStep(state)
}))(LdapWizardView)

export default LdapWizard
