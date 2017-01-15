import { getBins, getWhiteList } from '../../reducer'
import { backendError } from '../../actions'

// Bin level
export const replaceAllBins = (bins) => ({ type: 'WCPM/REPLACE_ALL_BINS', bins })
export const removeBin = () => ({ type: 'WCPM/REMOVE_BIN' })
export const addNewBin = (binNumber) => ({ type: 'WCPM/ADD_BIN', binNumber })
export const editModeOn = (binNumber) => ({ type: 'WCPM/EDIT_MODE_ON', binNumber })
export const editModeCancel = (binNumber) => ({ type: 'WCPM/EDIT_MODE_CANCEL', binNumber })
export const editModeSave = (binNumber) => ({ type: 'WCPM/EDIT_MODE_SAVE', binNumber })
export const confirmRemoveBin = (binNumber) => ({ type: 'WCPM/CONFIRM_REMOVE_BIN', binNumber })
export const cancelRemoveBin = () => ({ type: 'WCPM/CANCEL_REMOVE_BIN' })

// Realm
export const editRealm = (binNumber, value) => ({ type: 'WCPM/EDIT_REALM', binNumber, value })

// Attribute Lists
export const removeAttribute = (attribute) => (binNumber, pathNumber) => ({ type: 'WCPM/REMOVE_ATTRIBUTE_LIST', attribute, binNumber, pathNumber })
export const addAttribute = (attribute) => (binNumber, path) => ({ type: 'WCPM/ADD_ATTRIBUTE_LIST', attribute, binNumber, path })
export const editAttribute = (attribute) => (binNumber, value) => ({ type: 'WCPM/EDIT_ATTRIBUTE_LIST', attribute, binNumber, value })

// Attribute mapping reducers
export const addAttributeMapping = (binNumber) => ({ type: 'WCPM/ADD_ATTRIBUTE_MAPPING', binNumber })
export const removeAttributeMapping = (binNumber, claim) => ({ type: 'WCPM/REMOVE_ATTRIBUTE_MAPPING', binNumber, claim })

// Set Options
export const setPolicyOptions = (options) => ({ type: 'WCPM/SET_OPTIONS', options })

// Whitelist
export const replaceWhitelist = (whiteList) => ({ type: 'WCPM/REPLACE_WHITELIST', whiteList })

// Fetch
export const updatePolicyBins = (url) => (dispatch, getState) => {
  const opts = {
    method: 'GET',
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        dispatch(replaceAllBins(json[0].contextPolicyBins))
        dispatch(replaceWhitelist(json[0].whiteListContexts))
        dispatch(fetchOptions('/admin/beta/config/probe/context-policy-manager/options'))
      }
    })
    .catch(() => {
//    TODO handle probe errors
    })
}

export const persistChanges = (binNumber, url) => (dispatch, getState) => {
  dispatch(editModeSave(binNumber))

  const formattedBody = {
    configurationType: 'context-policy-manager',
    contextPolicyBins: getBins(getState()),
    whiteListContexts: getWhiteList(getState())
  }

  const opts = {
    method: 'POST',
    body: JSON.stringify(formattedBody),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
      } else {
        dispatch(backendError(json))
      }
    })
    .catch(() => {
//    TODO handle probe errors
    })
}

export const fetchOptions = (url) => (dispatch, getState) => {
  const formattedBody = {
    configurationType: 'context-policy-manager',
    contextPolicyBins: getBins(getState()),
    whiteListContexts: getWhiteList(getState())
  }

  const opts = {
    method: 'POST',
    body: JSON.stringify(formattedBody),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        dispatch(setPolicyOptions(json.probeResults))
      } else {
      }
    })
    .catch(() => {
//    TODO handle probe errors
    })
}

export const confirmRemoveBinAndPersist = (binNumber, url) => (dispatch) => {
  dispatch(confirmRemoveBin(binNumber))
  dispatch(persistChanges(binNumber, url))
}
