/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
const _ = require('underscore')
const Tabs = require('../tabs')
const store = require('js/store')
const QuerySettingsView = require('component/query-settings/query-settings.view')
const QueryStatusView = require('component/query-status/query-status.view')
const QueryScheduleView = require('component/query-schedule/query-schedule.view')
const QueryEditorView = require('component/query-editor/query-editor.view')
const QueryAnnotationsView = require('component/query-annotations/query-annotations.view')

var WorkspaceContentTabs = Tabs.extend({
  defaults: {
    tabs: {
      Search: QueryEditorView,
      Settings: QuerySettingsView,
      Schedule: QueryScheduleView,
      Status: QueryStatusView,
      Annotations: QueryAnnotationsView,
    },
  },
  getAssociatedQuery: function() {
    return store.getQuery()
  },
})

module.exports = WorkspaceContentTabs
