define([
    'marionette',
    '../tabs',
    '../tabs.view'
], function (Marionette, Tabs, TabsView) {

  var SimpleTab = TabsView.extend({
    initialize: function () {
      var o = {}
      this.options.tabs.forEach(function (tab) {
        o[tab] = true
      })
      this.model = new Tabs({ tabs: o })
        TabsView.prototype.initialize.call(this)
    },
    determineContent: function () {
      var c = this.options.getContent(this.model.getActiveTab())
      this.tabsContent.show(c)
    }
  })

  return SimpleTab
})
