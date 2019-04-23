const Marionette = require('marionette')
const template = require('./workspace-details.hbs')
const CustomElements = require('../../js/CustomElements.js')
const moment = require('moment')
const user = require('../singletons/user-instance.js')
const UnsavedIndicatorView = require('../unsaved-indicator/workspace/workspace-unsaved-indicator.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('workspace-details'),
  regions: {
    unsavedIndicator: '.title-indicator',
  },
  initialize: function(options) {
    this.listenTo(
      user.get('user').get('preferences'),
      'change:homeDisplay',
      this.handleDisplayPref
    )
  },
  onRender: function() {
    this.handleDisplayPref()
  },
  onBeforeShow: function() {
    this.unsavedIndicator.show(
      new UnsavedIndicatorView({
        model: this.model,
      })
    )
  },
  handleDisplayPref: function() {
    this.$el.toggleClass(
      'as-list',
      user
        .get('user')
        .get('preferences')
        .get('homeDisplay') === 'List'
    )
  },
  serializeData: function() {
    const workspacesJSON = this.model.toJSON()
    workspacesJSON.niceDate = moment(
      workspacesJSON['metacard.modified']
    ).fromNow()
    workspacesJSON.owner = workspacesJSON['metacard.owner'] || 'Guest'
    return workspacesJSON
  },
})
