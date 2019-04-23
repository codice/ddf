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
  getAssociatedQuery: function() {
    return store.getQuery()
  },
})

module.exports = WorkspaceContentTabs
