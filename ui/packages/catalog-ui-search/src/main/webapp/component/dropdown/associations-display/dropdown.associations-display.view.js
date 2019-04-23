const DropdownView = require('../dropdown.view');
const template = require('./dropdown.associations-display.hbs');

module.exports = DropdownView.extend({
  template: template,
  className: 'is-associationsDisplay',
  serializeData: function() {
    const modelJSON = DropdownView.prototype.serializeData.call(this);
    modelJSON.icon =
      modelJSON.concatenatedLabel === 'Graph' ? 'fa-sitemap' : 'fa-th-list'
    return modelJSON
  },
})
