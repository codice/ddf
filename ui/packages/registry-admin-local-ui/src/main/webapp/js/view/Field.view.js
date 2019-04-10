/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

define([
  'icanhaz',
  'marionette',
  'backbone',
  'wreqr',
  'underscore',
  'jquery',
  'text!templates/field.handlebars',
], function(ich, Marionette, Backbone, wreqr, _, $, field) {
  ich.addTemplate('field', field)

  var Field = {}

  Field.FieldView = Marionette.ItemView.extend({
    template: 'field',
    tagName: 'div',
    className: 'node-field',
    events: {
      'click .remove-field': 'removeField',
      'click .add-value': 'addValue',
      'click .remove-value': 'removeValue',
    },
    modelEvents: {
      'change:error': 'showHideError',
      change: 'fieldChanged',
    },

    initialize: function(options) {
      this.readOnly = options.readOnly
      this.modelBinder = new Backbone.ModelBinder()
      if (this.model.get('inlineGroup')) {
        this.$el.css('display', 'inline-block')
        this.$el.addClass(this.model.get('inlineGroup'))
      }
    },
    onRender: function() {
      var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name')
      this.modelBinder.bind(this.model, this.$el, bindings)
      var possibleValues = this.model.get('possibleValues')
      if (possibleValues) {
        this.$('.' + this.model.get('key')).autocomplete({
          source: possibleValues,
          change: function(event) {
            $(event.target).change()
          },
        })
      }
    },
    addValue: function() {
      this.model.addValue('')
      this.render()
      wreqr.vent.trigger('valueAdded:' + this.model.get('parentId'))
    },
    removeValue: function(event) {
      this.model.removeValue(event.target.getAttribute('name'))
      this.render()
      wreqr.vent.trigger('valueRemoved:' + this.model.get('parentId'))
    },
    removeField: function() {
      wreqr.vent.trigger(
        'removeField:' + this.model.get('parentId'),
        this.model.get('key')
      )
    },
    showHideError: function() {
      wreqr.vent.trigger('fieldErrorChange:' + this.model.get('parentId'))
      this.render()
    },
    fieldChanged: function() {
      wreqr.vent.trigger('fieldChange:' + this.model.get('parentId'))
    },
    serializeData: function() {
      var data = {}

      if (this.model) {
        data = this.model.toJSON()
        data.validationError = this.model.get('error')
        data.errorIndices = this.model.errorIndices
        if (
          this.model.get('identityNode') &&
          this.model.get('key') === 'Name'
        ) {
          data.editable = false
        }
        if (this.readOnly) {
          data.editable = false
        }
      }
      return data
    },
    onClose: function() {
      this.modelBinder.unbind()
    },
  })

  Field.FieldCollectionView = Marionette.CollectionView.extend({
    itemView: Field.FieldView,
    tagName: 'tr',
    initialize: function(options) {
      this.readOnly = options.readOnly
      this.listenTo(
        wreqr.vent,
        'addedField:' + options.parentId,
        this.addedField
      )
      this.listenTo(
        wreqr.vent,
        'removedField:' + options.parentId,
        this.removedField
      )
    },
    buildItemView: function(item, ItemViewType, itemViewOptions) {
      var options = _.extend(
        {
          model: item,
          parentId: this.options.parentId,
          readOnly: this.readOnly,
        },
        itemViewOptions
      )
      return new ItemViewType(options)
    },
    addedField: function(field) {
      this.collection.add(field)
      this.render()
    },
    removedField: function(field) {
      this.collection.remove(field)
      this.render()
    },
  })

  return Field
})
