import { post } from '../../fetch'

import { getAllConfig } from '../../reducer'

export const setProbeValue = (value) => ({ type: 'SET_PROBE_VALUE', value })
export const setMappingToAdd = (mapping) => ({type: 'SET_SELECTED_MAPPING', mapping})
export const addMapping = (mapping) => ({type: 'ADD_MAPPING', mapping})
export const setSelectedMappings = (indexs) => ({type: 'SELECT_MAPPINGS', indexs})
export const removeSelectedMappings = () => ({type: 'REMOVE_SELECTED_MAPPINGS'})

export const probe = (url) => async (dispatch, getState) => {
  const config = getAllConfig(getState())
  const body = JSON.stringify({ configurationType: 'ldap', ...config })

  const res = await dispatch(post(url, { body }))
  const json = await res.json()

  if (res.status === 200) {
    dispatch(setProbeValue(json.probeResults.ldapQueryResults))
  }
}
