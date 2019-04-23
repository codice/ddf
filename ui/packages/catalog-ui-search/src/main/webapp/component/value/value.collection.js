const Backbone = require('backbone')
const Value = require('./value')
module.exports = Backbone.Collection.extend({
  model: Value,
})
