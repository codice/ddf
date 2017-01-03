import { combineReducers } from 'redux-immutable'
import { fromJS, List, Map } from 'immutable'

export const getConfig = (state, id) => state.getIn(['config'].concat(id), Map()).toJS()
export const getAllConfig = (state) => state.get('config').map((config) => config.get('value')).toJS()

// TODO: add reducer checks for the wizardClear action to reset the state to defaults
const config = (state = Map(), { type, id, value, values, messages, options }) => {
  switch (type) {
    case 'EDIT_CONFIG':
      return state.setIn([id, 'value'], value)
    case 'SET_CONFIG_SOURCE':
      var formattedSource = {}
      Object.keys(value).map((key) => { formattedSource[key] = { value: value[key] } })
      return fromJS(state).map((value) => fromJS({value})).merge(formattedSource)
    case 'SET_DEFAULTS':
      return fromJS(values).map((value) => fromJS({ value })).merge(state)
    case 'SET_OPTIONS':
      return state.merge(fromJS(options).map((options) => fromJS({ options })))
    case 'CLEAR_CONFIG':
      return state.clear()
    case 'SET_MESSAGES':
      const errs = fromJS(messages)
        .filter((v) => v.has('configId'))
        .reduce((m, v) => m.set(v.get('configId'), Map.of('message', v)), Map())

      return state.mergeDeep(errs)
    case 'CLEAR_MESSAGES':
      return state.map((config) => config.delete('error').delete('success'))
    case 'CLEAR_WIZARD':
      return Map()
  }

  return state
}

const messages = (state = Map(), { type, id, messages }) => {
  switch (type) {
    case 'SET_MESSAGES':
      const msgs = fromJS(messages)
        .filter((m) => !m.has('configId'))

      return state.updateIn([id], List(), m => m.concat(msgs))
    case 'CLEAR_MESSAGES':
      return state.delete(id)
    case 'CLEAR_WIZARD':
      return Map()
    default:
      return state
  }
}

export const getMessages = (state, id) => state.getIn(['messages', id], List()).toJS()

const probeValue = (state = [], { type, value }) => {
  switch (type) {
    case 'SET_PROBE_VALUE':
      return value
    case 'CLEAR_WIZARD':
      return Map()
    default:
      return state
  }
}

const tableMappings = (state = [], { type, mapping, indexs }) => {
  switch (type) {
    case 'ADD_MAPPING':
      var duplicate = state.find((prev) => prev.subjectClaim === mapping.subjectClaim)

      return duplicate === undefined ? state.concat({subjectClaim: mapping.subjectClaim, userAttribute: mapping.userAttribute, selected: false}) : state
    case 'SELECT_MAPPINGS':
      return state.map((mapping, i) => {
        if (indexs.indexOf(i) !== -1) {
          return { ...mapping, selected: true }
        } else {
          return { ...mapping, selected: false }
        }
      })
    case 'REMOVE_SELECTED_MAPPINGS':
      return state.filter((mapping) => !mapping.selected)
    case 'CLEAR_WIZARD':
      return List()
    default:
      return state
  }
}

const mappingToAdd = (state = {}, { type, mapping }) => {
  switch (type) {
    case 'SET_SELECTED_MAPPING':
      return {subjectClaim: mapping.subjectClaim === undefined ? state.subjectClaim : mapping.subjectClaim,
        userAttribute: mapping.userAttribute === undefined ? state.userAttribute : mapping.userAttribute}
    case 'CLEAR_WIZARD':
      return Map()
    default:
      return state
  }
}

const selectedRemoveAttributeMapping = (state = [], {type, mappings}) => {
  switch (type) {
    case 'ADD_REMOVE_SELECTED_MAPPINGS':
      return state.concat(mappings)
    case 'CLEAR_WIZARD':
      return List()
    default:
      return state
  }
}

export const getProbeValue = (state) => state.getIn(['probeValue'])

const step = (state = 0, { type }) => {
  switch (type) {
    case 'NEXT_STEP':
      return state + 1
    case 'BACK_STEP':
      return (state > 0) ? state - 1 : 0
    case 'CLEAR_WIZARD':
      return 0
    default:
      return state
  }
}

export const getStep = (state) => state.get('step')

const submitting = (state = null, { type, id } = {}) => {
  switch (type) {
    case 'SUBMITTING_START':
      return id
    case 'SUBMITTING_END':
      return null
    case 'CLEAR_WIZARD':
      return null
    default:
      return state
  }
}

export const isSubmitting = (state, id) => state.get('submitting') === id

const ldapDisplayedStages = (state = ['introductionStage'], { type, stage }) => {
  switch (type) {
    case 'LDAP_ADD_STAGE':
      return [...state, stage]
    case 'LDAP_REMOVE_STAGE':
      return state.slice(0, -1)
    case 'CLEAR_WIZARD':
      return ['introductionStage']
    default:
      return state
  }
}

export const getDisplayedLdapStages = (state) => state.getIn(['ldapDisplayedStages'])

export default combineReducers({ config, probeValue, step, submitting, messages, ldapDisplayedStages, mappingToAdd, tableMappings, selectedRemoveAttributeMapping })

