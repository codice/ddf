const DropdownView = require('../dropdown.view')
const template = require('./dropdown.result-filter.hbs')
const ComponentView = require('../../result-filter/result-filter.view.js')
const user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  attributes: {
    'data-help':
      'Used to setup a local filter of a result set.  It does not re-execute the search.',
  },
  template: template,
  className: 'is-resultFilter',
  componentToShow: ComponentView,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultFilter',
      this.handleFilter
    )
    this.handleFilter()
  },
  handleFilter: function() {
    const resultFilter = user
      .get('user')
      .get('preferences')
      .get('resultFilter');
    this.$el.toggleClass('has-filter', Boolean(resultFilter))
  },
})
