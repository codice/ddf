import { Model } from 'backbone'

module.exports = new (Model.extend({
  initialize: function() {
    this.set('federation', 'enterprise')
  },
  setFederation: function(federation) {
    this.set('federation', federation)
  },
  getFederation: function() {
    return this.get('federation')
  },
}))()
