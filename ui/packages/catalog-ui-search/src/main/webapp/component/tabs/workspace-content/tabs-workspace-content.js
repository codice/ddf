const Tabs = require('../tabs')
const WorkspaceSearchView = require('../../workspace-search/workspace-search.view.js')
const WorkspaceListsView = require('../../workspace-lists/workspace-lists.view.js')

const WorkspaceContentTabs = Tabs.extend({
  defaults: {
    tabs: {
      Search: WorkspaceSearchView,
      Lists: WorkspaceListsView,
    },
  },
})

module.exports = WorkspaceContentTabs
