import React from 'react'

import { connect } from 'react-redux'
import { Map } from 'immutable'
import Flexbox from 'flexbox-react'
import { Link } from 'react-router'

import Paper from 'material-ui/Paper'
import AccountIcon from 'material-ui/svg-icons/action/account-circle'
import LanguageIcon from 'material-ui/svg-icons/action/language'
import RefreshIcon from 'material-ui/svg-icons/navigation/refresh'
import VpnLockIcon from 'material-ui/svg-icons/notification/vpn-lock'
import Divider from 'material-ui/Divider'
import { cyan500 } from 'material-ui/styles/colors'
import IconButton from 'material-ui/IconButton'
import RaisedButton from 'material-ui/RaisedButton'

import * as styles from './styles.less'
import { get, post } from '../fetch'

// actions

const setConfigs = (id, value) => ({type: 'SET_CONFIGS', id, value})

// async actions

const retrieve = (id) => async (dispatch) => {
  const res = await dispatch(get('/admin/beta/config/configurations/' + id))
  const json = await res.json()

  if (res.status === 200) {
    dispatch(setConfigs(id, json))
  }
}

const refresh = () => (dispatch) => {
  dispatch(retrieve('sources'))
  dispatch(retrieve('ldap'))
}

export const deleteConfig = ({ configurationType, factoryPid, servicePid }) => async (dispatch) => {
  const url = '/admin/beta/config/persist/' + configurationType + '/delete'
  const body = JSON.stringify({ configurationType, factoryPid, servicePid })

  const res = await dispatch(post(url, { body }))

  if (res.status === 200) {
    dispatch(refresh())
  }
}

// selectors

const getSourceConfigs = (state) => state.getIn(['home', 'sources'])
const getLdapConfigs = (state) => state.getIn(['home', 'ldap'])

// reducers

const configs = (state = Map(), { type, id, value = [] }) => {
  switch (type) {
    case 'SET_CONFIGS':
      return state.set(id, value)
    default:
      return state
  }
}

export default configs

// views

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

const SourceTileView = (props) => {
  const {
    sourceName,
    factoryPid,
    servicePid,
    sourceUserName,
    sourceUserPassword,
    endpointUrl,
    onDeleteConfig
  } = props

  return (
    <Paper className={styles.config}>
      <div title={servicePid} className={styles.tileTitle}>{sourceName} - {endpointUrl}</div>
      <div>Type: {getSourceTypeFromFactoryPid(factoryPid)}</div>
      <div>Username: {sourceUserName || 'none'}</div>
      <div>Password: {sourceUserPassword || 'none'}</div>

      <RaisedButton style={{marginTop: 20}} label='Delete' secondary onClick={onDeleteConfig} />
    </Paper>
  )
}

export const SourceTile = connect(null, (dispatch, props) => ({
  onDeleteConfig: () => dispatch(deleteConfig(props))
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
    onDeleteConfig
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

      <RaisedButton style={{marginTop: 20}} label='Delete' secondary onClick={onDeleteConfig} />
    </Paper>
  )
}
export const LdapTile = connect(null, (dispatch, props) => ({
  onDeleteConfig: () => dispatch(deleteConfig(props))
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

const Title = ({ children }) => (
  <h1 className={styles.title}>{children}</h1>
)

const SourcesHomeView = ({ sourceConfigs = [], ldapConfigs = [], onRefresh }) => (
  <div style={{width: '100%'}}>
    <Title>
      Setup Wizards
      <IconButton onClick={onRefresh}><RefreshIcon /></IconButton>
    </Title>

    <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
      <TileLink
        to='/sources'
        title='Source Setup Wizard'
        subtitle='Setup a new source for federating'>
        <LanguageIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>

      <TileLink
        to='/web-context-policy-manager'
        title='Endpoint Security'
        subtitle='Web context policy management'>
        <VpnLockIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>

      <TileLink
        to='/ldap'
        title='LDAP Setup Wizard'
        subtitle='Configure LDAP as a login'>
        <AccountIcon style={{color: cyan500, width: '50%', height: '50%'}} />
      </TileLink>
    </Flexbox>

    <Divider />

    <Title>Source Configurations</Title>
    <SourceConfigTiles sourceConfigs={sourceConfigs} />

    <Divider />

    <Title>LDAP Configurations</Title>
    <LdapConfigTiles ldapConfigs={ldapConfigs} />
  </div>
)

export const Home = connect((state) => ({
  sourceConfigs: getSourceConfigs(state),
  ldapConfigs: getLdapConfigs(state)
}), { onRefresh: refresh })(SourcesHomeView)

