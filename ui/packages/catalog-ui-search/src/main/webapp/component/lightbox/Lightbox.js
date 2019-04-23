const Backbone = require('backbone')
const Lightbox = Backbone.Model.extend({
  defaults: {
    open: false,
    title: 'Default Title',
  },
  close: function() {
    this.set('open', false)
  },
  open: function() {
    this.set('open', true)
  },
  isOpen: function() {
    return this.get('open')
  },
  updateTitle: function(title) {
    this.set('title', title)
  },
})

module.exports = Lightbox
