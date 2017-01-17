import { combineReducers } from 'redux-immutable'
import { fromJS, List } from 'immutable'

const emptyBin = {
  name: 'untitled',
  realm: '',
  authenticationTypes: [],
  requiredAttributes: {},
  contextPaths: []
}

const bins = (state = List(), { type, bin, bins, whitelistContexts, path, binNumber, pathNumber, value, attribute, claim }) => {
  switch (type) {
    case 'WCPM/REPLACE_ALL_BINS':
      let whitelistBin = {...emptyBin, name: 'WHITELIST', contextPaths: whitelistContexts}
      return fromJS([whitelistBin, ...bins])

    // Bin Level
    case 'WCPM/ADD_BIN':
      return state.push(fromJS(emptyBin))
    case 'WCPM/CONFIRM_REMOVE_BIN':
      return state.delete(binNumber)
    case 'WCPM/EDIT_MODE_ON':
      return state.update(binNumber, (bin) => bin.merge({ beforeEdit: bin }))
    case 'WCPM/EDIT_MODE_CANCEL':
      if (state.hasIn([binNumber, 'beforeEdit'])) {
        return state.update(binNumber, (bin) => bin.get('beforeEdit'))
      } else {
        return state.delete(binNumber)
      }
    case 'WCPM/EDIT_MODE_SAVE':
      return state.update(binNumber, (bin) => bin.delete('beforeEdit'))

    // Realm
    case 'WCPM/EDIT_REALM':
      return state.setIn([binNumber, 'realm'], value)

    // Attribute Lists
    case 'WCPM/ADD_ATTRIBUTE_LIST':
      return state.update(binNumber, (bin) => bin.update(attribute, (paths) => paths.push(bin.get('new' + attribute))).set('new' + attribute, ''))
    case 'WCPM/REMOVE_ATTRIBUTE_LIST':
      return state.deleteIn([binNumber, attribute, pathNumber])
    case 'WCPM/EDIT_ATTRIBUTE_LIST':
      return state.setIn([binNumber, 'new' + attribute], value)

    // Attribute Mappings
    case 'WCPM/ADD_ATTRIBUTE_MAPPING':
      // TODO: don't allow adding a key that already exists
      return state.setIn([binNumber, 'requiredAttributes', state.getIn([binNumber, 'newrequiredClaim'])], state.getIn([binNumber, 'newrequiredAttribute'])).setIn([binNumber, 'newrequiredAttribute'], '').setIn([binNumber, 'newrequiredClaim'], '')
    case 'WCPM/REMOVE_ATTRIBUTE_MAPPING':
      return state.deleteIn([binNumber, 'requiredAttributes', claim])

    default:
      return state
  }
}
const emptyClaims = ({
  realms: [],
  authenticationTypes: [],
  claims: []
})

const options = (state = fromJS(emptyClaims), { type, options }) => {
  switch (type) {
    case 'WCPM/SET_OPTIONS':
      return fromJS(options)
    default:
      return state
  }
}

const editingBinNumber = (state = null, { type, binNumber }) => {
  switch (type) {
    case 'WCPM/EDIT_MODE_ON':
      return binNumber
    case 'WCPM/EDIT_MODE_CANCEL':
      return null
    case 'WCPM/EDIT_MODE_SAVE':
      return null
    case 'WCPM/ADD_BIN':
      return binNumber
    case 'WCPM/CONFIRM_REMOVE_BIN':
      return null
    default:
      return state
  }
}

const confirmDelete = (state = false, { type, binNumber }) => {
  switch (type) {
    case 'WCPM/REMOVE_BIN':
      return true
    case 'WCPM/CANCEL_REMOVE_BIN':
      return false
    case 'WCPM/CONFIRM_REMOVE_BIN':
      return null
    case 'WCPM/EDIT_MODE_CANCEL':
      return null
    case 'WCPM/EDIT_MODE_SAVE':
      return null
    default:
      return state
  }
}

export const getOptions = (state) => state.get('options').toJS()
export const getBins = (state) => state.get('bins').toJS()
export const getWhiteList = (state) => state.get('whiteList').toJS()
export const getEditingBinNumber = (state) => state.get('editingBinNumber')
export const getConfirmDelete = (state) => state.get('confirmDelete')

export default combineReducers({ bins, options, editingBinNumber, confirmDelete })

/*
// Example State Data Structure
const mockPolicies = [
  {
    realm: 'LDAP',
    authenticationTypes: [
      'authType3',
      'authType4'
    ],
    requiredAttributes: {
        http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role: 'system-admin',
        http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role: 'system-admin'
    }
    contextPaths: [
      '/'
    ]
  }
]

const mockClaims = ({
  realms: [
    'karaf',
    'LDAP',
    'IDP'
  ],
  authenticationTypes: [
    'karaf',
    'basic',
    'GUEST',
    'LDAP',
    'IDP',
    'SAML',
    'PKI'
  ],
  claims: [
    '{http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=system-user}',
    '{http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=system-admin}',
    '{http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=system-other}'
  ]
})
*/
