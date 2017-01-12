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

// todo tbatie - we need to revisist the vocabulary of this stuff
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

const SourceTileView = (props) => {
  const {
    sourceName,
    factoryPid,
    servicePid,
    sourceUserName,
    sourceUserPassword,
    endpointUrl,
    deleteConfig
  } = props

  return (
    <Paper className={styles.config}>
      <div title={servicePid} className={styles.tileTitle}>{sourceName} - {endpointUrl}</div>
      <div>Type: {getSourceTypeFromFactoryPid(factoryPid)}</div>
      <div>Username: {sourceUserName || 'none'}</div>
      <div>Password: {sourceUserPassword || 'none'}</div>

      <RaisedButton style={{marginTop: 20}} label='Delete' secondary onClick={deleteConfig} />
    </Paper>
  )
}

export const SourceTile = connect(
  null,
  (dispatch, { configurationType, factoryPid, servicePid }) => ({
    deleteConfig: () => deleteConfig('/admin/beta/config/persist/' + configurationType + '/delete', configurationType, factoryPid, servicePid, dispatch)
  }))(SourceTileView)

const LdapTileView = (props) => {
  const {
    hostName,
    port,
    encryptionMethod,
    bindUserDn,
    bindUserPassword,
    userNameAttribute,
    baseGroupDn,
    baseUserDn,
    deleteConfig
  } = props

  return (
    <Paper className={styles.config}>
      <div>Hostname: {hostName}</div>
      <div>Port: {port}</div>
      <div>Encryption Method: {encryptionMethod}</div>
      <div>Bind User DN: {bindUserDn}</div>
      <div>Bind User Password: {bindUserPassword}</div>
      <div>UserName Attribute: {userNameAttribute}</div>
      <div>Base Group DN: {baseGroupDn}</div>
      <div>Base User DN: {baseUserDn}</div>

      <RaisedButton style={{marginTop: 20}} label='Delete' primary onClick={deleteConfig} />
    </Paper>
  )
}
export const LdapTile = connect(
  null,
  (dispatch, { configurationType, factoryPid, servicePid }) => ({
    deleteConfig: () => deleteConfig('/admin/beta/config/persist/' + configurationType + '/delete', configurationType, factoryPid, servicePid, dispatch)
  }))(LdapTileView)

const TileLink = ({ to, title, subtitle, children }) => (
  <Link to={to}>
    <Paper className={styles.main}>
      <div style={{width: '100%', height: '100%'}}>
        <Flexbox
          alignItems='center'
          flexDirection='column'
          justifyContent='center'
          style={{width: '100%', height: '100%'}}>

          <p className={styles.titleTitle}>{title}</p>
          {children}
          <p className={styles.tileSubtitle}>{subtitle}</p>

        </Flexbox>
      </div>
    </Paper>
  </Link>
)

const SourceConfigTiles = ({ sourceConfigs }) => {
  if (sourceConfigs.length === 0) {
    return <div style={{margin: '20px'}}>No Sources Configured </div>
  }

  return (
    <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
      {sourceConfigs.map((v, i) => (<SourceTile key={i} {...v} />))}
    </Flexbox>
  )
}

const LdapConfigTiles = ({ ldapConfigs }) => {
  if (ldapConfigs.length === 0) {
    return <div style={{margin: '20px'}}>No LDAP's Configured</div>
  }

  return (
    <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
      {ldapConfigs.map((v, i) => (<LdapTile key={i} {...v} />))}
    </Flexbox>
  )
}

const SourcesHomeView = ({ sourceConfigs = [], ldapConfigs = [], refresh }) => (
  <div style={{width: '100%'}}>
    <AppBar title={<span style={styles.title}>Setup Wizards</span>}
      iconElementLeft={<div />}
      iconElementRight={<IconButton onClick={refresh}><RefreshIcon /></IconButton>}
      showMenuIconButton />

    <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
      <TileLink
        to='/sources/'
        title='Source Setup Wizard'
        subtitle='Setup a new source for federating'>
        <LanguageIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>

      <TileLink
        to='/web-context-policy-manager/'
        title='Endpoint Security'
        subtitle='Web context policy management'>
        <VpnLockIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>

      <TileLink
        to='/ldap/'
        title='LDAP Setup Wizard'
        subtitle='Configure LDAP as a login'>
        <AccountIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>
    </Flexbox>

    <Divider />

    <AppBar title={<span style={styles.title}>Source Configurations</span>} showMenuIconButton={false} />
    <SourceConfigTiles sourceConfigs={sourceConfigs} />

    <Divider />

    <AppBar title={<span style={styles.title}>LDAP Configurations</span>} showMenuIconButton={false} />
    <LdapConfigTiles ldapConfigs={ldapConfigs} />
  </div>
)

const refresh = () => (dispatch) => {
  retrieveConfigurations('/admin/beta/config/configurations/sources', setSourceConfigs)(dispatch)
  retrieveConfigurations('/admin/beta/config/configurations/ldap', setLdapConfigs)(dispatch)
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
