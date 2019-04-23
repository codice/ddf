const DropdownView = require('../dropdown.view')
const template = require('./dropdown.hide-attribute.hbs')
const HideAttributeView = require('../../hide-attribute/hide-attribute.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-hideAttribute',
  componentToShow: HideAttributeView,
})
