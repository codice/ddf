import 'whatwg-fetch'

import { getAllConfig } from '../../reducer'

import { backendError, editConfig } from '../../actions'

export const submittingStart = (id) => ({ type: 'SUBMITTING_START', id })
export const submittingEnd = () => ({ type: 'SUBMITTING_END' })
export const setProbeValue = (value) => ({ type: 'SET_PROBE_VALUE', value })
export const setMessages = (id, messages) => ({ type: 'SET_MESSAGES', id, messages })
export const updateProgress = (id, value) => ({ type: 'UPDATE_PROGRESS', id, value })
export const clearMessages = (id) => ({ type: 'CLEAR_MESSAGES', id })
export const next = () => ({ type: 'NEXT_STEP' })
export const back = () => ({ type: 'BACK_STEP' })
export const nextStage = (stage) => ({ type: 'LDAP_ADD_STAGE', stage })
export const prevStage = () => ({ type: 'LDAP_REMOVE_STAGE' })
export const setMappingToAdd = (mapping) => ({type: 'SET_SELECTED_MAPPING', mapping})
export const addMapping = (mapping) => ({type: 'ADD_MAPPING', mapping})
export const setSelectedMappings = (indexs) => ({type: 'SELECT_MAPPINGS', indexs})
export const removeSelectedMappings = () => ({type: 'REMOVE_SELECTED_MAPPINGS'})
export const setOptions = (options) => ({type: 'SET_OPTIONS', options})

export const probe = (url) => (dispatch, getState) => {
  const config = getAllConfig(getState())

  const opts = {
    method: 'POST',
    body: JSON.stringify({ configurationType: 'ldap', ...config }),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        dispatch(setProbeValue(json.probeResults.ldapQueryResults))
      }
    })
    .catch(() => {
//    TODO handle probe errors
    })
}

export const probeAttributeMapping = (url, nextStageId) => (dispatch, getState) => {
  const config = getAllConfig(getState())

  const opts = {
    method: 'POST',
    body: JSON.stringify({ configurationType: 'ldap', ...config }),
    credentials: 'same-origin'
  }

  window.fetch(url, opts)
        .then((res) => Promise.all([ res.status, res.json() ]))
        .then(([status, json]) => {
          if (status === 200) {
            dispatch(editConfig('userAttributes', json.probeResults.ldapUserAttributes))
            dispatch(editConfig('subjectClaims', json.probeResults.subjectClaims))
            dispatch(nextStage(nextStageId))
          }
        })
        .catch(() => {
//    TODO handle probe errors
        })
}

export const probeLdapDir = (id, nextStageId) => (dispatch, getState) => {
  (id) ? dispatch(submittingStart(id)) : null
  const config = getAllConfig(getState())

  const opts = {
    method: 'POST',
    body: JSON.stringify({ configurationType: 'ldap', ...config }),
    credentials: 'same-origin'
  }

  window.fetch('/admin/wizard/probe/ldap/directoryStructure', opts)
    .then((res) => Promise.all([ res.status, res.json() ]))
    .then(([status, json]) => {
      if (status === 200) {
        dispatch(setOptions(json.probeResults))
        dispatch(editConfig('baseUserDn', json.probeResults.baseUserDn[0]))
        dispatch(editConfig('userNameAttribute', json.probeResults.userNameAttribute[0]))
        dispatch(editConfig('baseGroupDn', json.probeResults.baseGroupDn[0]))
        if (json.probeResults.groupObjectClass) dispatch(editConfig('groupObjectClass', json.probeResults.groupObjectClass[0]))
        if (json.probeResults.membershipAttribute) dispatch(editConfig('membershipAttribute', json.probeResults.membershipAttribute[0]))
        if (nextStageId) dispatch(nextStage(nextStageId))
      }
      dispatch(submittingEnd())
    })
    .catch(() => {
    //    TODO handle probe errors
    })
}
/*
 <InputAuto id='baseUserDn' disabled={disabled} label='Base User DN' />
 <InputAuto id='userNameAttribute' disabled={disabled} label='User Name Attribute' />
 <InputAuto id='baseGroupDn' disabled={disabled} label='Base Group DN' />
 {ldapUseCase === 'loginAndCredentialStore' || ldapUseCase === 'credentialStore'
 ? <div>
 <InputAuto id='groupObjectClass' disabled={disabled} label='LDAP Group ObjectClass' />
 <InputAuto id='membershipAttribute' disabled={disabled} label='LDAP Membership Attribute' />
 </div>
 : null}
 */

export const testConfig = (id, url, nextStageId, configType = 'ldap') => (dispatch, getState) => {
  dispatch(clearMessages(id))
  dispatch(submittingStart(id))

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
      dispatch(submittingEnd())
      if (status === 400) {
        dispatch(setMessages(id, json.messages))
      } else if (status === 200) {
        dispatch(setMessages(id, json.messages))
        dispatch(nextStage(nextStageId))
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', body }))
      }
    }, () => {
      dispatch(submittingEnd())
    })
}

export const testAndProbeConfig = (id, url, nextStageId, configType = 'ldap', probe) => (dispatch, getState) => {
  dispatch(clearMessages(id))
  dispatch(submittingStart(id))

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
      dispatch(submittingEnd())
      if (status === 400) {
        dispatch(setMessages(id, json.messages))
      } else if (status === 200) {
        dispatch(setMessages(id, json.messages))
        dispatch(probe)
      } else if (status === 500) {
        dispatch(backendError({ ...json, url, method: 'POST', body }))
      }
    }, () => {
      dispatch(submittingEnd())
    })
}
