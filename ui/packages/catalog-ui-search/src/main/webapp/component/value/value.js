const Backbone = require('backbone')
const PropertyModel = require('../property/property.js')
require('backbone-associations')

module.exports = Backbone.AssociatedModel.extend({
  defaults: {
    value: undefined,
    isValid: true,
    property: undefined,
  },
  relations: [
    {
      type: Backbone.One,
      key: 'property',
      relatedModel: PropertyModel,
    },
  ],
  setValue: function(value) {
    this.set('value', value)
  },
  getValue: function() {
    return this.get('value')
  },
  setIsValid: function(isValid) {
    this.set('isValid', isValid)
  },
  isValid() {
    return this.get('isValid')
  },
  getCalculatedType: function() {
    return this.get('property').getCalculatedType()
  },
  getId: function() {
    return this.get('property').getId()
  },
  isReadOnly: function() {
    return this.get('property').isReadOnly()
  },
  isEditing: function() {
    return this.get('property').isEditing()
  },
  isMultivalued: function() {
    return this.get('property').isMultivalued()
  },
  onlyEditing: function() {
    return this.get('property').onlyEditing()
  },
  showLabel: function() {
    return this.get('property').showLabel()
  },
  showValidationIssues: function() {
    return this.get('property').showValidationIssues()
  },
})
