const DropdownView = require('../dropdown.view')
const template = require('./dropdown.popout.hbs')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-popout',
})
