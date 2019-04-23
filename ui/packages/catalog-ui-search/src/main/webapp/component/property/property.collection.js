const Backbone = require('backbone')
const Property = require('./property')
module.exports = Backbone.Collection.extend({
  model: Property,
})
