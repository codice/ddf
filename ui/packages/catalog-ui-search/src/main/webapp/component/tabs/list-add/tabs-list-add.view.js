const TabsView = require('../tabs.view')
const ListAddTabsModel = require('./tabs-list-add')

module.exports = TabsView.extend({
  className: 'is-list-add',
  setDefaultModel(options) {
    this.model = new ListAddTabsModel()
  },
  initialize(options) {
    this.setDefaultModel(options)

    TabsView.prototype.initialize.call(this)
    this.model.set('activeTab', 'Import')
  },
  determineContent() {
    const ActiveTab = this.model.getActiveView();
    if (this.model.attributes.activeTab === 'Import') {
      this.tabsContent.show(
        new ActiveTab({
          isList: true,
          extraHeaders: this.options.extraHeaders,
          url: this.options.url,
          handleUploadSuccess: this.options.handleUploadSuccess,
        })
      )
    } else {
      this.tabsContent.show(
        new ActiveTab({
          handleNewMetacard: this.options.handleNewMetacard,
          close: this.options.close,
          model: this.model,
        })
      )
    }
  },
})
