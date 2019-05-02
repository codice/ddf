const _ = require('underscore')
const TabsView = require('../tabs.view')
const MetacardTabsModel = require('./tabs-metacard')
const store = require('../../../js/store.js')
const properties = require('../../../js/properties.js')

module.exports = TabsView.extend({
  className: 'is-metacard',
  setDefaultModel: function() {
    this.model = new MetacardTabsModel()
  },
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface = options.selectionInterface || store
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.determineDisabledContent()
    this.determineAvailableContent()
    TabsView.prototype.initialize.call(this)
    const debounceDetermineContent = _.debounce(this.handleMetacardChange, 200)
    const throttleDetermineContent = _.throttle(this.handleMetacardChange, 200)
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'update',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'add',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'remove',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'reset',
      debounceDetermineContent
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'refreshdata',
      throttleDetermineContent
    )
  },
  handleMetacardChange: function() {
    this.determineAvailableContent()
    this.determineContent()
  },
  determineContentFromType: function() {
    const activeTabName = this.model.get('activeTab')
    const result = this.selectionInterface.getSelectedResults().first()
    if (
      result.isRevision() &&
      ['History', 'Actions', 'Overwrite', 'Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Summary')
    } else if (
      result.isDeleted() &&
      ['History', 'Actions', 'Overwrite'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Summary')
    } else if (
      result.isWorkspace() &&
      ['History', 'Actions', 'Overwrite', 'Archive'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Summary')
    }
    if (
      result.isRemote() &&
      ['History', 'Associations', 'Quality', 'Archive', 'Overwrite'].indexOf(
        activeTabName
      ) >= 0
    ) {
      this.model.set('activeTab', 'Summary')
    }
    if (
      properties.isEditingRestricted() &&
      ['Archive', 'Overwrite'].indexOf(activeTabName) >= 0
    ) {
      this.model.set('activeTab', 'Summary')
    }
    if (!result.hasPreview() && ['Preview'].indexOf(activeTabName) >= 0) {
      this.model.set('activeTab', 'Summary')
    }
    const activeTab = this.model.getActiveView()
    if (activeTab) {
      this.tabsContent.show(
        new activeTab({
          selectionInterface: this.selectionInterface,
        })
      )
    }
  },
  determineContent: function() {
    if (this.selectionInterface.getSelectedResults().length === 1) {
      this.determineContentFromType()
    }
  },
  determineAvailableContent: function() {
    if (this.selectionInterface.getSelectedResults().length === 1) {
      const result = this.selectionInterface.getSelectedResults().first()
      this.$el.toggleClass('is-workspace', result.isWorkspace())
      this.$el.toggleClass('is-resource', result.isResource())
      this.$el.toggleClass('is-revision', result.isRevision())
      this.$el.toggleClass('is-deleted', result.isDeleted())
      this.$el.toggleClass('is-remote', result.isRemote())
      this.$el.toggleClass('lacks-preview', !result.hasPreview())
    }
  },
  determineDisabledContent: function() {
    this.$el.toggleClass(
      'is-editing-disabled',
      properties.isEditingRestricted()
    )
    this.$el.toggleClass(
      'is-preview-disabled',
      !properties.isMetacardPreviewEnabled()
    )
  },
})
