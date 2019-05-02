const Backbone = require('backbone')
module.exports = Backbone.Model.extend({
  defaults: {
    prompt: 'Default prompt.',
    no: undefined,
    yes: 'Default yes',
    choice: undefined,
  },
  makeChoice: function(choice) {
    this.set('choice', choice)
  },
})
