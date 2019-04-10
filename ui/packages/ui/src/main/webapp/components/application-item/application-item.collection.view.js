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
  'backbone',
  'backbone.marionette',
  'js/wreqr',
  'jquery',
  'underscore',
  './application-item.view',
  'js/CustomElements',
  './application-item.collection.empty.hbs',
], function(
  Backbone,
  Marionette,
  wreqr,
  $,
  _,
  AppCardItemView,
  CustomElements,
  emptyTemplate
) {
  'use strict'

  // Collection of all the applications
  var AppCardCollectionView = Marionette.CollectionView.extend({
    itemView: AppCardItemView,
    tagName: CustomElements.register('application-item-collection'),
    emptyView: Marionette.ItemView.extend({
      template: emptyTemplate,
    }),
    className: 'apps-grid list',
    itemViewOptions: {},
    modelEvents: {
      change: 'render',
    },
    initialize: function(options) {
      this.AppShowState = options.AppShowState
      this.listenTo(wreqr.vent, 'toggle:layout', this.toggleLayout)
    },
  })

  return AppCardCollectionView
})
