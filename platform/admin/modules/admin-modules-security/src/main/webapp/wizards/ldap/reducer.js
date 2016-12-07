import { combineReducers } from 'redux-immutable'
import { fromJS, List, Map } from 'immutable'

export const getConfig = (state, id) => state.getIn(['config'].concat(id), Map()).toJS()
export const getAllConfig = (state) => state.get('config').map((config) => config.get('value')).toJS()

// TODO: add reducer checks for the wizardClear action to reset the state to defaults
const config = (state = Map(), { type, id, value, values, messages }) => {
  switch (type) {
    case 'EDIT_CONFIG':
      return state.setIn([id, 'value'], value)
    case 'SET_CONFIG_SOURCE':
      var formattedSource = {}
      Object.keys(value).map((key) => { formattedSource[key] = { value: value[key] } })
      return fromJS(state).map((stateValue) => fromJS({stateValue})).merge(formattedSource)
    case 'SET_DEFAULTS':
      return fromJS(values).map((value) => fromJS({ value })).merge(state)
    case 'CLEAR_CONFIG':
      return state.clear()
    case 'SET_MESSAGES':
      const errs = fromJS(messages)
        .filter((v) => v.has('configId'))
        .reduce((m, v) => m.set(v.get('configId'), Map.of('message', v)), Map())

      return state.mergeDeep(errs)
    case 'CLEAR_MESSAGES':
      return state.map((config) => config.delete('error').delete('success'))
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
    default:
      return state
  }
}

export const getMessages = (state, id) => state.getIn(['messages', id], List()).toJS()

const probeValue = (state = [], { type, value }) => {
  if (type === 'SET_PROBE_VALUE') {
    return value
  }
  return state
}

const tableMappings = (state = [], { type, mapping, indexs }) => {
  switch (type) {
    case 'ADD_MAPPING':
      var duplicate = state.find((prev) => prev.subjectClaim === mapping.subjectClaim && prev.userAttribute === mapping.userAttribute)
      return duplicate === undefined ? state.concat({subjectClaim: mapping.subjectClaim, userAttribute: mapping.userAttribute, selected: false}) : state
    case 'SELECT_MAPPINGS':
      state.map((mapping) => (mapping.selected = false))
      indexs.map((index) => (state[index].selected = true))
      return state
    case 'REMOVE_SELECTED_MAPPINGS':
      return state.filter((mapping) => !mapping.selected)
    default:
      return state
  }
}

const mappingToAdd = (state = {}, { type, mapping }) => {
  switch (type) {
    case 'SET_SELECTED_MAPPING':
      return {subjectClaim: mapping.subjectClaim === undefined ? state.subjectClaim : mapping.subjectClaim,
        userAttribute: mapping.userAttribute === undefined ? state.userAttribute : mapping.userAttribute}

    default:
      return state
  }
}

const selectedRemoveAttributeMapping = (state = [], {type, mappings}) => {
  switch (type) {
    case 'ADD_REMOVE_SELECTED_MAPPINGS':
      return state.concat(mappings)
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

