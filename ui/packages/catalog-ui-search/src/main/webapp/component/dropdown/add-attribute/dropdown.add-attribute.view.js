const DropdownView = require('../dropdown.view')
const template = require('./dropdown.add-attribute.hbs')
const AddAttributeView = require('../../add-attribute/add-attribute.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-addAttribute',
  componentToShow: AddAttributeView,
})
