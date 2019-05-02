const Backbone = require('backbone')
const QueryResult = require('../../../js/model/QueryResult.js')
module.exports = Backbone.AssociatedModel.extend({
  relations: [
    {
      type: Backbone.Many,
      key: 'results',
      relatedModel: QueryResult,
    },
  ],
  defaults: {
    results: [],
  },
  initialize: function() {},
})
