import React from 'react'

import { connect } from 'react-redux'
import { getProbeValue, isSubmitting, getMessages, getConfig, getDisplayedLdapStages } from '../../reducer'
import { setDefaults, editConfig } from '../../actions'

import Mount from '../../components/mount'
import {Card, CardActions, CardHeader} from 'material-ui/Card'
import {Table, TableBody, TableHeader, TableHeaderColumn, TableRow, TableRowColumn} from 'material-ui/Table'
import Paper from 'material-ui/Paper'
import RaisedButton from 'material-ui/RaisedButton'
import FlatButton from 'material-ui/FlatButton'
import CircularProgress from 'material-ui/CircularProgress'
import Flexbox from 'flexbox-react'
import {List, ListItem} from 'material-ui/List'
import SelectField from 'material-ui/SelectField'
import MenuItem from 'material-ui/MenuItem'
import * as styles from './styles.less'
import {testConfig, probe, probeLdapDir, nextStage, prevStage, probeAttributeMapping, setMappingToAdd, addMapping, setSelectedMappings, removeSelectedMappings, testAndProbeConfig} from './actions'
import {Input, InputAuto, Password, Hostname, Port, Select, RadioSelection} from '../inputs'
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

const mapDispatchToPropsProbeAndNext = (dispatch, {id, url, nextStageId, probe}) => ({
  nextAndProbe: () => dispatch(testAndProbeConfig(id, url, nextStageId, 'ldap', probe))
})
const ProbeAndNextView = ({nextAndProbe, disabled, nextStageId}) => <RaisedButton label='Next'
  disabled={disabled}
  primary
  onClick={nextAndProbe} />
const ProbeAndNext = connect(null, mapDispatchToPropsProbeAndNext)(ProbeAndNextView)

const Save = connect(null, (dispatch, {id, url, nextStageId, configType}) => ({
  saveConfig: () => dispatch(testConfig(id, url, nextStageId, configType))
}))(({saveConfig}) => (
  <RaisedButton label='Save' primary onClick={saveConfig} />
))

const BeginView = ({onBegin, disabled, next}) => <RaisedButton disabled={disabled} primary
  label='begin'
  onClick={next} />

const Begin = connect(null, (dispatch, {nextStageId}) => ({next: () => dispatch(nextStage(nextStageId))}))(BeginView)

const Message = ({type, message}) => (
  <div className={type === 'FAILURE' ? styles.error : styles.success}>{message}</div>
)

export const Submit = ({ label = 'Submit', onClick, disabled = false }) => (
  <RaisedButton label={label} disabled={disabled} primary onClick={onClick} />
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

const LdapUseCases = [{value: 'login', label: 'Login'},
{value: 'credentialStore', label: 'Credential store'},
{value: 'loginAndCredentialStore', label: 'Login and Credential Store'}
]
// TODO update description to described LDAP as a login or credential store
// TODO Make the value selected from the radio button persist
const IntroductionStageView = ({id, disabled, ldapUseCase}) => (
  <Stage id={id}>
    <Title>Welcome to the LDAP Configuration Wizard</Title>
    <Description>
      This guide will walk through setting up the LDAP as an
      authentication source for users. To begin, make sure you
      have the hostname and port of the LDAP you plan to. How do you plan to use LDAP?
    </Description>
    <RadioSelection id='ldapUseCase' options={LdapUseCases} name='LDAP Use Cases' disabled={disabled} />
    <StageControls justifyContent='center'>
      <Begin disabled={disabled || !ldapUseCase} nextStageId='ldapTypeSelection' />
    </StageControls>
  </Stage>
)
const getLdapUseCase = (state) => (getConfig(state, 'ldapUseCase') !== undefined ? getConfig(state, 'ldapUseCase').value : undefined)
const IntroductionStage = connect((state) => ({ ldapUseCase: getLdapUseCase(state) }))(IntroductionStageView)

// TODO Make the value selected from the radio button persist
const LdapTypes = [{value: 'activeDirectory', label: 'Active Directory'},
{value: 'openDj', label: 'OpenDJ'},
{value: 'openLdap', label: 'OpenLDAP'},
{value: 'embeddedLdap', label: 'DDF Embedded LDAP'},
{value: 'unknown', label: 'Not Sure/None Of The Above'}]

const LdapTypeSelectionView = ({ id, disabled, ldapType }) => (
  <Stage id={id}>
    <Title>LDAP Type Selection</Title>
    <Description>
      Select the type of LDAP you plan to connect to.
    </Description>
    <RadioSelection id='ldapType' options={LdapTypes} name='LDAP Type Selections' disabled={disabled} />
    <StageControls>
      <Back disabled={disabled} />
      <Begin disabled={disabled || !ldapType} nextStageId={ldapType === 'embeddedLdap' ? 'configureEmbeddedLdap' : 'networkSettings'} />
    </StageControls>
  </Stage>
)
const getLdapType = (state) => (getConfig(state, 'ldapType') !== undefined ? getConfig(state, 'ldapType').value : undefined)
const LdapTypeSelection = connect((state) => ({ldapType: getLdapType(state)}))(LdapTypeSelectionView)

const ConfigureEmbeddedLdap = ({ id, disabled }) => (
  <Stage id={id} defaults={{ embeddedLdapPort: 1389, embeddedLdapsPort: 1636, embeddedLdapAdminPort: 4444, embeddedLdapStorageLocation: 'etc/org.codice.opendj/ldap', ldifPath: 'etc/org.codice.opendj/ldap' }}>
    <Title>Configure DDF Embedded LDAP</Title>
    <Port id='embeddedLdapPort' label='LDAP port' disabled={disabled} />
    <Port id='embeddedLdapsPort' label='LDAPS port' disabled={disabled} />
    <Port id='embeddedLdapAdminPort' label='Admin port' disabled={disabled} />
    <div style={{textAlign: 'right', marginTop: 20}} >
      <Input id='ldifPath' disabled={disabled} label='LDIF Path' />
      <RaisedButton disabled={disabled} label='Import Users' />
    </div>
    <div style={{textAlign: 'right', marginTop: 20}} >
      <Input id='embeddedLdapStorageLocation' disabled label='Storage Location' />
      <RaisedButton disabled={disabled} label='Set LDAP Storage Directory' />
    </div>
    <StageControls>
      <Back disabled={disabled} />
      <Save id={id} label='mic check check' disabled={disabled} url='/admin/wizard/persist/embeddedLdap/create' configType='embeddedLdap' nextStageId='networkSettings' />
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

const BindSettingsView = ({id, disabled, probeLdapAndChangeStage}) => (
  <Stage id={id} defaults={{bindUserDn: 'cn=admin', bindUserPassword: 'secret', bindUserMethod: 'Simple'}}>
    <Title>LDAP Bind User Settings</Title>
    <Description>
      Now that we've figured out the network environment, we need to
      bind a user to the LDAP Store to retrieve additional information.
    </Description>

    <Input id='bindUserDn' disabled={disabled} label='Bind User DN' />
    <Password id='bindUserPassword' disabled={disabled} label='Bind User Password' />
    <Select id='bindUserMethod'
      label='Bind User Method'
      disabled={disabled}
      options={[ 'Simple', 'SASL', 'GSSAPI SASL', 'Digest MD5 SASL' ]} />

    {/* TODO GSSAPI SASL only */}
    <Input id='bindKdcAddress' disabled={disabled} label='KDC Address (for Kerberos authentication)' />

    {/* TODO GSSAPI and Digest MD5 SASL only */}
    <Input id='bindRealm' disabled={disabled} label='Realm (for Kerberos and Digest MD5 authentication)' />

    <StageControls>
      <Back disabled={disabled} />
      <Submit id={id} label='Next' onClick={() => probeLdapAndChangeStage(id, 'directorySettings')} />
    </StageControls>
  </Stage>
)
const BindSettings = connect(null, {
  probeLdapAndChangeStage: probeLdapDir
})(BindSettingsView)

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

const DirectorySettingsView = ({probe, probeAttributeMapping, probeValue = [], id, disabled, ldapUseCase, probeLdapDir}) => (
  <Stage id={id}>
    <Title>LDAP Directory Structure</Title>
    <Description>
      Next we need to configure the directories to for users/members and the attributes to use.
      Below is the LDAP Query Tool, capable of executing queries against the connected LDAP to discover the required field values
    </Description>
    <InputAuto id='baseUserDn' disabled={disabled} label='Base User DN' />
    <InputAuto id='userNameAttribute' disabled={disabled} label='User Name Attribute' />
    <InputAuto id='baseGroupDn' disabled={disabled} label='Base Group DN' />
    {ldapUseCase === 'loginAndCredentialStore' || ldapUseCase === 'credentialStore'
      ? <div>
        <InputAuto id='groupObjectClass' disabled={disabled} label='LDAP Group ObjectClass' />
        <InputAuto id='membershipAttribute' disabled={disabled} label='LDAP Membership Attribute' />
      </div>
      : null}
    <Card >
      <CardHeader style={{textAlign: 'center', fontSize: '1.1em'}}
        title='LDAP Query Tool'
        subtitle='Execute queries against the connected LDAP'
        actAsExpander
        showExpandableButton
      />
      <CardActions expandable style={{margin: '5px'}}>
        <InputAuto id='query' disabled={disabled} label='Query' />
        <InputAuto id='queryBase' disabled={disabled} label='Query Base DN' />

        <div style={{textAlign: 'right', marginTop: 20}}>
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
      </CardActions>
    </Card>

    <StageControls>
      <RaisedButton label='Lookup LDAP Directory' disabled={disabled} primary onClick={() => probeLdapDir()} />
      <Back disabled={disabled} />
      {ldapUseCase === 'loginAndCredentialStore' || ldapUseCase === 'credentialStore'
        ? (<ProbeAndNext id={id} disabled={disabled}
          url='/admin/wizard/test/ldap/testLdapDirStruct'
          probe={() => probeAttributeMapping('/admin/wizard/probe/ldap/subjectAttributeMap', 'attributeMapping')} />)
        : (<Next id={id}
          disabled={disabled}
          url='/admin/wizard/test/ldap/testLdapDirStruct'
          nextStageId={'confirm'} />)}
    </StageControls>
  </Stage>
)

const DirectorySettings = connect(
    (state) => ({probeValue: getProbeValue(state), ldapUseCase: getLdapUseCase(state)}),
    {probe, probeAttributeMapping, probeLdapDir}
)(DirectorySettingsView)

const LdapAttributeMappingStageView = (props) => {
  const {
    id,
    disabled,
    tableMappings = [],
    subjectClaims = [],
    userAttributes = [],
    mappingToadd,

    // actions
    setMappingToAdd,
    addMapping,
    setSelectedMappings,
    removeSelectedMappings
  } = props

  return (
    <Stage id={id}>
      <Title>LDAP User Attribute Mapping</Title>
      <Description>
        In order to authenticate users, the attributes of the users must be mapped to the STS
        claims.
        Not all attributes must be mapped but any unmapped attributes will not be used for
        authentication.
        Claims can be mapped to 1 or more attributes.
      </Description>
      <SelectField floatingLabelText='STS Claim'
        value={mappingToadd.subjectClaim}
        style={{width: '100%', clear: 'both'}}
        onChange={(e, i) => setMappingToAdd({subjectClaim: subjectClaims[i]})}>
        {subjectClaims.map((claim, i) => <MenuItem key={i} value={claim} primaryText={claim} />)}
      </SelectField>
      <SelectField floatingLabelText='LDAP User Attribute'
        value={mappingToadd.userAttribute}
        style={{width: '100%', clear: 'both'}}
        onChange={(e, i) => setMappingToAdd({userAttribute: userAttributes[i]})}>
        {userAttributes.map((attri, i) => <MenuItem key={i} value={attri}
          primaryText={attri} />)}
      </SelectField>
      <RaisedButton
        label='Add Mapping'
        primary
        disabled={mappingToadd.subjectClaim === undefined || mappingToadd.userAttribute === undefined}
        style={{margin: '0 auto', marginBottom: '30px', marginTop: '10px', display: 'block'}}
        onClick={() => addMapping(mappingToadd)} />
      <Card expanded style={{ width: '100%' }}>
        <CardHeader style={{ fontSize: '0.80em' }}>
          <Title>STS Claims to LDAP Attribute Mapping</Title>
          <Description>
            The mappings below will be saved.
          </Description>
        </CardHeader>
        <Table onRowSelection={(indexs) => setSelectedMappings(indexs)}
          multiSelectable>
          <TableHeader displaySelectAll={false} adjustForCheckbox={false}>
            <TableRow >
              <TableHeaderColumn>STS Claim</TableHeaderColumn>
              <TableHeaderColumn style={{ width: 120 }}>LDAP User Attribute</TableHeaderColumn>
            </TableRow>
          </TableHeader>
          <TableBody showRowHover deselectOnClickaway={false}>
            {tableMappings.map((mapping, i) =>
              <TableRow key={i} selected={mapping.selected}>
                <TableRowColumn>
                  <span style={{cursor: 'help'}} title={mapping.subjectClaim}>{mapping.subjectClaim}</span>
                </TableRowColumn>
                <TableRowColumn style={{ width: 120 }}>{mapping.userAttribute}</TableRowColumn>
              </TableRow>)}
          </TableBody>
        </Table>
        <RaisedButton
          label='Remove Selected Mappings'
          primary
          style={{display: 'block'}}
          disabled={tableMappings.filter((mapping) => mapping.selected).length === 0}
          onClick={() => removeSelectedMappings()} />
      </Card>
      <StageControls>
        <Back disabled={disabled} />
        <NextAttributeMapping id={id} disabled={disabled || tableMappings.length === 0} url='/admin/wizard/test/ldap/testAttributeMapping' attributeMappings={toAttributeMapping(tableMappings)}
          nextStageId='confirm' />
      </StageControls>
    </Stage>
  )
}

const toAttributeMapping = (tableMappings) => {
  return (tableMappings.length !== 0) ? tableMappings.reduce((prevObj, mapping) => {
    prevObj[mapping.subjectClaim] = mapping.userAttribute
    return prevObj
  }, {}) : {}
}
// todo Need to transform map into a string, list map
const mapDispatchToPropsNextAttributeMapping = (dispatch, {id, url, nextStageId, attributeMappings}) => ({
  next: () => { dispatch(editConfig('attributeMappings', attributeMappings)); dispatch(testConfig(id, url, nextStageId)) }
})
const NextAttributeMappingView = ({next, disabled, nextStageId}) => <RaisedButton label='Next' disabled={disabled} primary onClick={next} />
const NextAttributeMapping = connect(null, mapDispatchToPropsNextAttributeMapping)(NextAttributeMappingView)

const getSubjectClaims = (state) => (getConfig(state, 'subjectClaims') !== undefined ? getConfig(state, 'subjectClaims').value : undefined)
const getUserAttributes = (state) => (getConfig(state, 'userAttributes') !== undefined ? getConfig(state, 'userAttributes').value : undefined)
const getTableMappings = (state) => state.getIn(['wizard', 'tableMappings'])
const getMappingToAdd = (state) => state.getIn(['wizard', 'mappingToAdd'])
const LdapAttributeMappingStage = connect(
  (state) => ({
    subjectClaims: getSubjectClaims(state),
    userAttributes: getUserAttributes(state),
    tableMappings: getTableMappings(state),
    mappingToadd: getMappingToAdd(state)
  }),

  {
    probeAttributeMapping,
    setMappingToAdd,
    addMapping,
    setSelectedMappings,
    removeSelectedMappings
  }
)(LdapAttributeMappingStageView)

const Confirm = ({id}) => (
  <Stage id={id}>
    <Title>LDAP Confirm</Title>

    <Description>
      All of the values have been successfully verified. Would you like to
      save the LDAP configuration?
    </Description>

    <StageControls>
      <Back />
      <Save id={id} url='/admin/wizard/persist/ldap/create' />
    </StageControls>
  </Stage>
)

let stageMapper = (stage, key) => {
  const stageMapping = {
    introductionStage: <IntroductionStage key={key} />,
    ldapTypeSelection: <LdapTypeSelection id='ldap-type-selection' key={key} />,
    configureEmbeddedLdap: <ConfigureEmbeddedLdap id='configure-embedded-ldap' key={key} />,
    networkSettings: <NetworkSettings id='network-settings' key={key} />,
    bindSettings: <BindSettings id='bind-settings' key={key} />,
    directorySettings: <DirectorySettings id='dir-settings' key={key} />,
    attributeMapping: <LdapAttributeMappingStage id='attribute-mapping' key={key} />,
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
