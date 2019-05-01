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

var store = require('../../../js/store.js')
var SaveView = require('../save.view')

module.exports = SaveView.extend({
  attributes: {
    'data-help': 'Saves all workspaces.',
    title: 'Saves all workspaces.',
  },
  setDefaultModel: function() {
    this.model = store
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenTo(
      this.model.get('workspaces'),
      'change:saved update add remove',
      this.handleSaved
    )
  },
  isSaved: function() {
    return !this.model.get('workspaces').find(function(workspace) {
      return !workspace.isSaved()
    })
  },
  triggerSave: function() {
    this.model.get('workspaces').saveAll()
  },
})
