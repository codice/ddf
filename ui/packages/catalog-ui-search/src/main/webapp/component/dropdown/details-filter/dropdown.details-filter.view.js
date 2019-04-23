const DropdownView = require('../dropdown.view');
const template = require('./dropdown.details-filter.hbs');
const ComponentView = require('../../details-filter/details-filter.view.js');

module.exports = DropdownView.extend({
  template: template,
  className: 'is-detailsFilter',
  componentToShow: ComponentView,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.handleFilter()
    this.listenTo(this.model, 'change:value', this.handleFilter)
  },
  handleFilter: function() {
    const value = this.model.get('value');
    this.$el.toggleClass('has-filter', value !== undefined && value !== '')
  },
})
