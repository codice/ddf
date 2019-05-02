const DropdownView = require('../dropdown.view')
const template = require('./dropdown.search-form-selector.hbs')
const SearchFormsList = require('../../search-form-list/search-form-list.view')
const SearchFormCollection = require('../../search-form/search-form-collection-instance')
const Backbone = require('backbone')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-form-selector',
  componentToShow: SearchFormsList,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(this.model, 'change:isOpen', this.handleClose)
  },
  handleClose: function() {
    if (!this.model.get('isOpen')) {
      this.onDestroy()
      this.initializeDropdown()
    }
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = new Backbone.Model({
      currentQuery: this.options.modelForComponent,
      searchForms: SearchFormCollection.getCollection(),
    })
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
})
