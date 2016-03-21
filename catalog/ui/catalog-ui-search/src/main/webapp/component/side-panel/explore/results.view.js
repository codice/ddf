define([
  'marionette',
  'text!./result.hbs',
], function (Marionette, query) {

  var QueryView =  Marionette.ItemView.extend({
    template: query
  })

  var QueriesView = Marionette.CollectionView.extend({
    childView: QueryView
  })

  return QueriesView
})
