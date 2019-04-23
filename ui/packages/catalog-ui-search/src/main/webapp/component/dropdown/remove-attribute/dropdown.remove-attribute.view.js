const DropdownView = require('../dropdown.view')
const template = require('./dropdown.remove-attribute.hbs')
const RemoveAttributeView = require('../../remove-attribute/remove-attribute.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-removeAttribute',
  componentToShow: RemoveAttributeView,
})
