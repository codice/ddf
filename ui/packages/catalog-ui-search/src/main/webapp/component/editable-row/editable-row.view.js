const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./editable-row.hbs')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('editable-row'),
  template: template,
  events: { 'click .remove': 'removeRow' },
  regions: { embed: '.embed' },
  removeRow: function() {
    this.model.destroy()
  },
  onRender: function() {
    this.embed.show(this.options.embed(this.model, this.options.embedOptions))
  },
})
