const Backbone = require('backbone')
module.exports = Backbone.Model.extend({
  defaults: {
    isOpen: false,
    value: undefined,
    isEditing: true,
  },
  getValue: function() {
    return this.get('value')
  },
  toggleOpen: function() {
    this.set('isOpen', !this.get('isOpen'))
  },
  close: function() {
    this.set('isOpen', false)
  },
  open: function() {
    this.set('isOpen', true)
  },
})
