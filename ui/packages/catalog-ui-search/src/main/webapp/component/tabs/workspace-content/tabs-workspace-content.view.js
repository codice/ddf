const TabsView = require('../tabs.view')
const store = require('../../../js/store.js')

const WorkspaceContentTabsView = TabsView.extend({
  initialize: function() {
    TabsView.prototype.initialize.call(this)
    this.listenTo(
      this.options.selectionInterface,
      'change:currentQuery',
      this.handleQuery
    )
  },
  closePanelTwo: function() {
    switch (this.model.get('activeTab')) {
      case 'Searches':
        this.options.selectionInterface.setCurrentQuery(undefined)
        this.options.selectionInterface.setActiveSearchResults([])
        this.options.selectionInterface.clearSelectedResults()
        break
      default:
        store.get('content').set('query', undefined)
        this.options.selectionInterface.setCurrentQuery(undefined)
        this.options.selectionInterface.setActiveSearchResults([])
        this.options.selectionInterface.clearSelectedResults()
    }
  },
  determineContent: function() {
    const activeTab = this.model.getActiveView()
    this.tabsContent.show(
      new activeTab({
        selectionInterface: store,
      })
    )
  },
  onDestroy: function() {
    this.closePanelTwo()
  },
  handleQuery: function() {
    if (
      store.getCurrentQuery() !== undefined &&
      store.getCurrentQueries().get(store.getCurrentQuery()) !== undefined
    ) {
      this.model.set('activeTab', 'Search')
    }
  },
})

module.exports = WorkspaceContentTabsView
