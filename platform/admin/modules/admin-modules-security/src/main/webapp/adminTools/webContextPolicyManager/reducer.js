import { combineReducers } from 'redux-immutable'
import { fromJS, List } from 'immutable'

const emptyEditBin = fromJS({
  name: 'untitled',
  realm: '',
  authenticationTypes: [],
  requiredAttributes: [],
  contextPaths: [],
  editing: true
})

const bins = (state = List(), { type, bin, bins, path, binNumber, pathNumber, value, attribute }) => {
  switch (type) {
    case 'REPLACE_ALL_BINS':
      return fromJS(bins)

    // Bin Level
    case 'WCPM_ADD_BIN':
      return state.push(emptyEditBin)
    case 'WCPM_REMOVE_BIN':
      return state.delete(binNumber)
    case 'WCPM_EDIT_MODE_ON':
      return state.update(binNumber, (bin) => bin.merge({ beforeEdit: bin, editing: true }))
    case 'WCPM_EDIT_MODE_CANCEL':
      if (state.hasIn([binNumber, 'beforeEdit'])) {
        return state.update(binNumber, (bin) => bin.get('beforeEdit'))
      } else {
        return state.delete(binNumber)
      }
    case 'WCPM_EDIT_MODE_SAVE':
      return state.update(binNumber, (bin) => bin.delete('beforeEdit').merge({ editing: false }))

    // Realm
    case 'WCPM_EDIT_REALM':
      return state.setIn([binNumber, 'realm'], value)

    // Attribute Lists
    case 'WCPM_ADD_ATTRIBUTE_LIST':
      return state.update(binNumber, (bin) => bin.update(attribute, (paths) => paths.push(bin.get('new' + attribute))).set('new' + attribute, ''))
    case 'WCPM_REMOVE_ATTRIBUTE_LIST':
      return state.deleteIn([binNumber, attribute, pathNumber])
    case 'WCPM_EDIT_ATTRIBUTE_LIST':
      return state.setIn([binNumber, 'new' + attribute], value)

    // Attribute Mappings
    case 'WCPM_ADD_ATTRIBUTE_MAPPING':
      // TODO: don't allow adding a key that already exists
      return state.updateIn([binNumber, 'requiredAttributes', state.getIn([binNumber, 'newrequiredClaim'])], () => state.getIn([binNumber, 'newrequiredAttribute']))

    default:
      return state
  }
}

const options = (state = {}, { type, options }) => {
  switch (type) {
    case 'WCPM_SET_OPTIONS':
      return fromJS(options)
    default:
      return state
  }
}

export const getOptions = (state) => state.get('options').toJS()
export const getBins = (state) => state.get('bins').toJS()

export default combineReducers({ bins, options })

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
