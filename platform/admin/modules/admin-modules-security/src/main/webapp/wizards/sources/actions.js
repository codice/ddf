import 'whatwg-fetch'

import { getAllConfig } from '../../reducer'
import { backendError } from '../../actions'

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

export const testConfig = (url, configType, nextStageId, id) => (dispatch, getState) => {
  dispatch(startSubmitting())
  dispatch(clearMessages(id))
  const config = getAllConfig(getState())
  const body = { configurationType: configType, ...config }

  const opts = {
    method: 'POST',
    body: JSON.stringify(body),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      dispatch(endSubmitting())
      if (status === 400) {
        dispatch(setMessages(id, json.messages))
      } else if (status === 200) {
        dispatch(setSourceSelections(json.probeResults.discoverSources))
        dispatch(clearMessages(id))
        dispatch(changeStage(nextStageId))
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', body }))
      }
    }, () => {
      dispatch(endSubmitting())
    })
}
