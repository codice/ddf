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
import 'whatwg-fetch'
import { combineReducers } from 'redux-immutable'
import { fromJS, Map } from 'immutable'
import traverse from 'traverse'
import ReactDOM from 'react-dom'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import getMuiTheme from 'material-ui/styles/getMuiTheme'
import muiThemeable from 'material-ui/styles/muiThemeable'
import MenuItem from 'material-ui/MenuItem'
import IconMenu from 'material-ui/IconMenu'
import IconButton from 'material-ui/IconButton'
import ContentAddIcon from 'material-ui/svg-icons/content/add'
import admin from 'themes/admin'
import catalog from 'themes/catalog'
import FontAwesome from 'react-fontawesome'

import 'font-awesome/css/font-awesome.css'
import 'golden-layout/src/css/goldenlayout-base.css'
import 'golden-layout/src/css/goldenlayout-dark-theme.css'

window.React = React
window.ReactDOM = ReactDOM
const GoldenLayout = require('golden-layout')

const themes = {
  admin,
  catalog,
}

const Visualization = muiThemeable()(({ children, muiTheme, icon }) => (
  <div style={{ color: muiTheme.palette.primary1Color, textAlign: 'center' }}>
    <h1>{children}</h1>
    <FontAwesome name={icon} size="3x" />
  </div>
))
const configPath = ['value', 'configurations', 0, 'properties']
const baseDefault = [
  {
    type: 'stack',
    content: [
      {
        type: 'component',
        component: 'cesium',
        componentName: 'cesium',
        title: '3D Map',
      },
      {
        type: 'component',
        component: 'inspector',
        componentName: 'inspector',
        title: 'Inspector',
      },
    ],
  },
]

const select = state => state.get('layout')
const getConfig = state => select(state).get('config')
const getEditor = state => select(state).get('editor')
export const getBuffer = state => select(state).get('buffer')
export const isLoading = state => select(state).get('loading')
export const getMessage = state => select(state).get('msg')

export const hasChanges = state => {
  const buffer = getBuffer(state).get('buffer')
  const config = getConfig(state)
  try {
    const buffMap = fromJS(JSON.parse(buffer))
    const confMap = fromJS(config)
    return !confMap.equals(buffMap)
  } catch (e) {
    return false
  }
}

export const setConfig = value => ({
  type: 'default-layout/SET_CONFIG',
  value,
})
export const setBuffer = value => ({
  type: 'default-layout/SET_BUFFER',
  value,
})
export const setEditor = value => ({
  type: 'default-layout/INIT_EDITOR',
  value,
})
export const start = () => ({
  type: 'default-layout/START_SUBMIT',
})
export const end = () => ({
  type: 'default-layout/END_SUBMIT',
})
export const message = (text, action) => ({
  type: 'default-layout/MESSAGE',
  text,
  action,
})

export const validateJson = json => {
  try {
    JSON.parse(json)
    return 'valid'
  } catch (e) {
    return 'invalid'
  }
}

export const update = value => (dispatch, getState) => {
  const isValid = validateJson(value)
  if (isValid === 'valid') {
    updateLayout(value, getState)

    return dispatch({
      type: 'default-layout/UPDATE',
      value,
    })
  }
}

export const reset = () => (dispatch, getState) => {
  const config = getConfig(getState())

  updateLayout(config.get('defaultLayout'), getState)

  return dispatch({
    type: 'default-layout/RESET',
    value: config,
  })
}

export const updateLayout = (value, getState) => {
  const state = getState()
  const editor = getEditor(state)

  const settings = editor.config
  const prevSettings = settings.content
  try {
    settings.content = convertLayout(value, true)
    editor.destroy()
    editor.config = settings
    editor.init()
  } catch (e) {
    editor.destroy()
    editor.config.content = prevSettings
    editor.init()
  }
}

export const rendered = () => (dispatch, getState) => {}
export const fetch = () => (dispatch, getState) => {
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
      const config = fromJS(json).getIn(configPath)
      dispatch(setConfig(config))
      setupEditor(dispatch, getState)
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
  const buffer = getBuffer(state)

  if (validate(buffer) !== undefined) {
    return dispatch(message('Cannot save because of validation errors'))
  }
  dispatch(start())

  const layout = convertLayout(buffer.get('buffer'), false)
  let config = getConfig(state).set('defaultLayout', layout)

  const body = {
    type: 'EXEC',
    mbean:
      'org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0',
    operation: 'update',
    arguments: [
      'org.codice.ddf.catalog.ui',
      config.update('defaultLayout', JSON.stringify).toJS(),
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
      config = getConfig(state).set('defaultLayout', JSON.stringify(layout))
      dispatch(setConfig(config))
      dispatch(end())
      dispatch(message('Successfully saved default layout', 'open intrigue'))
    })
    .catch(e => {
      dispatch(end())
      dispatch(message(`Unable to save default layout: ${e.message}`))
    })
}

export const validate = buffer => {
  let error
  const conf = buffer.get('buffer')
  try {
    JSON.parse(conf)
  } catch (e) {
    error = `Invalid JSON configuration`
  }
  return error
}

export const convertLayout = (configStr, toReact) => {
  const config = JSON.parse(configStr)
  const omit = ['isClosable', 'reorderEnabled', 'activeItemIndex', 'header']
  return traverse(config).map(function(el) {
    if (this.key === 'componentName') {
      this.update(toReact ? 'lm-react-component' : this.parent.node.component)
    }
    if (omit.includes(this.key)) {
      this.remove()
    }
  })
}

const setupEditor = (dispatch, getState) => {
  const state = getState()
  const config = getConfig(state).get('defaultLayout')
  const baseConf = {
    settings: {
      showPopoutIcon: false,
      showMaximiseIcon: false,
    },
    content: convertLayout(config, true),
  }
  const visualizations = [
    {
      name: 'openlayers',
      title: '2D Map',
      icon: 'map',
    },
    {
      name: 'cesium',
      title: '3D Map',
      icon: 'globe',
    },
    {
      name: 'inspector',
      title: 'Inspector',
      icon: 'info',
    },
    {
      name: 'histogram',
      title: 'Histogram',
      icon: 'bar-chart',
    },
    {
      name: 'table',
      title: 'Table',
      icon: 'table',
    },
  ]

  let layout = new GoldenLayout(baseConf, '#layoutContainer')
  visualizations.forEach(function(component) {
    layout.registerComponent(
      component.name,
      React.createClass({
        render: function() {
          return (
            <MuiThemeProvider muiTheme={getMuiTheme(themes['admin'])}>
              <Visualization icon={component.icon}>
                {component.title}
              </Visualization>
            </MuiThemeProvider>
          )
        },
      })
    )
  })

  layout.on('initialised', function() {
    dispatch(setEditor(layout))
  })

  layout.on('stateChanged', function() {
    if (layout.isInitialised) {
      var glConf = layout.toConfig().content
      var content = JSON.stringify(glConf, null, 2)
      dispatch(setBuffer(content))
    }
  })

  layout.init()

  const LayoutOption = React.createClass({
    componentWillMount: function() {
      this.setState({
        component: {
          type: 'react-component',
          component: this.props.item.name,
          componentName: this.props.item.name,
          title: this.props.item.title,
        },
      })
    },
    componentDidMount: function() {
      layout.createDragSource(ReactDOM.findDOMNode(this), this.state.component)
    },
    handleClick: function(e) {
      if (layout.root.contentItems.length === 0) {
        layout.root.addChild({
          type: 'stack',
          content: [this.state.component],
        })
      } else {
        layout.root.contentItems[0].addChild(this.state.component)
      }
    },
    render: function() {
      return (
        <MenuItem onClick={this.handleClick}>
          <FontAwesome
            name={this.props.item.icon}
            style={{ marginRight: 10, width: 25, textAlign: 'center' }}
          />
          {this.props.item.title}
        </MenuItem>
      )
    },
  })
  const MenuList = React.createClass({
    render: function() {
      return (
        <MuiThemeProvider muiTheme={getMuiTheme(themes['admin'])}>
          <IconMenu
            iconButtonElement={
              <IconButton>
                <ContentAddIcon />
              </IconButton>
            }
            targetOrigin={{ horizontal: 'right', vertical: 'top' }}
            anchorOrigin={{ horizontal: 'right', vertical: 'top' }}
          >
            {visualizations.map(function(component, i) {
              return <LayoutOption key={i} item={component} />
            })}
          </IconMenu>
        </MuiThemeProvider>
      )
    },
  })
  ReactDOM.render(<MenuList />, document.getElementById('layoutMenu'))
}

const loading = (state = false, { type }) => {
  switch (type) {
    case 'default-layout/END_SUBMIT':
      return false
    case 'default-layout/START_SUBMIT':
      return true
    default:
      return state
  }
}

export const config = (state = Map(), { type, value }) => {
  switch (type) {
    case 'default-layout/SET_CONFIG':
      if (!value.get('defaultLayout')) {
        value = value.set('defaultLayout', JSON.stringify(baseDefault))
      }
      return value
    case 'default-layout/RESET':
      return state
    default:
      return state
  }
}

const editor = (state = Map(), { type, value = undefined }) => {
  switch (type) {
    case 'default-layout/INIT_EDITOR':
      return value
    default:
      return state
  }
}

export const buffer = (state = Map(), { type, value }) => {
  switch (type) {
    case 'default-layout/SET_BUFFER':
      const parsed = convertLayout(value, false)
      return state.set('buffer', JSON.stringify(parsed, null, 2))
    case 'default-layout/RESET':
      const defaultConf = JSON.parse(value.get('defaultLayout'))
      return state.set('buffer', JSON.stringify(defaultConf, null, 2))
    default:
      return state
  }
}

const msg = (state = {}, { type, text, action }) => {
  switch (type) {
    case 'default-layout/MESSAGE':
      return { text, action }
    default:
      return state
  }
}

export default combineReducers({ config, buffer, editor, loading, msg })
