import React from 'react'

import Paper from 'material-ui/Paper'
import * as styles from './styles.less'
import { connect } from 'react-redux'
import { combineReducers } from 'redux-immutable'
import AccountIcon from 'material-ui/svg-icons/action/account-circle'
import LanguageIcon from 'material-ui/svg-icons/action/language'
import RefreshIcon from 'material-ui/svg-icons/navigation/refresh'
import VpnLockIcon from 'material-ui/svg-icons/notification/vpn-lock'
import Flexbox from 'flexbox-react'
import Divider from 'material-ui/Divider'
import { cyan500 } from 'material-ui/styles/colors'
import IconButton from 'material-ui/IconButton'
import { Link } from 'react-router'
import RaisedButton from 'material-ui/RaisedButton'
import AppBar from 'material-ui/AppBar'

// map state to props
const setSourceConfigs = (value) => ({type: 'SET_SOURCE_CONFIGS', value})
const getSourceConfigs = (state) => {
  return state.getIn(['sourcesHome', 'sourceConfigs'])
}

const setLdapConfigs = (value) => ({type: 'SET_LDAP_CONFIGS', value})
const getLdapConfigs = (state) => state.getIn(['sourcesHome', 'ldapConfigs'])

const TileTitle = ({id, text}) => (
  <div>
    <p id={id} className={styles.titleTitle}>{text}</p>
  </div>
)

const TileSubtitle = ({id, text}) => (
  <div>
    <p id={id} className={styles.tileSubtitle}>{text}</p>
  </div>
)

const getSourceTypeFromFactoryPid = (factoryPid) => {
  if (factoryPid.includes('Wfs_v1_0_0')) {
    return 'WFS v1 Source'
  } else if (factoryPid.includes('Wfs_v2_0_0')) {
    return 'WFS v2 Source'
  } else if (factoryPid.includes('Csw_Federation_Profile_Source')) {
    return 'DDF Extended Capabilities CSW Source'
  } else if (factoryPid.includes('Gmd_Csw_Federated_Source')) {
    return 'GMD CSW Specification Source'
  } else if (factoryPid.includes('Csw_Federated_Source')) {
    return 'CSW Specification Source'
  } else if (factoryPid.includes('OpenSearchSource')) {
    return 'Open Search Source'
  } else {
    return 'Unknown'
  }
}

export const deleteConfig = (url, configurationType, factoryPid, servicePid, dispatch) => {
  const body = { configurationType, factoryPid, servicePid }
  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }
  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        refresh()(dispatch)
      }
    })
}

const SourceTileView = ({ sourceName, factoryPid, servicePid, sourceUserName, sourceUserPassword, endpointUrl, deleteConfig }) => {
  return (
    <Paper className={styles.main}>
      <div>Source Name: {sourceName}</div>
      <div>Service Pid: {servicePid}</div>
      <div>Type: {getSourceTypeFromFactoryPid(factoryPid)}</div>
      <div>Username: {sourceUserName === undefined || sourceUserName === '' ? 'none' : sourceUserName}</div>
      <div>Password: {sourceUserPassword === undefined || sourceUserPassword === '' ? 'none' : sourceUserPassword}</div>
      <div>Query Endpoint: {endpointUrl}</div>
      <RaisedButton label='Delete' primary onClick={deleteConfig} />
    </Paper>)
}
export const SourceTile = connect(
  null,
  (dispatch, { configurationType, factoryPid, servicePid }) => ({
    deleteConfig: () => deleteConfig('/admin/wizard/persist/' + configurationType + '/delete', configurationType, factoryPid, servicePid, dispatch)
  }))(SourceTileView)

const LdapTileView = ({ type, hostName, port, encryptionMethod, bindUserDn, bindUserPassword, userNameAttribute, baseGroupDn, baseUserDn, deleteConfig }) => {
  return (
    <Paper className={styles.main}>
      <div>Ldap Type: {type}</div>
      <div>Hostname: {hostName}</div>
      <div>Port: {port}</div>
      <div>Encryption Method: {encryptionMethod}</div>
      <div>Bind User DN: {bindUserDn}</div>
      <div>Bind User Password: {bindUserPassword}</div>
      <div>UserName Attribute: {userNameAttribute}</div>
      <div>Base Group DN: {baseGroupDn}</div>
      <div>Base User DN: {baseUserDn}</div>
      <RaisedButton label='Delete' primary onClick={deleteConfig} />
    </Paper>)
}
export const LdapTile = connect(
  null,
  (dispatch, { configurationType, factoryPid, servicePid }) => ({
    deleteConfig: () => deleteConfig('/admin/wizard/persist/' + configurationType + '/delete', configurationType, factoryPid, servicePid, dispatch)
  }))(LdapTileView)

const SourceWizardTile = () => (
  <Link to='/sources/'>
    <Paper className={styles.main}>
      <div style={{width: '100%', height: '100%'}}>
        <Flexbox alignItems='center' flexDirection='column' justifyContent='center' style={{width: '100%', height: '100%'}}>
          <TileTitle text='Source Setup Wizard' />
          <LanguageIcon style={{color: cyan500, width: '50%', height: '50%'}} />
          <TileSubtitle text='Setup a new source for federating' />
        </Flexbox>
      </div>
    </Paper>
  </Link>
)

const ContextPolicyManagerTile = () => (
  <Link to='/webContextPolicyManager/'>
    <Paper className={styles.main}>
      <div style={{width: '100%', height: '100%'}}>
        <Flexbox alignItems='center' flexDirection='column' justifyContent='center' style={{width: '100%', height: '100%'}}>
          <TileTitle text='Endpoint Security' />
          <VpnLockIcon style={{color: cyan500, width: '50%', height: '50%'}} />
          <TileSubtitle text='Web context policy management' />
        </Flexbox>
      </div>
    </Paper>
  </Link>
)

const LdapWizardTile = () => (
  <Link to='/ldap/'>
    <Paper className={styles.main}>
      <div style={{width: '100%', height: '100%'}}>
        <Flexbox alignItems='center' flexDirection='column' justifyContent='center' style={{width: '100%', height: '100%'}}>
          <TileTitle text='LDAP Setup Wizard' />
          <AccountIcon style={{color: cyan500, width: '50%', height: '50%'}} />
          <TileSubtitle text='Configure LDAP as a login' />
        </Flexbox>
      </div>
    </Paper>
  </Link>
)

const getSourceConfigTiles = (sourceConfigs) => {
  return sourceConfigs.map((v, i) => (<SourceTile key={i} {...v} />))
}

const getLdapConfigTiles = (ldapConfigs) => {
  return ldapConfigs.map((v, i) => (<LdapTile key={i} {...v} />))
}

const SourcesHomeView = ({sourceConfigs = [], ldapConfigs = [], refresh}) => (
  <div style={{width: '100%'}}>
    <Flexbox flexDirection='row' style={{width: '100%'}}>
      <span style={{width: '100%', height: '100%'}}>
        <AppBar title={<span style={styles.title}>Setup Wizards</span>}
          iconElementLeft={<div />}
          iconElementRight={<IconButton onClick={refresh}><RefreshIcon /></IconButton>}
          showMenuIconButton />
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          <SourceWizardTile />
          <LdapWizardTile />
          <ContextPolicyManagerTile />
        </Flexbox>
        <Divider />
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          <AppBar title={<span style={styles.title}>Source Configurations</span>} showMenuIconButton={false} />
          {sourceConfigs.length === 0 ? <div style={{margin: '20px'}}>No Sources Configured </div> : getSourceConfigTiles(sourceConfigs)}
        </Flexbox>
        <Divider />
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          <AppBar title={<span style={styles.title}>LDAP Configurations</span>} showMenuIconButton={false} />
          {ldapConfigs.length === 0 ? <div style={{margin: '20px'}}>No LDAP's Configured</div> : getLdapConfigTiles(ldapConfigs)}
        </Flexbox>
      </span>
    </Flexbox>
  </div>
)

const refresh = () => (dispatch) => {
  retrieveConfigurations('/admin/wizard/configurations/sources', setSourceConfigs)(dispatch)
  retrieveConfigurations('/admin/wizard/configurations/ldap', setLdapConfigs)(dispatch)
}

// actions
const retrieveConfigurations = (url, action) => (dispatch) => {
  const opts = {
    method: 'GET',
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        dispatch(action(json))
      }
    })
}

export const SourcesHome = connect((state) => ({ sourceConfigs: getSourceConfigs(state), ldapConfigs: getLdapConfigs(state) }),
  { refresh }
)(SourcesHomeView)

// reducers
const sourceConfigs = (state = [], {type, value = []}) => {
  switch (type) {
    case 'SET_SOURCE_CONFIGS':
      return value
    default:
      return state
  }
}

const ldapConfigs = (state = [], {type, value}) => {
  switch (type) {
    case 'SET_LDAP_CONFIGS':
      return value
    default:
      return state
  }
}

export default combineReducers({ sourceConfigs, ldapConfigs })
