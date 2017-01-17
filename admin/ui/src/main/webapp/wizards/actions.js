import { post } from '../fetch'

import { getAllConfig } from '../reducer'

export const setMessages = (id, messages) => ({ type: 'SET_MESSAGES', id, messages })
export const updateProgress = (id, value) => ({ type: 'UPDATE_PROGRESS', id, value })
export const clearMessages = (id) => ({ type: 'CLEAR_MESSAGES', id })
export const nextStage = (stage) => ({ type: 'LDAP_ADD_STAGE', stage })
export const prevStage = () => ({ type: 'LDAP_REMOVE_STAGE' })
export const setOptions = (options) => ({type: 'SET_OPTIONS', options})
export const editConfig = (id, value) => ({ type: 'EDIT_CONFIG', id, value })
export const editConfigs = (values) => ({ type: 'EDIT_CONFIGS', values })
export const setDefaults = (values) => ({ type: 'SET_DEFAULTS', values })

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
