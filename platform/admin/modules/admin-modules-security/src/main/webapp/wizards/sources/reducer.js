import { combineReducers } from 'redux-immutable'
import { fromJS, Map } from 'immutable'

const sourceStage = (state = 'welcomeStage', { type, stage }) => {
  switch (type) {
    case 'SOURCE_CHANGE_STAGE':
      return stage
    case 'SOURCE_NAV_STAGE':
      return stage
    case 'CLEAR_WIZARD': // also make clear config info
      return 'welcomeStage'
    default:
      return state
  }
}

export const getSourceStage = (state) => state.getIn(['sourceWizard', 'sourceStage'])

const sourceStagesClean = (state = false, { type }) => {
  switch (type) {
    case 'SOURCE_CLEAN_STAGES':
      return true
    case 'SOURCE_MODIFIED_STAGES':
      return false
    case 'EDIT_CONFIG':
      return false
    case 'SOURCE_CHANGE_STAGE':
      return true
    case 'CLEAR_WIZARD':
      return false
    default:
      return state
  }
}

export const getStageProgress = (state) => state.getIn(['sourceWizard', 'sourceStageProgress'])

const sourceStageProgress = (state = 'welcomeStage', { type, stage }) => {
  switch (type) {
    case 'SET_CURRENT_PROGRESS':
      return stage
    case 'SOURCE_CHANGE_STAGE':
      return stage
    case 'CLEAR_WIZARD':
      return 'welcomeStage'
    default:
      return state
  }
}

export const getStagesClean = (state) => state.getIn(['sourceWizard', 'sourceStagesClean'])

export const getConfig = (state, id) => state.getIn(['wizard', 'config', id], Map()).toJS()

const sourceSelections = (state = Map(), { type, sourceConfigs }) => {
  switch (type) {
    case 'SET_SOURCE_SELECTIONS':
      return sourceConfigs
    case 'CLEAR_WIZARD':
      return Map()
    default :
      return state
  }
}

const isSubmitting = (state = false, { type }) => {
  switch (type) {
    case 'START_SUBMITTING':
      return true
    case 'END_SUBMITTING':
      return false
    case 'CLEAR_WIZARD':
      return false
    default:
      return state
  }
}

const configIds = (state = fromJS([]), { type, ids }) => {
  switch (type) {
    case 'SOURCES/SET_CONFIG_IDS':
      return fromJS(ids)

    default:
      return state
  }
}

// TODO: replace these absolute paths with relative ones like the other wizards
export const getConfigIds = (state) => state.getIn(['sourceWizard', 'configIds']).toJS()

export const getSourceSelections = (state) => state.getIn(['sourceWizard', 'sourceSelections'])

export const getConfigurationHandlerId = (state) => state.getIn(['wizard', 'config', 'configurationHandlerId'])

export const getSourceName = (state) => state.getIn(['wizard', 'config', 'sourceName', 'value'])

export const getIsSubmitting = (state) => state.getIn(['sourceWizard', 'isSubmitting'])

export default combineReducers({ sourceStage, sourceStagesClean, sourceStageProgress, sourceSelections, isSubmitting, configIds })

