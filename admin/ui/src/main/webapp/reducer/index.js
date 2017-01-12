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

export const getAllConfig = (state) => ldap.getAllConfig(state.get('wizard'))
export const getConfig = (state, id) => ldap.getConfig(state.get('wizard'), id)
export const getProbeValue = (state) => ldap.getProbeValue(state.get('wizard'))
export const getStep = (state) => ldap.getStep(state.get('wizard'))
export const isSubmitting = (state, id) => ldap.isSubmitting(state.get('wizard'), id)
export const getMessages = (state, id) => ldap.getMessages(state.get('wizard'), id)
export const getDisplayedLdapStages = (state) => ldap.getDisplayedLdapStages(state.get('wizard'))

export const getBins = (state) => webContext.getBins(state.get('wcpm'))
export const getOptions = (state) => webContext.getOptions(state.get('wcpm'))
export const getWhiteList = (state) => webContext.getWhiteList(state.get('wcpm'))
export const getEditingBinNumber = (state) => webContext.getEditingBinNumber(state.get('wcpm'))
export const getConfirmDelete = (state) => webContext.getConfirmDelete(state.get('wcpm'))

export default combineReducers({ wizard, backendError, sourceWizard, home, wcpm })
