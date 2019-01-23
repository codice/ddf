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
/*global define*/
/** Main view page for add. */
define([
  'backbone.marionette',
  'underscore',
  'backbone',
  'js/wreqr.js',
  'jquery',
  './configuration-field-multivalue-header.hbs',
  'components/configuration-field-multivalue/configuration-field-multivalue.collection.view',
  'js/CustomElements',
], function(
  Marionette,
  _,
  Backbone,
  wreqr,
  $,
  template,
  ConfigurationFieldMultivalueCollectionView,
  CustomElements
) {
  return Marionette.Layout.extend({
    template: template,
    itemView: ConfigurationFieldMultivalueCollectionView,
    tagName: CustomElements.register('configuration-field-multivalue-header'),
    regions: {
      listItems: '#listItems',
    },
    /**
     * Button events, right now there's a submit button
     * I do not know where to go with the cancel button.
     */
    events: {
      'click .plus-button': 'plusButton',
    },
    modelEvents: {
      change: 'updateValues',
    },
    initialize: function(options) {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.configuration = options.configuration
      this.collectionArray = new Backbone.Collection()
      this.listenTo(wreqr.vent, 'refresh', this.updateValues)
      this.listenTo(wreqr.vent, 'beforesave', this.saveValues)
    },
    updateValues: function() {
      var csvVal,
        view = this
      if (
        this.configuration.get('properties') &&
        this.configuration.get('properties').get(this.model.get('id'))
      ) {
        csvVal = this.configuration.get('properties').get(this.model.get('id'))
      } else {
        csvVal = this.model.get('defaultValue')
      }
      this.collectionArray.reset()
      if (csvVal && csvVal !== '') {
        if (_.isArray(csvVal)) {
          _.each(csvVal, function(item) {
            view.addItem({
              value: item,
              optionLabels: view.model.get('optionLabels'),
              optionValues: view.model.get('optionValues'),
              type: view.model.get('type'),
            })
          })
        } else {
          _.each(csvVal.split(/[,]+/), function(item) {
            view.addItem({
              value: item,
              optionLabels: view.model.get('optionLabels'),
              optionValues: view.model.get('optionValues'),
              type: view.model.get('type'),
            })
          })
        }
      }
    },
    saveValues: function() {
      var values = []
      _.each(this.collectionArray.models, function(model) {
        values.push(model.get('value'))
      })
      this.configuration.get('properties').set(this.model.get('id'), values)
    },
    onRender: function() {
      this.listItems.show(
        new ConfigurationFieldMultivalueCollectionView({
          collection: this.collectionArray,
        })
      )

      this.updateValues()
    },
    addItem: function(value) {
      this.collectionArray.add(new Backbone.Model(value))
    },
    /**
     * Creates a new text field for the properties collection.
     */
    plusButton: function() {
      this.addItem({
        value: '',
        optionLabels: this.model.get('optionLabels'),
        optionValues: this.model.get('optionValues'),
        type: this.model.get('type'),
      })
    },
  })
})
