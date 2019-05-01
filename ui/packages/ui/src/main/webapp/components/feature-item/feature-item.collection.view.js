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
  'backbone.marionette',
  './feature-item.view',
  'js/CustomElements',
  './feature-item.collection.empty.hbs',
], function(Marionette, FeatureItemView, CustomElements, emptyTemplate) {
  return Marionette.CollectionView.extend({
    initialize: function() {
      this.listenTo(this.collection, 'sort', this.render)
    },
    emptyView: Marionette.ItemView.extend({
      template: emptyTemplate,
    }),
    itemView: FeatureItemView,
    itemViewOptions: function() {
      return {
        filter: this.options.filter,
      }
    },
    tagName: CustomElements.register('feature-item-collection'),
    updateFilter: function(filter) {
      this.children.forEach(function(childView) {
        childView.updateFilter && childView.updateFilter(filter)
      })
    },
  })
})
