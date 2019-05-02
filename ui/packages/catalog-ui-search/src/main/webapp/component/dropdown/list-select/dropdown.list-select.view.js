const DropdownView = require('../dropdown.view')
const template = require('./dropdown.list-select.hbs')
const ListItemView = require('../../list-item/list-item.view.js')
const ListSelectView = require('../../list-select/list-select.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-listSelect',
  componentToShow: ListSelectView,
  regions: {
    listItem: '.list-item',
  },
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(this.options.workspaceLists, 'remove', this.handleRemoveList)
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  handleRemoveList: function(removedList) {
    if (removedList.id === this.model.get('value')) {
      this.model.set('value', undefined)
    }
  },
  onRender: function() {
    DropdownView.prototype.onRender.call(this)
    const listId = this.model.get('value')
    if (listId) {
      this.listItem.show(
        new ListItemView({
          model: this.options.workspaceLists.get(listId),
        })
      )
      this.$el.addClass('list-selected')
    } else {
      this.$el.removeClass('list-selected')
    }
  },
})
