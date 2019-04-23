const user = require('../../singletons/user-instance.js')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.login-form.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const ComponentView = require('component/login-form/login-form.view')

const getName = function(user) {
  if (user.isGuestUser()) {
    return 'Sign In'
  }

  return user.get('username')
}

module.exports = DropdownView.extend({
  template: template,
  tagName: CustomElements.register('login-dropdown'),
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    this.modelForComponent = user
    this.model.set('value', getName(this.modelForComponent.get('user')))
  },
  listenToComponent: function() {
    this.listenTo(
      this.modelForComponent,
      'change',
      function() {
        this.model.set('value', getName(this.modelForComponent.get('user')))
      }.bind(this)
    )
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
})
