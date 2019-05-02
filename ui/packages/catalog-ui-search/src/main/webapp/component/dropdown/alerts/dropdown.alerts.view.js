const DropdownView = require('../dropdown.view')
const template = require('./dropdown.alerts.hbs')
const ComponentView = require('../../alerts/alerts.view.js')
const user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-alerts is-button',
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = user
      .get('user')
      .get('preferences')
      .get('alerts')
    this.handleAlerts()
  },
  listenToComponent: function() {
    this.listenTo(this.modelForComponent, 'add remove reset', this.handleAlerts)
  },
  handleAlerts: function() {
    this.$el.toggleClass('has-alerts', this.modelForComponent.length > 0)
  },
  serializeData: function() {
    return this.modelForComponent.toJSON()
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el.querySelector('.notification-icon')
  },
  hasTail: true,
})
