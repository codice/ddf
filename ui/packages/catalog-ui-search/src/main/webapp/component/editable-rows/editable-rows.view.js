const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const EditableRowsTemplate = require('./editable-rows.hbs')
const EditableRowsView = require('../editable-row/editable-row.collection.view.js')
const JsonView = require('../json/json.view.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('editable-rows'),
  template: EditableRowsTemplate,
  events: { 'click .add-row': 'addRow' },
  regions: { rows: '.rows' },
  initialize: function() {
    this.listenTo(this.collection, 'add remove update reset', this.checkEmpty)
  },
  checkEmpty: function() {
    this.$el.toggleClass('is-empty', this.collection.isEmpty())
  },
  addRow: function() {
    this.collection.add({})
  },
  embed: function(model) {
    return new JsonView({ model: model })
  },
  onRender: function() {
    this.rows.show(
      new EditableRowsView({
        collection: this.collection,
        embed: this.embed,
        embedOptions: this.options,
      })
    )
    this.checkEmpty()
  },
})
