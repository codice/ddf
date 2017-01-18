import 'whatwg-fetch'

import { getAllConfig } from '../../reducer'
import { backendError, clearWizard } from '../../actions'

export const changeStage = (stageId) => ({ type: 'SOURCE_CHANGE_STAGE', stage: stageId })
export const resetStages = () => ({type: 'SOURCE_RESET_STAGES'})
export const stagesClean = () => ({type: 'SOURCE_CLEAN_STAGES'})
export const stagesModified = () => ({type: 'SOURCE_MODIFIED_STAGES'})
export const setStageProgress = (stageId) => ({type: 'SET_CURRENT_PROGRESS', stage: stageId})
export const setNavStage = (stageId) => ({type: 'SOURCE_NAV_STAGE', stage: stageId})
export const setSourceSelections = (selections) => ({type: 'SET_SOURCE_SELECTIONS', sourceConfigs: selections})
export const setSelectedSource = (source) => ({type: 'SET_SELECTED_SOURCE', selectedSource: source})
export const clearConfiguration = () => ({type: 'CLEAR_CONFIG'})
export const setMessages = (id, messages) => ({ type: 'SET_MESSAGES', id, messages })
export const clearMessages = (id) => ({ type: 'CLEAR_MESSAGES', id })
export const startSubmitting = () => ({ type: 'START_SUBMITTING' })
export const endSubmitting = () => ({ type: 'END_SUBMITTING' })
export const setConfigSource = (source) => ({ type: 'SET_CONFIG_SOURCE', value: source })
export const setConfigTypes = (types) => ({ type: 'SOURCES/SET_CONFIG_IDS', types })

export const testSources = (url, configType, nextStageId, id) => (dispatch, getState) => {
  dispatch(startSubmitting())
  dispatch(clearMessages(id))
  const config = getAllConfig(getState())
  const body = { configurationType: configType, ...config }

  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }

  window.fetch('/admin/beta/config/test/sources/valid-url', opts)
      .then((res) => Promise.all([ res.status, res.json() ]))
      .then(([status, json]) => {
        if (status === 400) {
          dispatch(setMessages(id, json.messages))
          dispatch(endSubmitting())
        } else if (status === 200) {
          discoverSources(url, opts, dispatch, id, nextStageId, body)
        } else if (status === 500) {
          dispatch(backendError({ ...json, url, method: 'POST', body }))
          dispatch(endSubmitting())
        }
      })
}

const discoverSources = (url, opts, dispatch, id, nextStageId, body) => {
  dispatch(startSubmitting())
  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 400) {
        dispatch(setMessages(id, json.messages))
      } else if (status === 200) {
        dispatch(setSourceSelections(json.probeResults.discoveredSources))
        dispatch(clearMessages(id))
        dispatch(fetchConfigTypes(nextStageId))
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', body }))
      }
    })
}

export const persistConfig = (url, config, nextStageId, configType) => (dispatch, getState) => {
  dispatch(startSubmitting())
  const body = { configurationType: configType, ...getAllConfig(getState()) }

  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 400) {
//      TODO dispatch error messages
//        dispatch(setConfigErrors(json.results))
      } else if (status === 200) {
        dispatch(changeStage(nextStageId))
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', opts }))
      }
      dispatch(endSubmitting())
    })
}

export const fetchConfigTypes = (nextStageId) => (dispatch, getState) => {
  dispatch(startSubmitting())
  const config = getAllConfig(getState())
  const body = { configurationType: 'sources', ...config }

  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }

  window.fetch('/admin/beta/config/probe/sources/config-handlers', opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 400) {
//      TODO dispatch error messages
//        dispatch(setConfigErrors(json.results))
      } else if (status === 200) {
        dispatch(setConfigTypes(json.probeResults.sourceConfigHandlers))
        if (nextStageId) {
          dispatch(changeStage(nextStageId))
        }
      } else if (status === 500) {
        dispatch(backendError({ ...json, url: '/admin/beta/config/probe/sources/config-handlers', method: 'POST', body: JSON.stringify(config) }))
      }
      dispatch(endSubmitting())
    })
}

export const resetSourceWizardState = () => (dispatch) => {
  dispatch(clearWizard())
  dispatch(clearConfiguration())
}

export const testManualUrl = (endpointUrl, configType, nextStageId, id) => (dispatch, getState) => {
  dispatch(startSubmitting())
  dispatch(clearMessages(id))
  const config = getAllConfig(getState())
  const body = { ...config, configurationType: configType, endpointUrl: endpointUrl }
  const url = '/admin/beta/config/probe/' + configType + '/retrieveConfiguration'

  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 400) {
        dispatch(setMessages(id, json.messages))
      } else if (status === 200) {
        dispatch(setConfigSource(json.probeResults.retrieveConfiguration))
        dispatch(changeStage(nextStageId))
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', body }))
      }
      dispatch(endSubmitting())
    })
}
