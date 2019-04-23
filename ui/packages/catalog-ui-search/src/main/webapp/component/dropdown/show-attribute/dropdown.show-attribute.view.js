const DropdownView = require('../dropdown.view')
const template = require('./dropdown.show-attribute.hbs')
const ShowAttributeView = require('../../show-attribute/show-attribute.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-showAttribute',
  componentToShow: ShowAttributeView,
})
