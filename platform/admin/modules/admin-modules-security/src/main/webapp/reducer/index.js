import { combineReducers } from 'redux-immutable'

import { fromJS } from 'immutable'

export const getCurrentStage = (state) => state.get('stage').first().toJS()

export const getAllStages = (state) => state.get('stage').pop().toJS()

export const canGoBack = (state) => state.get('stage').size > 2

const clearError = (component) => {
  const noErrors = component.delete('error')

  if (noErrors.has('children')) {
    return noErrors.update('children', (children) => children.map(clearError))
  }

  return noErrors
}

const stage = (state = fromJS([{}]), { type, stage, id, value } = {}) => {
  switch (type) {
    case 'SET_STAGE':
      return state.unshift(fromJS(stage))
    case 'RESET_STAGE':
      return fromJS([ stage, {} ])
    case 'RESET_LAST_STAGE':
      return state.shift().unshift(fromJS(stage))
    case 'CLEAR_LAST_ERRORS':
      return state.updateIn([0, 'rootComponent'], clearError)
    case 'EDIT_VALUE':
      return state.setIn([ ...id, 'value' ], value)
    case 'BACK_STAGE':
      if (state.size > 2) {
        return state.shift()
      } else {
        return state
      }
    case 'CHANGE_DISPLAY_TYPE':
      return state.update()
    default:
      return state
  }
}

const submitting = (state = false, { type } = {}) => {
  switch (type) {
    case 'SUBMITTING_START':
      return true
    case 'SUBMITTING_END':
      return false
    default:
      return state
  }
}

export const isSubmitting = (state) => state.get('submitting')

const errors = (state = null, { type, message } = {}) => {
  switch (type) {
    case 'SET_STAGE':
      return null
    case 'DISMISS_ERRORS':
      return null
    case 'ERROR':
      return message
    default:
      return state
  }
}

export const getErrors = (state) => state.get('errors')

const list = (state = fromJS([]), { type, list } = {}) => {
  switch (type) {
    case 'SET_LIST':
      return fromJS(list)
    default:
      return state
  }
}

export const getList = (state) => state.get('list').toJS()

const backendError = (state = {}, { type, err } = {}) => {
  switch (type) {
    case 'BACKEND_ERRORS':
      return err
    default: return state
  }
}

export const getBackendErrors = (state) => state.get('backendError')

export default combineReducers({ list, errors, submitting, stage, backendError })
