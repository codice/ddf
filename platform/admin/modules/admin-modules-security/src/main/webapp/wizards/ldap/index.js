import React from 'react'

import { connect } from 'react-redux'
import { getProbeValue, isSubmitting, getMessages, getDisplayedLdapStages } from '../../reducer'
import { setDefaults } from '../../actions'

import Mount from '../../components/mount'
import Paper from 'material-ui/Paper'
import { RadioButton, RadioButtonGroup } from 'material-ui/RadioButton'
import RaisedButton from 'material-ui/RaisedButton'
import FlatButton from 'material-ui/FlatButton'
import CircularProgress from 'material-ui/CircularProgress'
import Flexbox from 'flexbox-react'
import {List, ListItem} from 'material-ui/List'
import * as styles from './styles.less'
import {testConfig, probe, nextStage, prevStage} from './actions'
import {Input, Password, Hostname, Port, Select} from '../inputs'
import Wizard from '../components/wizard'

const Title = ({children}) => <h1 className={styles.title}>{children}</h1>
const Description = ({children}) => <p className={styles.description}>{children}</p>

const BackView = ({onBack, disabled}) => (
  <FlatButton disabled={disabled} secondary label='back' onClick={onBack} />
)

const Back = connect(null, {onBack: prevStage})(BackView)

const mapDispatchToPropsNext = (dispatch, {id, url, nextStageId}) => ({
  next: () => dispatch(testConfig(id, url, nextStageId))
})

const NextView = ({next, disabled, nextStageId}) => <RaisedButton label='Next' disabled={disabled} primary onClick={next} />

const Next = connect(null, mapDispatchToPropsNext)(NextView)

const Save = connect(null, (dispatch, { id, url, configType }) => ({
  saveConfig: () => dispatch(testConfig(id, url, configType))
}))(({ saveConfig }) => (
  <RaisedButton label='Save' primary onClick={saveConfig} />
))

const BeginView = ({onBegin, disabled, next}) => <RaisedButton disabled={disabled} primary label='begin'
  onClick={next} />

const Begin = connect(null, (dispatch, { nextStageId }) => ({next: () => dispatch(nextStage(nextStageId))}))(BeginView)

const Message = ({type, message}) => (
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

const Stage = connect((state, {id}) => ({
  messages: getMessages(state, id),
  submitting: isSubmitting(state, id)
}), {setDefaults, testConfig})(StageView)

const StageControls = ({children, style = {}, ...rest}) => (
  <Flexbox style={{marginTop: 20, ...style}} justifyContent='space-between' {...rest}>
    {children}
  </Flexbox>
)

// TODO update description to described LDAP as a login or credential store
// TODO Make the value selected from the radio button persist
const IntroductionStage = ({ disabled }) => (
  <Stage>
    <Title>Welcome to the LDAP Configuration Wizard</Title>
    <Description>
      This guide will walk through setting up the LDAP as an
      authentication source for users. To begin, make sure you
      have the hostname and port of the LDAP you plan to. How do you plan to use LDAP?
      configure.
    </Description>
    <RadioButtonGroup id='ldapUseCase' name='ldapUseCases' defaultSelected='activeDirectory' >
      <RadioButton
        value='login'
        label='Login'
      />
      <RadioButton
        value='credentialStore'
        label='Credential store'
      />
      <RadioButton
        value='loginAndCredentialStore'
        label='Login and Credential Store'
      />
    </RadioButtonGroup>
    <StageControls justifyContent='center'>
      <Begin disabled={disabled} nextStageId='ldapTypeSelection' />
    </StageControls>
  </Stage>
)

// TODO Make the value selected from the radio button persist
const LdapTypeSelection = ({ id, disabled }) => (
  <Stage id={id}>
    <Title>LDAP Type Selection</Title>
    <Description>
      Select the type of LDAP you plan to connect to.
    </Description>
    <RadioButtonGroup id='ldapTypeSelection' name='ldapTypeSelections' defaultSelected='activeDirectory'>
      <RadioButton
        value='activeDirectory'
        label='Active Directory'
      />
      <RadioButton
        value='openDj'
        label='OpenDJ'
      />
      <RadioButton
        value='openLdap'
        label='OpenLDAP'
      />
      <RadioButton
        value='embeddedLdap'
        label='DDF Embedded LDAP'
      />
      <RadioButton
        label='Not Sure/None Of The Above'
       />
    </RadioButtonGroup>
    <StageControls>
      <Back disabled={disabled} />
      <Begin disabled={disabled} nextStageId='networkSettings' />
    </StageControls>
  </Stage>
)

const ConfigureEmbeddedLdap = ({ id, disabled }) => (
  <Stage id={id} defaults={{ embeddedLdapPort: 1389, embeddedLdapsPort: 1636, embeddedLdapAdminPort: 4444, embeddedLdapStorageLocation: 'etc/org.codice.opendj/ldap' }}>
    <Title>Configure DDF Embedded LDAP</Title>
    <Port id='embeddedLdapPort' label='LDAP port' disabled={disabled} />
    <Port id='embeddedLdapsPort' label='LDAPS port' disabled={disabled} />
    <Port id='embeddedLdapAdminPort' label='Admin port' disabled={disabled} />
    <div style={{textAlign: 'right', marginTop: 20}} >
      <Input id='ldifPath' disabled label='LDIF Path' />
      <RaisedButton disabled={disabled} label='Import Users' />
    </div>
    <div style={{textAlign: 'right', marginTop: 20}} >
      <Input id='embeddedLdapStorageLocation' disabled label='Storage Location' />
      <RaisedButton disabled={disabled} label='Set LDAP Storage Directory' />
    </div>
    <StageControls>
      <Back disabled={disabled} />
      <Save id={id} url='/admin/wizard/persist/embeddedLdap' configType='embeddedLdapConfiguration' nextStageId='successfullyConfiguredEmbeddedLdap' />
    </StageControls>
  </Stage>
)

const SuccessfullyConfiguredEmbeddedLdap = ({ id, disabled }) => (
  <Stage id={id}>
    <Title>DDF Embedded Has Successfully Been Started</Title>
    <StageControls>
      <Begin disabled={disabled} label='Continue LDAP Wizard' nextStageId='networkSettings' />
    </StageControls>
  </Stage>
)

const NetworkSettings = ({ id, disabled }) => (
  <Stage id={id} defaults={{ port: 1636, encryptionMethod: 'LDAPS', hostName: 'localhost' }}>
    <Title>LDAP Network Settings</Title>
    <Description>
      Lets start with the network configurations of your LDAP store.
    </Description>

    <Hostname id='hostName' disabled={disabled} />
    <Port id='port' disabled={disabled} options={[389, 636]} />
    <Select id='encryptionMethod'
      label='Encryption Method'
      disabled={disabled}
      options={[ 'None', 'LDAPS', 'StartTLS' ]} />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapConnection' nextStageId='bindSettings' />
    </StageControls>
  </Stage>
)

const BindSettings = ({id, disabled}) => (
  <Stage id={id} defaults={{bindUserDn: 'cn=admin', bindUserPassword: 'secret'}}>
    <Title>LDAP Bind User Settings</Title>
    <Description>
      Now that we've figured out the network environment, we need to
      bind a user to the LDAP Store to retrieve additional information.
    </Description>

    <Input id='bindUserDn' disabled={disabled} label='Bind User DN' />
    <Password id='bindUserPassword' disabled={disabled} label='Bind User Password' />

    <StageControls>
      <Back disabled={disabled} />
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapBind' nextStageId='query' />
    </StageControls>
  </Stage>
)

const QueryResult = (props) => {
  const {name, uid, cn, ou} = props

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

const QueryView = ({probe, probeValue = [], id, disabled}) => (
  <Stage id={id} defaults={{
    query: 'objectClass=*',
    queryBase: 'dc=example,dc=com',
    baseUserDn: 'ou=users,dc=example,dc=com',
    baseGroupDn: 'ou=groups,dc=example,dc=com',
    userNameAttribute: 'uid'
  }}>

    <Title>LDAP Query</Title>

    <Description>
      The search fields below can be used to execute searches to help
      find the Base User DN, Group User DN and User name attribute
      required to setup LDAP.
    </Description>

    <Input id='query' disabled={disabled} label='Query' />
    <Input id='queryBase' disabled={disabled} label='Query Base DN' />

    <div style={{textAlign: 'right', marginTop: 20}}>
      <FlatButton disabled={disabled} secondary label='run query'
        onClick={() => probe('/admin/wizard/probe/ldap/ldapQuery')} />
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
      <Next id={id} disabled={disabled} url='/admin/wizard/test/ldap/testLdapDirStruct' nextStageId='confirm' />
    </StageControls>
  </Stage>
)

const Query = connect(
  (state) => ({probeValue: getProbeValue(state)}),
  {probe}
)(QueryView)

const Confirm = ({id}) => (
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

let stageMapper = (stage, key) => {
  const stageMapping = {
    introductionStage: <IntroductionStage key={key} />,
    ldapTypeSelection: <LdapTypeSelection id='ldap-type-selection' key={key} />,
    configureEmbeddedLdap: <ConfigureEmbeddedLdap id='configure-embedded-ldap' key={key} />,
    successfullyConfiguredEmbeddedLdap: <SuccessfullyConfiguredEmbeddedLdap id='success-embedded-ldap' key={key} />,
    networkSettings: <NetworkSettings id='network-settings' key={key} />,
    bindSettings: <BindSettings id='bind-settings' key={key} />,
    query: <Query id='ldap-query' key={key} />,
    confirm: <Confirm id='ldap-save' key={key} />
  }
  return (stageMapping[stage] || (<div>Undefined Stage</div>))
}

const LdapWizardView = ({ stages, isSubmitting = false }) => (
  <Wizard id='ldap'>
    {stages.map((id, key) =>
      React.cloneElement(stageMapper(id), {key, disabled: key !== stages.length - 1}))}
  </Wizard>
)

const LdapWizard = connect((state) => ({
  stages: getDisplayedLdapStages(state)
}))(LdapWizardView)

export default LdapWizard
