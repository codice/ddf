import { combineReducers } from 'redux-immutable'

const backendError = (state = {}, { type, err } = {}) => {
  switch (type) {
    case 'BACKEND_ERRORS':
      return err
    case 'CLEAR_BACKEND_ERRORS':
      return {}
    default: return state
  }
}

export const getBackendErrors = (state) => state.get('backendError')

import wizard, * as ldap from '../wizards/ldap/reducer'
import sourceWizard from '../wizards/sources/reducer'
import sourcesHome from '../wizards/sourcesHome/index'

export const getAllConfig = (state) => ldap.getAllConfig(state.get('wizard'))
export const getConfig = (state, id) => ldap.getConfig(state.get('wizard'), id)
export const getProbeValue = (state) => ldap.getProbeValue(state.get('wizard'))
export const getStep = (state) => ldap.getStep(state.get('wizard'))
export const isSubmitting = (state, id) => ldap.isSubmitting(state.get('wizard'), id)
export const getMessages = (state, id) => ldap.getMessages(state.get('wizard'), id)
export const getDisplayedLdapStages = (state) => ldap.getDisplayedLdapStages(state.get('wizard'))

export default combineReducers({ wizard, backendError, sourceWizard, sourcesHome })
