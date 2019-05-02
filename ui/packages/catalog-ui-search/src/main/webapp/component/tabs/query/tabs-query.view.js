const TabsView = require('../tabs.view')
const QueryTabsModel = require('./tabs-query')
const store = require('../../../js/store.js')

const QueryTabsView = TabsView.extend({
  className: 'is-query',
  setDefaultModel: function() {
    this.model = new QueryTabsModel()
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.listenTo(store.get('content'), 'change:query', this.handleQuery)
    this.determineAvailableContent()
    TabsView.prototype.initialize.call(this)
  },
  handleQuery: function() {
    this.determineAvailableContent()
    this.determineContent()
  },
  determineTabForExistingQuery: function() {
    const activeTab = this.model.getActiveView()
    this.tabsContent.show(
      new activeTab({
        model: this.model.getAssociatedQuery(),
      })
    )
  },
  determineTabForNewQuery: function() {
    const activeTabName = this.model.get('activeTab')
    if (activeTabName !== 'Search') {
      this.model.set('activeTab', 'Search')
    }
    this.determineTabForExistingQuery()
  },
  determineContent: function() {
    const currentQuery = store.get('content').get('query')
    if (currentQuery) {
      if (currentQuery._cloneOf) {
        this.determineTabForExistingQuery()
      } else {
        this.determineTabForNewQuery()
      }
    }
  },
  determineAvailableContent: function() {
    const currentQuery = store.get('content').get('query')
    this.$el.toggleClass('is-new', currentQuery && !currentQuery._cloneOf)
  },
})

module.exports = QueryTabsView
