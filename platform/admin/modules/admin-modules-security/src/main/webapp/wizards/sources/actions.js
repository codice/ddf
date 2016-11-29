import 'whatwg-fetch'

import { getAllConfig } from '../../reducer'

export const changeStage = (stageId) => ({ type: 'SOURCE_CHANGE_STAGE', stage: stageId })

export const resetStages = () => ({type: 'SOURCE_RESET_STAGES'})

export const stagesClean = () => ({type: 'SOURCE_CLEAN_STAGES'})

export const stagesModified = () => ({type: 'SOURCE_MODIFIED_STAGES'})

export const setStageProgress = (stageId) => ({ type: 'SET_CURRENT_PROGRESS', stage: stageId })

export const setNavStage = (stageId) => ({type: 'SOURCE_NAV_STAGE', stage: stageId})

export const setSourceSelections = (selections) => ({type: 'SET_SOURCE_SELECTIONS', sourceConfigs: selections})

export const setSelectedSource = (source) => ({type: 'SET_SELECTED_SOURCE', selectedSource: source})

export const clearConfiguration = () => ({type: 'CLEAR_CONFIG'})

export const testConfig = (url, configurationType, nextStageId) => (dispatch, getState) => {
  const config = getAllConfig(getState())

  const opts = {
    method: 'POST',
    body: JSON.stringify({ configurationType, ...config }),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 400) {
//      TODO dispatch error messages
//        dispatch(setConfigErrors(json.results))
      } else if (status === 200) {
        dispatch(setSourceSelections(json.probeResults.discoverSources))
        dispatch(changeStage(nextStageId))
      }
    })
}
