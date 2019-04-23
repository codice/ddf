const DropdownView = require('../dropdown.view')
const template = require('./dropdown.uploads.hbs')
const ComponentView = require('../../uploads/uploads.view.js')
const user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-uploads is-button',
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = user
      .get('user')
      .get('preferences')
      .get('uploads')
    this.handleUploads()
  },
  listenToComponent: function() {
    this.listenTo(
      this.modelForComponent,
      'add remove reset',
      this.handleUploads
    )
  },
  handleUploads: function() {
    this.$el.toggleClass('has-uploads', this.modelForComponent.length > 0)
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
