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
  'backbone.marionette',
  'underscore',
  'backbone',
  'js/wreqr.js',
  'jquery',
  'components/configuration-field-multivalue-header/configuration-field-multivalue-header.view',
  'components/configuration-field/configuration-field.view',
  'js/CustomElements',
], function(
  Marionette,
  _,
  Backbone,
  wreqr,
  $,
  ConfigurationFieldMultivalueHeaderView,
  ConfigurationFieldView,
  CustomElements
) {
  return Marionette.CollectionView.extend({
    tagName: CustomElements.register('configuration-field-collection'),
    itemView: ConfigurationFieldView,
    buildItemView: function(item, ItemViewType, itemViewOptions) {
      let view
      const configuration = this.options.configuration
      this.collection.forEach(function(property) {
        if (item.get('id') === property.id) {
          if (property.description) {
            item.set({ description: property.description })
          }
          if (property.note) {
            item.set({ note: property.note })
          }
          const options = _.extend(
            { model: item, configuration: configuration },
            itemViewOptions
          )

          view = new ItemViewType(options)
        }
      })
      if (view) {
        return view
      }
      return new Marionette.ItemView()
    },
    getItemView: function(item) {
      if (item.get('cardinality') > 0 || item.get('cardinality') < 0) {
        return ConfigurationFieldMultivalueHeaderView
      } else {
        return ConfigurationFieldView
      }
    },
  })
})
