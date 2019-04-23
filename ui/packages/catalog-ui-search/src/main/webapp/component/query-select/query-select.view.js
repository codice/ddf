const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const QueryItemCollectionView = require('../query-item/query-item.collection.view.js')

const eventsHash = {
  click: 'handleClick',
};

const namespace = CustomElements.getNamespace();
const queryItemClickEvent = 'click ' + namespace + 'query-item';
eventsHash[queryItemClickEvent] = 'handleQueryItemClick'

module.exports = QueryItemCollectionView.extend({
  className: 'is-query-select composed-menu',
  events: eventsHash,
  onBeforeShow: function() {
    this.handleValue()
  },
  handleQueryItemClick: function(event) {
    this.model.set('value', $(event.currentTarget).attr('data-queryid'))
    this.handleValue()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  handleValue: function() {
    const queryId = this.model.get('value');
    this.$el.find(namespace + 'query-item').removeClass('is-selected')
    if (queryId) {
      this.$el
        .find(namespace + 'query-item[data-queryid="' + queryId + '"]')
        .addClass('is-selected')
    }
  },
})
