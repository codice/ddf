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
import home from '../home'
import wcpm, * as webContext from '../adminTools/webContextPolicyManager/reducer'

import fetch, * as fetchSelectors from '../fetch'

export const getException = (state) => fetchSelectors.getException(state.get('fetch'))
export const isSubmitting = (state, id) => fetchSelectors.isSubmitting(state.get('fetch'), id)

export const getAllConfig = (state) => ldap.getAllConfig(state.get('wizard'))
export const getConfig = (state, id) => ldap.getConfig(state.get('wizard'), id)
export const getProbeValue = (state) => ldap.getProbeValue(state.get('wizard'))
export const getStep = (state) => ldap.getStep(state.get('wizard'))
export const getMessages = (state, id) => ldap.getMessages(state.get('wizard'), id)
export const getDisplayedLdapStage = (state) => ldap.getDisplayedLdapStage(state.get('wizard'))

export const getBins = (state) => webContext.getBins(state.get('wcpm'))
export const getOptions = (state) => webContext.getOptions(state.get('wcpm'))
export const getWhiteList = (state) => webContext.getWhiteList(state.get('wcpm'))
export const getEditingBinNumber = (state) => webContext.getEditingBinNumber(state.get('wcpm'))
export const getConfirmDelete = (state) => webContext.getConfirmDelete(state.get('wcpm'))

export default combineReducers({ fetch, wizard, backendError, sourceWizard, home, wcpm })
