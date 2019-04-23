const DropdownView = require('../dropdown.view');
const template = require('./dropdown.attributes-rearrange.hbs');
const ComponentView = require('../../attributes-rearrange/attributes-rearrange.view.js');

module.exports = DropdownView.extend({
  template: template,
  className: 'is-attributesRearrange',
  componentToShow: ComponentView,
})
