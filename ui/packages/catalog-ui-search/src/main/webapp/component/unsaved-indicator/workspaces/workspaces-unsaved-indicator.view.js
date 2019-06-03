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

const store = require('../../../js/store.js')
const SaveView = require('../unsaved-indicator.view')

module.exports = SaveView.extend({
  attributes: {
    'data-help': 'Indicates a workspace is unsaved.',
    title: 'Indicates a workspace is unsaved.',
  },
  setDefaultModel() {
    this.model = store
  },
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenTo(
      this.model.get('workspaces'),
      'change:saved update add remove',
      this.handleSaved
    )
  },
  isSaved() {
    return !this.model.get('workspaces').find(workspace => !workspace.isSaved())
  },
})
