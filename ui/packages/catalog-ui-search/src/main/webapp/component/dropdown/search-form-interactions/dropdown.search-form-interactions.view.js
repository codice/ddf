const DropdownView = require('../dropdown.view')
const template = require('./dropdown.search-form-interactions.hbs')
const SearchFormInteractionsView = require('../../search-form-interactions/search-form-interactions.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-form-interactions',
  componentToShow: SearchFormInteractionsView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
})
