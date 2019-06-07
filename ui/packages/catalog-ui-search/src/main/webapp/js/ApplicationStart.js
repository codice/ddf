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

const $ = require('jquery')
const app = require('./application.js')
const properties = require('./properties.js')
const store = require('./store.js')
const user = require('../component/singletons/user-instance.js')
require('./MediaQueries.js')
require('./Theming.js')
require('./SystemUsage.js')
require('../component/singletons/session-auto-renew.js')
require('./SessionTimeout.js')

const workspaces = store.get('workspaces')

function getWorkspacesOwnedByUser() {
  return workspaces.filter(
    workspace =>
      user.isGuest()
        ? workspace.get('localStorage') === true
        : workspace.get('metacard.owner') === user.get('user').get('email')
  )
}

function hasEmptyHashAndNoWorkspaces() {
  return getWorkspacesOwnedByUser().length === 0 && location.hash === ''
}

function checkForEmptyHashAndOneWorkspace() {
  if (
    location.hash === '' &&
    workspaces.fetched &&
    getWorkspacesOwnedByUser().length === 1
  ) {
    location.hash = '#workspaces/' + getWorkspacesOwnedByUser()[0].id
  }
}

function attemptToStart() {
  checkForEmptyHashAndOneWorkspace()
  if (workspaces.fetched && user.fetched && !hasEmptyHashAndNoWorkspaces()) {
    app.App.start({})
  } else if (!user.fetched) {
    user.once('sync', () => {
      attemptToStart()
    })
  } else if (!workspaces.fetched) {
    workspaces.once('sync', () => {
      attemptToStart()
    })
  } else if (hasEmptyHashAndNoWorkspaces()) {
    workspaces.once('sync', (workspace, resp, options) => {
      location.hash = '#workspaces/' + workspace.id
      attemptToStart()
    })
    workspaces.createWorkspace()
  }
}

//$(window).trigger('resize');
$(window.document).ready(() => {
  window.document.title = properties.branding + ' ' + properties.product
})
attemptToStart()
