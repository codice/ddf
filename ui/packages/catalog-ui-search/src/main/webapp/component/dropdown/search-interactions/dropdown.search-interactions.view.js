const DropdownView = require('../dropdown.view')
const template = require('./dropdown.search-interactions.hbs')
const ComponentView = require('../../search-interactions/search-interactions.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-interactions',
  componentToShow: ComponentView,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.handleSchedule()
    this.listenTo(this.options.modelForComponent, 'change', this.handleSchedule)
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  handleSchedule: function() {
    this.$el.toggleClass(
      'is-polling',
      this.options.modelForComponent.get('polling')
    )
  },
})
