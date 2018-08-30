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
define([
  'wreqr',
  'marionette',
  'underscore',
  'jquery',
  '../dropdown.view',
  './dropdown.notifications.hbs',
  'component/notifications/notifications.view',
], function(wreqr, Marionette, _, $, DropdownView, template, ComponentView) {
  return DropdownView.extend({
    template: template,
    className: 'is-notifications is-button',
    componentToShow: ComponentView,
    initializeComponentModel: function() {
      //override if you need more functionality
      this.modelForComponent = wreqr.reqres.request('notifications')
      this.handleNotifications()
    },
    listenToComponent: function() {
      this.listenTo(
        this.modelForComponent,
        'add remove reset',
        this.handleNotifications
      )
    },
    handleNotifications: function() {
      this.$el.toggleClass(
        'has-notifications',
        this.modelForComponent.length > 0
      )
    },
    serializeData: function() {
      return this.modelForComponent.toJSON()
    },
    isCentered: true,
    getCenteringElement: function() {
      return this.el.querySelector('.notification-icon')
    },
    hasTail: true,
  })
})
