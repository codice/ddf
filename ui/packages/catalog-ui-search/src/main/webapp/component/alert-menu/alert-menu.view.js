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

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const $ = require('jquery')
const template = require('./alert-menu.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const alertInstance = require('../alert/alert.js')
const Common = require('../../js/Common.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('alert-menu'),
  events: {
    'click > .workspace-title': 'goToWorkspace',
  },
  onFirstRender() {
    this.listenTo(alertInstance, 'change:currentAlert', this.render)
  },
  goToWorkspace(e) {
    const workspaceId = $(e.currentTarget).attr('data-workspaceid')
    wreqr.vent.trigger('router:navigate', {
      fragment: 'workspaces/' + workspaceId,
      options: {
        trigger: true,
      },
    })
  },
  serializeData() {
    if (alertInstance.get('currentAlert') === undefined) {
      return {}
    }
    const alertJSON = alertInstance.get('currentAlert').toJSON()
    const workspace = store
      .get('workspaces')
      .filter(workspace => workspace.get('queries').get(alertJSON.queryId))[0]
    let query
    if (workspace) {
      query = workspace.get('queries').get(alertJSON.queryId)
    }
    return {
      amount: alertJSON.metacardIds.length,
      when: Common.getMomentDate(alertJSON.when),
      query: query ? query.toJSON() : undefined,
      workspace: workspace ? workspace.toJSON() : undefined,
    }
  },
})
