const DropdownView = require('../dropdown.view')
const template = require('./dropdown.filter-comparator.hbs')
const ComponentView = require('../../filter-comparator/filter-comparator.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-filterComparator',
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
    this.listenTo(
      this.modelForComponent,
      'change:comparator',
      function() {
        this.model.set('value', this.modelForComponent.get('comparator'))
      }.bind(this)
    )
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
})
