const ResultFilter = require('../result-filter.view')
const CustomElements = require('../../../js/CustomElements.js')

module.exports = ResultFilter.extend({
  className: 'is-list',
  getResultFilter: function() {
    return this.model.get('value')
  },
  removeFilter: function() {
    this.model.set('value', '')
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  saveFilter: function() {
    this.model.set('value', this.getFilter())
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
