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
import React from 'react'
import { render, unmountComponentAtNode, findDOMNode } from 'react-dom'

import Monitor from './monitor'

import { Map } from 'immutable'
import { v4 } from 'uuid'

import { createStore, combineReducers } from 'redux'

const get = (action, key) => {
  if (typeof action[key] === 'function') {
    return action[key]()
  }
  return action[key]
}

const status = (state = false, action) => {
  switch (action.type) {
    case 'STARTED':
      return true
    default:
      return state
  }
}

const createdActions = (state = Map(), { type, action }) => {
  switch (type) {
    case 'CREATE':
      return state.set(action.type, action)
    default:
      return state
  }
}

const registeredActions = (state = Map(), action) => {
  switch (action.type) {
    case 'REGISTER':
      return state.set(action.key, action.value)
    case 'UNREGISTER':
      return state.delete(action.key)
    default:
      return state
  }
}

const rootReducer = combineReducers({
  status,
  definitions: createdActions,
  actions: registeredActions,
})

let store = createStore(
  rootReducer,
  window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__()
)

const sleep = time => new Promise(resolve => setTimeout(resolve, time))

const visibleActions = store => {
  return store
    .getState()
    .actions.toList()
    .filter(action => {
      const el = get(action, 'el')
      if (el !== undefined) {
        return isVisible(el)
      }
      return true
    })
}

const getActions = () => {
  return visibleActions(store)
    .groupBy(action => action.type)
    .map((actions, type) => {
      const params = actions.reduce(
        (params, action) => params.concat(get(action, 'params')),
        []
      )
      const { docs } = store.getState().definitions.get(type)
      return { type, docs, params }
    })
    .valueSeq()
    .toJS()
}

const getAction = (store, type, params) => {
  const actions = visibleActions(store)
    .filter(action => action.type === type)
    .map(action => ({ ...action, meta: get(action, 'meta') }))

  if (actions.size < 2) {
    return actions.get(0)
  }

  const { selector } = store.getState().definitions.get(type)

  if (typeof selector !== 'function') {
    return actions.get(0)
  }

  return selector(actions, params)
}

const dispatch = async action => {
  store.dispatch({ type: 'DISPATCH', value: action })
  const { type, ...args } = action
  const dispatchTime = Date.now()
  for (let i = 0; i < 60; i++) {
    const resolvedAction = getAction(store, type, args)

    if (resolvedAction === undefined) {
      console.log('waiting for', type)
      await sleep(1000)
      continue
    }

    const { fn } = resolvedAction

    if (typeof fn === 'function') {
      const startTime = Date.now()
      const result = await fn(args)
      const endTime = Date.now()

      return {
        result,
        dispatchTime,
        startTime,
        endTime,
      }
    }
  }

  throw new Error(`Unknown action type: ${type}`)
}

export default () => {
  store.dispatch({ type: 'STARTED' })
  const api = { dispatch, getActions }
  window.api = api
  if (typeof parent.onStart === 'function') {
    parent.onStart(api)
  }
}

const register = action => {
  const key = v4()

  store.dispatch({
    type: 'REGISTER',
    key,
    value: action,
  })

  return key
}

const unregister = key => {
  store.dispatch({ type: 'UNREGISTER', key })
}

const isCssHidden = el => {
  if (el === null) {
    return false
  }

  const style = getComputedStyle(el)

  return style.display === 'none' || style.opacity === '0'
    ? true
    : isCssHidden(el.parentElement)
}

const isInViewport = el => {
  const width = window.innerWidth || document.documentElement.clientWidth
  const height = window.innerHeight || document.documentElement.clientHeight
  const { right, bottom, left, top } = el.getBoundingClientRect()
  return !(right < 0 || bottom < 0 || left > width || top > height)
}

const isCovered = el => {
  const { x, y, width, height } = el.getBoundingClientRect()
  const node = document.elementFromPoint(x + width / 2, y + height / 2)
  return !el.contains(node)
}

const isVisible = el => {
  return !isCssHidden(el) && isInViewport(el) && !isCovered(el)
}

const withAction = ({
  type,
  fn,
  meta = () => {},
  params = [],
  ...args
}) => Component =>
  class WithAction extends React.Component {
    constructor(props) {
      super(props)
      this.ref = React.createRef()
    }
    componentWillMount() {
      this.action = register({
        type,
        ...args,
        params: () => {
          if (typeof params === 'function') {
            return params(this.props)
          }
          return params
        },
        el: () => {
          if (this.ref.current !== null) {
            return findDOMNode(this.ref.current)
          }
        },
        meta: () => meta(this.props),
        fn: args =>
          fn({
            ...args,
            props: this.props,
          }),
      })
    }
    componentWillUnmount() {
      unregister(this.action)
    }
    render() {
      return <Component ref={this.ref} {...this.props} {...this.state} />
    }
  }

export const createAction = action => {
  store.dispatch({ type: 'CREATE', action })
  const { type, docs } = action
  return {
    register(args) {
      return register({ type, ...args })
    },
    unregister(action) {
      return unregister(action)
    },
    withAction(args) {
      return withAction({ type, ...args })
    },
  }
}

export const setup = (url, done) => {
  const iframeRoot = document.createElement('div')
  const monitorRoot = document.createElement('div')
  document.body.insertBefore(iframeRoot, document.body.firstChild)
  document.body.insertBefore(monitorRoot, document.body.firstChild)

  const cleanup = () => {
    unmountComponentAtNode(iframeRoot)
    unmountComponentAtNode(monitorRoot)
    iframeRoot.remove()
    monitorRoot.remove()
    delete window.onStart
  }

  window.onStart = api => {
    render(<Monitor api={api} />, monitorRoot)
    done({ ...api, cleanup })
  }

  render(
    <iframe
      src={url}
      style={{
        border: 0,
        top: 0,
        left: 0,
        width: 'calc(100vw - 480px)',
        height: '100vh',
        zIndex: 1000,
        position: 'absolute',
      }}
    />,
    iframeRoot
  )
}
