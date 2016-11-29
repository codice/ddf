import Paper from 'material-ui/Paper'
import * as styles from './styles.less'
import { connect } from 'react-redux'
import { combineReducers } from 'redux-immutable'
import AccountIcon from 'material-ui/svg-icons/action/account-circle'
import LanguageIcon from 'material-ui/svg-icons/action/language'
import RefreshIcon from 'material-ui/svg-icons/navigation/refresh'
import Flexbox from 'flexbox-react'
import Divider from 'material-ui/Divider'
import { cyan500 } from 'material-ui/styles/colors'
import IconButton from 'material-ui/IconButton'
import { Link } from 'react-router'

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

const SourceTile = ({ sourceName, type, sourceHostName, sourcePort, sourceUserName, sourceUserPassword, endpointUrl }) => {
  return (
    <Paper className={styles.main}>
      <div>{sourceName}</div>
      <div>{type}</div>
      <div>{sourceHostName}</div>
      <div>{sourcePort}</div>
      <div>{sourceUserName}</div>
      <div>{sourceUserPassword}</div>
      <div>{endpointUrl}</div>
    </Paper>)
}

const LdapTile = ({ sourceName, type, hostName, port, encryptionMethod, bindUserDn, bindUserPassword, userNameAttribute, baseGroupDn, baseUserDn }) => {
  return (
    <Paper className={styles.main}>
      <div>{sourceName}</div>
      <div>{type}</div>
      <div>{hostName}</div>
      <div>{port}</div>
      <div>{encryptionMethod}</div>
      <div>{bindUserDn}</div>
      <div>{bindUserPassword}</div>
      <div>{userNameAttribute}</div>
      <div>{baseGroupDn}</div>
      <div>{baseUserDn}</div>
    </Paper>)
}

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
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          <SourceWizardTile />
          <LdapWizardTile />
        </Flexbox>
        <Divider />
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          {sourceConfigs.length === 0 ? null : getSourceConfigTiles(sourceConfigs)}
        </Flexbox>
        <Divider />
        <Flexbox flexDirection='row' flexWrap='wrap' style={{width: '100%'}}>
          {ldapConfigs.length === 0 ? null : getLdapConfigTiles(ldapConfigs)}
        </Flexbox>
        <Paper circle style={{width: '50px', height: '50px', marginLeft: '25px'}}>
          <IconButton onClick={refresh}>
            <RefreshIcon style={{width: '50px', height: '50px'}} />
          </IconButton>
        </Paper>
      </span>
    </Flexbox>
  </div>
)

const refresh = () => (dispatch, getState) => {
  retrieveConfigurations('/admin/wizard/configurations/sources', setSourceConfigs)(dispatch, getState)
  retrieveConfigurations('/admin/wizard/configurations/ldap', setLdapConfigs)(dispatch, getState)
}

// actions
const retrieveConfigurations = (url, action) => (dispatch, getState) => {
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
