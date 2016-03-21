define([
  'marionette',
  'text!./queries-layout.hbs',
  'text!./query.hbs',
  'js/store'
], function (Marionette, layout, query, store) {

  var selected = store.get('selected')

  var QueryView =  Marionette.ItemView.extend({
    modelEvents: {
      'change': 'render'
    },
    events: {
      'click .query-item': 'selectQuery'
    },
    initialize: function () {
      this.listenTo(selected, 'change', this.render)
    },
    serializeData: function () {
      return {
        query: this.model.toJSON(),
        selected: this.model === selected.get('object')
      }
    },
    selectQuery: function () {
      selected.set({
        type: 'query',
        object: this.model
      })
    },
    template: query
  })

  var QueriesView = Marionette.CollectionView.extend({
    childView: QueryView
  })

  var QueriesLayoutView = Marionette.LayoutView.extend({
    className: 'queries-layout-view',
    template: layout,
    regions : {
      queriesRegion: '#queries'
    },
    events: {
      'click #add-query': 'addQuery'
    },
    initialize: function () {
      this.listenTo(this.collection, 'add', this.render)
      this.listenTo(this.collection, 'remove', this.render)
    },
    addQuery: function () {
      selected.set({
        type: 'query',
        object: this.collection.add({})
      });
    },
    serializeData: function () {
      return {
        hasQueries: this.collection.length > 0
      }
    },
    onRender: function () {
      this.queriesRegion.show(new QueriesView({ collection: this.collection }))
    }
  })

  return QueriesLayoutView
})
