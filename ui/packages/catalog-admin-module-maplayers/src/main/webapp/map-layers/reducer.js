/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import 'whatwg-fetch'
import { combineReducers } from 'redux-immutable'
import { fromJS, List, Map } from 'immutable'
import isURL from 'validator/lib/isURL'
import matches from 'validator/lib/matches'

const select = state => state.get('layers')

import options from './options'

const getConfig = state => select(state).get('config')
export const getProviders = state => select(state).get('providers')
export const isLoading = state => select(state).get('loading')
export const hasChanges = state => {
  const providers = getProviders(state).map(layer => layer.get('layer'))
  const config = getConfig(state)
  return !providers.equals(config.get('imageryProviders'))
}
export const getMessage = state => select(state).get('msg')
export const getInvalid = state => select(state).get('invalid')

export const set = value => ({ type: 'map-layers/SET', value })
const start = () => ({ type: 'map-layers/START_SUBMIT' })
const end = () => ({ type: 'map-layers/END_SUBMIT' })
export const setInvalid = buffer => ({
  type: 'map-layers/SET_INVALID',
  buffer,
})
export const message = (text, action) => ({
  type: 'map-layers/MESSAGE',
  text,
  action,
})
export const update = (value, path) => ({
  type: 'map-layers/UPDATE',
  value,
  path,
})
export const reset = () => (dispatch, getState) =>
  dispatch({
    type: 'map-layers/SET',
    value: getConfig(getState()),
  })

const configPath = ['value', 'configurations', 0, 'properties']

export const fetch = () => dispatch => {
  dispatch(start())

  const url = [
    '..',
    'jolokia',
    'exec',
    'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
    'getService',
    '(service.pid=org.codice.ddf.catalog.ui)',
  ].join('/')

  window
    .fetch(url, {
      credentials: 'same-origin',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
      },
    })
    .then(res => res.json())
    .then(json => {
      const config = fromJS(json)
        .getIn(configPath)
        .update('imageryProviders', providers => {
          if (providers === undefined || providers === '') {
            return fromJS([])
          }
          try {
            const parsed = JSON.parse(providers)
            const err = validateStructure(parsed)
            if (err !== undefined) {
              throw Error(err)
            }
            return fromJS(parsed)
          } catch (e) {
            dispatch(setInvalid(providers))
            dispatch(message(`Existing map layers are invalid: ${e.message}`))
            return fromJS([])
          }
        })
      dispatch(set(config))
      dispatch(end())
    })
    .catch(e => {
      dispatch(end())
      dispatch(message(`Unable to retrieve map layers: ${e.message}`))
    })
}

export const save = () => (dispatch, getState) => {
  const state = getState()
  const url =
    '../jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/add'

  const providers = getProviders(state)

  if (!validate(providers).isEmpty()) {
    return dispatch(message('Cannot save because of validation errors'))
  }

  dispatch(start())

  const config = getConfig(state).set(
    'imageryProviders',
    providers.map(provider => provider.get('layer'))
  )

  const body = {
    type: 'EXEC',
    mbean:
      'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
    operation: 'update',
    arguments: [
      'org.codice.ddf.catalog.ui',
      config.update('imageryProviders', JSON.stringify).toJS(),
    ],
  }

  const opts = {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
    },
    body: JSON.stringify(body),
  }

  window
    .fetch(url, opts)
    .then(res => res.text())
    .then(body => {
      const res = JSON.parse(
        body.replace(
          /(\[Ljava\.lang\.(Long|String);@[^,]+)/g,
          (_, str) => `"${str}"`
        )
      )
      if (res.status !== 200) {
        throw new Error(res.error)
      }
      dispatch(set(config))
      dispatch(end())
      dispatch(message('Successfully saved map layers', 'open intrigue'))
    })
    .catch(e => {
      dispatch(end())
      dispatch(message(`Unable to save map layers: ${e.message}`))
    })
}

export const validateJson = json => {
  try {
    JSON.parse(json)
    return undefined
  } catch (e) {
    return 'Invalid JSON configuration'
  }
}

export const validateStructure = providers => {
  if (!Array.isArray(providers)) {
    return 'Providers should be an array'
  }

  if (providers.some(obj => Array.isArray(obj) || typeof obj !== 'object')) {
    return 'All provider entries must be objects'
  }
}

export const validate = providers => {
  let errors = List()

  providers.forEach((provider, i) => {
    const layer = provider.get('layer')

    const name = layer.get('name')
    const existing = providers.findIndex(function(o, q) {
      return o.get('layer').get('name') === name && q !== i
    })

    if (name === '' || name === undefined) {
      errors = errors.setIn([i, 'name'], 'Name cannot be empty')
    } else if (!matches(name, '^[\\w\\-\\s]+$')) {
      errors = errors.setIn([i, 'name'], 'Name must be alphanumeric')
    } else if (existing < i && existing !== -1) {
      errors = errors.setIn(
        [i, 'name'],
        'Name is already in use and must be unique'
      )
    }

    const alpha = layer.get('alpha')

    if (alpha === '') {
      errors = errors.setIn([i, 'alpha'], 'Alpha cannot be empty')
    } else if (typeof alpha !== 'number') {
      errors = errors.setIn([i, 'alpha'], 'Alpha must be a number')
    } else if (alpha < 0) {
      errors = errors.setIn([i, 'alpha'], 'Alpha too small')
    } else if (alpha > 1) {
      errors = errors.setIn([i, 'alpha'], 'Alpha too large')
    }

    const proxyEnabled = layer.get('proxyEnabled')

    if (typeof proxyEnabled !== 'boolean') {
      errors = errors.setIn(
        [i, 'proxyEnabled'],
        'Proxy enabled must be true or false'
      )
    }

    const show = layer.get('show')

    if (typeof show !== 'boolean') {
      errors = errors.setIn([i, 'show'], 'Show must be true or false')
    }

    const type = layer.get('type')

    if (type === '') {
      errors = errors.setIn([i, 'type'], 'Type cannot be empty')
    } else if (!Object.keys(options).includes(type)) {
      errors = errors.setIn([i, 'type'], `Invalid type: ${type}`)
    }

    const url = layer.get('url')

    const opts = {
      protocols: ['http', 'https'],
      require_protocol: true,
    }

    if (url === '') {
      errors = errors.setIn([i, 'url'], 'URL cannot be empty')
    } else if (typeof url !== 'string') {
      errors = errors.setIn([i, 'url'], 'URL must be a string')
    } else if (!isURL(url, opts)) {
      errors = errors.setIn([i, 'url'], 'Invalid URL')
    }

    const err = validateJson(provider.get('buffer'))

    if (err !== undefined) {
      errors = errors.setIn([i, 'buffer'], err)
    }

    const order = layer.get('order')

    if (order === '' || order === undefined) {
      errors = errors.setIn([i, 'order'], 'Order cannot be empty')
    } else if (typeof order !== 'number') {
      errors = errors.setIn([i, 'order'], 'Order must be a number')
    } else if (order < 0 || order > providers.size - 1) {
      errors = errors.setIn(
        [i, 'order'],
        `Order should be between 0 and ${providers.size - 1}`
      )
    } else {
      const previous = providers
        .slice(0, i)
        .find(provider => order === provider.getIn(['layer', 'order']))
      if (previous !== undefined) {
        errors = errors.setIn(
          [i, 'order'],
          `Order ${order} previously used for ${previous.getIn([
            'layer',
            'name',
          ])}`
        )
      }
    }

    const redirects = layer.get('withCredentials')

    if (typeof redirects !== 'boolean') {
      errors = errors.setIn(
        [i, 'withCredentials'],
        'With credentials must be true or false'
      )
    }
  })

  return errors
}

const emptyProvider = index => {
  const layer = {
    name: '',
    url: '',
    type: '',
    alpha: '',
    proxyEnabled: true,
    withCredentials: false,
    show: true,
    parameters: {
      transparent: false,
      format: '',
    },
    order: index,
  }
  const buffer = JSON.stringify(layer, null, 2)
  return fromJS({ buffer, layer })
}

const applyDefaults = (previousType = '') => provider => {
  const layer = provider.get('layer')

  if (previousType === '' && previousType !== layer.get('type')) {
    const opts = options[layer.get('type')]
    const defaults = fromJS({ layer: opts !== undefined ? opts.config : {} })
    return defaults.mergeDeep(provider)
  }

  return provider
}

const updateLayerFromBuffer = provider => {
  try {
    const layer = fromJS(JSON.parse(provider.get('buffer')))
    return provider.set('layer', layer)
  } catch (e) {
    return provider
  }
}

const updateBufferFromLayer = provider => {
  const layer = provider.get('layer')
  const buffer = JSON.stringify(layer, null, 2)
  return provider.set('buffer', buffer)
}

const providers = (
  state = List(),
  { type, path, value = emptyProvider(state.size) }
) => {
  switch (type) {
    case 'map-layers/SET':
      return value
        .get('imageryProviders')
        .map(layer => fromJS({ layer, buffer: JSON.stringify(layer, null, 2) }))
    case 'map-layers/UPDATE':
      const [index] = path

      if (value === null) {
        return state
          .remove(index)
          .map((layer, i) =>
            layer.setIn(['layer', 'order'], i).update(updateBufferFromLayer)
          )
      }

      const previousType = state.getIn([index, 'layer', 'type'])

      const updater = path.includes('buffer')
        ? updateLayerFromBuffer
        : updateBufferFromLayer

      return state
        .setIn(path, fromJS(value))
        .update(index, applyDefaults(previousType))
        .update(index, updater)
        .sortBy(provider => provider.getIn(['layer', 'order']))
    default:
      return state
  }
}

const loading = (state = false, { type }) => {
  switch (type) {
    case 'map-layers/END_SUBMIT':
      return false
    case 'map-layers/START_SUBMIT':
      return true
    default:
      return state
  }
}

const config = (state = Map(), { type, value }) => {
  switch (type) {
    case 'map-layers/SET':
      return value.mergeDeep(value)
    default:
      return state
  }
}

const msg = (state = {}, { type, text, action }) => {
  switch (type) {
    case 'map-layers/MESSAGE':
      return { text, action }
    default:
      return state
  }
}

const invalid = (state = null, { type, buffer }) => {
  switch (type) {
    case 'map-layers/SET_INVALID':
      return buffer
    default:
      return state
  }
}

export default combineReducers({ config, providers, loading, msg, invalid })
