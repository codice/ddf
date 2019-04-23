const DropdownView = require('../dropdown.view')
const template = require('./dropdown.workspaces-filter.hbs')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-workspacesFilter',
  getCenteringElement: function() {
    return this.el.querySelector('.dropdown-text')
  },
})
