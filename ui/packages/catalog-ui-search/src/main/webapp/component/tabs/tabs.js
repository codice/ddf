const Backbone = require('backbone')
const Tabs = Backbone.Model.extend({
  defaults: {
    tabs: {},
    tabsOptions: {},
    activeTab: undefined,
  },
  initialize: function() {
    this.setDefaultActiveTab()
  },
  setDefaultActiveTab: function() {
    const tabs = this.get('tabs');
    if (Object.keys(tabs).length > 0 && !this.getActiveTab()) {
      this.set('activeTab', Object.keys(tabs)[0])
    }
  },
  setActiveTab: function(tab) {
    this.set('activeTab', tab)
  },
  getActiveTab: function() {
    return this.get('activeTab')
  },
  getActiveView: function() {
    return this.get('tabs')[this.getActiveTab()]
  },
  getActiveViewOptions: function() {
    if (this.get('tabsOptions')) {
      return this.get('tabsOptions')[this.getActiveTab()]
    }
  },
});

module.exports = Tabs
