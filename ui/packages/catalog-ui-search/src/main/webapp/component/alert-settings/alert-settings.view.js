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
/*global define, setTimeout*/
define([
  'marionette',
  'underscore',
  'jquery',
  './alert-settings.hbs',
  'js/CustomElements',
  'component/singletons/user-instance',
  'component/property/property.view',
  'component/property/property',
], function(
  Marionette,
  _,
  $,
  template,
  CustomElements,
  user,
  PropertyView,
  Property
) {
  return Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('alert-settings'),
    modelEvents: {},
    events: {
      'click .editor-edit': 'turnOnEditing',
      'click .editor-cancel': 'cancel',
      'click .editor-save': 'save',
    },
    regions: {
      propertyPersistence: '.property-persistence',
      propertyExpiration: '.property-expiration',
    },
    ui: {},
    onBeforeShow: function() {
      this.setupPersistence()
      this.setupExpiration()
      this.handlePersistence()
      this.startListeningToRegions()
      this.turnOnEditing()
    },
    startListeningToRegions: function() {
      this.listenTo(
        this.propertyPersistence.currentView.model,
        'change:value',
        this.handlePersistence
      )
      this.listenTo(
        this.propertyExpiration.currentView.model,
        'change:value',
        this.save
      )
      this.listenTo(
        this.propertyPersistence.currentView.model,
        'change:value',
        this.save
      )
    },
    stopListeningToRegions: function() {
      this.stopListening(this.propertyPersistence.currentView.model)
    },
    handlePersistence: function() {
      const persistence = this.propertyPersistence.currentView.model.getValue()[0]
      this.$el.toggleClass('is-persisted', persistence)
    },
    setupPersistence: function() {
      const persistence = user
        .get('user')
        .get('preferences')
        .get('alertPersistence')
      this.propertyPersistence.show(
        new PropertyView({
          model: new Property({
            id: 'Keep notifications after logging out',
            enum: [
              {
                label: 'Yes',
                value: true,
              },
              {
                label: 'No',
                value: false,
              },
            ],
            value: [persistence],
          }),
        })
      )
    },
    setupExpiration: function() {
      const expiration = user
        .get('user')
        .get('preferences')
        .get('alertExpiration')
      const millisecondsInDay = 24 * 60 * 60 * 1000
      this.propertyExpiration.show(
        new PropertyView({
          model: new Property({
            id: 'Expire After',
            enum: [
              {
                label: '1 Day',
                value: millisecondsInDay,
              },
              {
                label: '2 Days',
                value: 2 * millisecondsInDay,
              },
              {
                label: '4 Days',
                value: 4 * millisecondsInDay,
              },
              {
                label: '1 Week',
                value: 7 * millisecondsInDay,
              },
              {
                label: '2 Weeks',
                value: 14 * millisecondsInDay,
              },
              {
                label: '1 Month',
                value: 30 * millisecondsInDay,
              },
              {
                label: '2 Months',
                value: 60 * millisecondsInDay,
              },
              {
                label: '4 Months',
                value: 120 * millisecondsInDay,
              },
              {
                label: '6 Months',
                value: 180 * millisecondsInDay,
              },
              {
                label: '1 Year',
                value: 365 * millisecondsInDay,
              },
            ],
            value: [expiration],
          }),
        })
      )
    },
    turnOnEditing: function() {
      this.$el.addClass('is-editing')
      this.regionManager.forEach(function(region) {
        if (region.currentView) {
          region.currentView.turnOnEditing()
        }
      })
    },
    save: function() {
      const preferences = user.get('user').get('preferences')
      preferences.set({
        alertPersistence: this.propertyPersistence.currentView.model.get(
          'value'
        )[0],
        alertExpiration: this.propertyExpiration.currentView.model.get(
          'value'
        )[0],
      })
      preferences.savePreferences()
    },
  })
})
