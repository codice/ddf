import { post } from '../../fetch'

import { getAllConfig } from '../../reducer'

import { setDefaults } from '../../actions'

export const setProbeValue = (value) => ({ type: 'SET_PROBE_VALUE', value })
export const setMessages = (id, messages) => ({ type: 'SET_MESSAGES', id, messages })
export const updateProgress = (id, value) => ({ type: 'UPDATE_PROGRESS', id, value })
export const clearMessages = (id) => ({ type: 'CLEAR_MESSAGES', id })
export const nextStage = (stage) => ({ type: 'LDAP_ADD_STAGE', stage })
export const prevStage = () => ({ type: 'LDAP_REMOVE_STAGE' })
export const setMappingToAdd = (mapping) => ({type: 'SET_SELECTED_MAPPING', mapping})
export const addMapping = (mapping) => ({type: 'ADD_MAPPING', mapping})
export const setSelectedMappings = (indexs) => ({type: 'SELECT_MAPPINGS', indexs})
export const removeSelectedMappings = () => ({type: 'REMOVE_SELECTED_MAPPINGS'})
export const setOptions = (options) => ({type: 'SET_OPTIONS', options})

export const probeOptions = (id, url) => async (dispatch, getState) => {
  const config = getAllConfig(getState())
  const body = JSON.stringify({ configurationType: 'ldap', ...config })

  const res = await dispatch(post(url, { id, body }))
  const json = await res.json()

  if (res.status === 200) {
    let defaults = {}
    Object.keys(json.probeResults).forEach((key) => {
      defaults[key] = json.probeResults[key][0]
    })
    dispatch(setDefaults(defaults))
    dispatch(setOptions(json.probeResults))
  }
}

export const probe = (url) => async (dispatch, getState) => {
  const config = getAllConfig(getState())
  const body = JSON.stringify({ configurationType: 'ldap', ...config })

  const res = await dispatch(post(url, { body }))
  const json = await res.json()

  if (res.status === 200) {
    dispatch(setProbeValue(json.probeResults.ldapQueryResults))
  }
}

export const testConfig = (id, url, nextStageId, configurationType = 'ldap') => async (dispatch, getState) => {
  dispatch(clearMessages(id))

  const config = getAllConfig(getState())
  const body = JSON.stringify({ configurationType, ...config })

  const res = await dispatch(post(url, { id, body }))
  const json = await res.json()

  if (res.status === 400) {
    dispatch(setMessages(id, json.messages))
  } else if (res.status === 200) {
    dispatch(setMessages(id, json.messages))
    dispatch(nextStage(nextStageId))
  }
}
