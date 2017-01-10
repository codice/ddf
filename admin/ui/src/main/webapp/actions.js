export const setStage = (stage) => ({ type: 'SET_STAGE', stage })
export const resetStage = (stage) => ({ type: 'RESET_STAGE', stage })
export const resetLastStage = (stage) => ({ type: 'RESET_LAST_STAGE', stage })

export const edit = (id, value) => ({ type: 'EDIT_VALUE', id, value })
export const submittingStart = () => ({ type: 'SUBMITTING_START' })
export const submittingEnd = () => ({ type: 'SUBMITTING_END' })
export const back = () => ({ type: 'BACK_STAGE' })
export const changeDisplay = (value) => ({ type: 'CHANGE_DISPLAY_TYPE', value })
export const clearWizard = () => ({ type: 'CLEAR_WIZARD' })

export const networkError = () => ({
  type: 'ERROR',
  message: 'Cannot submit form. Network error.'
})

export const dismissErrors = () => ({ type: 'DISMISS_ERRORS' })
export const clearLastErrors = () => ({ type: 'CLEAR_LAST_ERRORS' })
export const backendError = (err) => ({ type: 'BACKEND_ERRORS', err })
export const clearBackendError = () => ({ type: 'CLEAR_BACKEND_ERRORS' })
export const editConfig = (id, value) => ({ type: 'EDIT_CONFIG', id, value })
export const editConfigs = (values) => ({ type: 'EDIT_CONFIGS', values })
export const setDefaults = (values) => ({ type: 'SET_DEFAULTS', values })

