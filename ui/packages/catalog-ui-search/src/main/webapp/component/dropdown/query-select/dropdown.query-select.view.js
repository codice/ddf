const DropdownView = require('../dropdown.view')
const template = require('./dropdown.query-select.hbs')
const QueryItemView = require('../../query-item/query-item.view.js')
const SearchSelectView = require('../../search-select/search-select.view.js')
const store = require('../../../js/store.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-querySelect',
  componentToShow: SearchSelectView,
  regions: {
    queryItem: '.querySelect-item',
  },
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(store.getCurrentQueries(), 'remove', this.handleRemoveQuery)
    this.handleHideActions()
  },
  handleHideActions: function() {
    this.$el.toggleClass('hide-actions', this.options.hideActions === true)
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.model
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
  handleRemoveQuery: function(removedQuery) {
    if (removedQuery.id === this.model.get('value')) {
      this.model.set('value', undefined)
    }
  },
  onRender: function() {
    DropdownView.prototype.onRender.call(this)
    const queryId = this.model.get('value')
    if (queryId) {
      this.queryItem.show(
        new QueryItemView({
          model: store.getCurrentQueries().get(queryId),
        })
      )
      this.$el.addClass('query-selected')
    } else {
      this.$el.removeClass('query-selected')
    }
  },
})
