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
/* global define */
define([
  'backbone.marionette',
  './tab-item.view',
  'js/CustomElements',
], function(Marionette, TabItemView, CustomElements) {
  return Marionette.CollectionView.extend({
    tagName: CustomElements.register('tab-item-collection'),
    itemView: TabItemView,
    events: {
      'shown.bs.tab': 'tabShown',
    },
    tabShown: function(event) {
      const id = event.target.getAttribute('data-id');
      this.children.each(function(childView) {
        if (childView.model.id === id) {
          childView.triggerShown()
        } else {
          childView.triggerHidden()
        }
      })
    },
  });
})
