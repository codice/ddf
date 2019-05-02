const Marionette = require('marionette')
const template = require('./multivalue.hbs')
const CustomElements = require('../../js/CustomElements.js')
const ValueCollection = require('../value/value.collection.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('multivalue'),
  events: {
    'click .multivalue-add': 'addNewValue',
  },
  modelEvents: {
    'change:isEditing': 'handleEdit',
  },
  regions: {
    values: '.multivalue-values',
  },
  initialize: function() {
    this.handleMultivalue()
    this.handleEdit()
  },
  handleEdit: function() {
    this.$el.toggleClass('is-editing', this.model.get('isEditing'))
  },
  onBeforeShow: function() {
    this.values.show(ValueCollection.generateValueCollectionView(this.model))
  },
  handleMultivalue: function() {
    this.$el.toggleClass('is-multivalued', this.model.get('multivalued'))
  },
  serializeData: function() {
    return {
      numberOfValues: Object.keys(this.model.get('value')).length,
    }
  },
  addNewValue: function() {
    this.values.currentView.addNewValue(this.model)
  },
  isValid: function() {
    if (this.model.get('enumCustom')) {
      return true
    }
    return this.values.currentView.children.every(function(valueView) {
      const inputView = valueView.input.currentView
      return inputView.isValid()
    })
  },
})
