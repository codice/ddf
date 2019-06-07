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

const Tabs = require('../tabs')
const store = require('../../../js/store.js')
const QuerySettingsView = require('../../query-settings/query-settings.view.js')
const QueryStatusView = require('../../query-status/query-status.view.js')
const QueryScheduleView = require('../../query-schedule/query-schedule.view.js')
const QueryEditorView = require('../../query-editor/query-editor.view.js')
const QueryAnnotationsView = require('../../query-annotations/query-annotations.view.js')

const WorkspaceContentTabs = Tabs.extend({
  defaults: {
    tabs: {
      Search: QueryEditorView,
      Settings: QuerySettingsView,
      Schedule: QueryScheduleView,
      Status: QueryStatusView,
      Annotations: QueryAnnotationsView,
    },
  },
  getAssociatedQuery() {
    return store.getQuery()
  },
})

module.exports = WorkspaceContentTabs
