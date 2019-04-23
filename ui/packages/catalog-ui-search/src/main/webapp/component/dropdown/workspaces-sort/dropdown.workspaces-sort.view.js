const DropdownView = require('../dropdown.view')
const template = require('./dropdown.workspaces-sort.hbs')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-workspacesSort',
  getCenteringElement: function() {
    return this.el
  },
})
