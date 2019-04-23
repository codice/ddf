const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./session-timeout.hbs')
const sessionTimeoutModel = require('../singletons/session-timeout.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('session-timeout'),
  model: null,
  events: {
    'click button': 'renewSession',
  },
  initialize: function(options) {},
  onRender: function() {
    setTimeout(this.refreshTimeLeft.bind(this), 1000)
  },
  refreshTimeLeft: function() {
    if (!this.isDestroyed) {
      this.render()
    }
  },
  serializeData: function() {
    return {
      timeLeft: sessionTimeoutModel.getIdleSeconds(),
    }
  },
  renewSession: function() {
    sessionTimeoutModel.renew()
  },
})
