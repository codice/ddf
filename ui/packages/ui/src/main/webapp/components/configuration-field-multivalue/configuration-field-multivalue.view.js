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

/** Main view page for add. */
define([
  'backbone',
  'backbone.marionette',
  './configuration-field-multivalue.hbs',
  'js/CustomElements',
], function(Backbone, Marionette, template, CustomElements) {
  return Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('configuration-field-multivalue'),
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder()
    },
    events: {
      'click .minus-button': 'minusButton',
    },
    minusButton: function() {
      this.model.collection.remove(this.model)
    },
    onRender: function() {
      const bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
      this.modelBinder.bind(this.model, this.$el, bindings)
    },
    onClose: function() {
      this.modelBinder.unbind()
    },
  });
})
