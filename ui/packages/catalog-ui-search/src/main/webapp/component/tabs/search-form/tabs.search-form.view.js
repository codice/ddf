const TabsView = require('../tabs.view')
const SearchFormTabsModel = require('./tabs.search-form')
const store = require('../../../js/store.js')
const Query = require('../../../js/model/Query.js')

module.exports = TabsView.extend({
  selectionInterface: store,
  setDefaultModel: function() {
    this.model = new SearchFormTabsModel()
  },
  initialize: function() {
    this.setDefaultModel()
    TabsView.prototype.initialize.call(this)
  },
  determineContent: function() {
    const activeTab = this.model.getActiveView();
    this.tabsContent.show(
      new activeTab({
        model: this.options.queryModel || new Query.Model(),
      })
    )
  },
})
