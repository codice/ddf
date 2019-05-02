const DropdownView = require('../dropdown.view')
const template = require('./dropdown.details-interactions.hbs')
const ComponentView = require('../../details-interactions/details-interactions.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-detailsInteractions',
  componentToShow: ComponentView,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
  },
})
