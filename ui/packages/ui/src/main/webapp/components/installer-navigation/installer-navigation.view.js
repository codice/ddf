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
/*global define, alert*/
/** Main view page for add. */
define([
  'marionette',
  './installer-navigation.hbs',
  './installer-navigation-buttons.hbs',
  'backbone',
  'js/CustomElements',
  'modelbinder',
], function(
  Marionette,
  navigationTemplate,
  navButtons,
  Backbone,
  CustomElements
) {
  var WelcomeView = Marionette.ItemView.extend({
    template: navigationTemplate,
    tagName: CustomElements.register('installer-navigation'),
    events: {
      'click .previous': 'previous',
      'click .next': 'next',
      'click .finish': 'finish',
      'click .restart-later': 'admin',
      'click .restart-now': 'shutdown',
    },
    initialize: function() {
      this.listenTo(this.model, 'change', this.updateProgress)
      this.modelBinder = new Backbone.ModelBinder()
    },
    onRender: function() {
      var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name')
      this.modelBinder.bind(this.model, this.$el, bindings)
      this.updateProgress()
    },
    close: function() {
      this.stopListening(this.model)
      this.modelBinder.unbind()
    },
    updateProgress: function() {
      var percentComplete =
        100 * (this.model.get('stepNumber') / this.model.get('totalSteps')) +
        '%'
      this.$('.progress-bar').animate({ width: percentComplete }, 0, 'swing')
      this.$('.pager').html(navButtons(this.model.toJSON()))
      this.$('.progress-text').show()
    },
    previous: function() {
      this.model.trigger('previous')
    },
    next: function() {
      this.model.trigger('next')
    },
    finish: function() {
      this.model.trigger('block')
      this.model.set({
        message:
          'Shutting down.... When complete please manually start the system and then go to the Admin Console.',
      })
      this.model.save().fail(function() {
        alert(
          'Final installation failed, please check application logs for details.'
        )
      })
    },
    shutdown: function() {
      this.model.trigger('block')
      this.model.set({
        message: 'Restarting now.... This could take a few minutes.',
      })
      this.model.save(true).fail(function() {
        alert(
          'Final installation failed, please check application logs for details.'
        )
      })
    },
    admin: function() {
      this.model.trigger('block')
      this.model.set({ message: 'Completing installation. Please wait...' })
      this.model.save().fail(function() {
        alert(
          'Final installation failed, please check application logs for details.'
        )
      })
    },
  })

  return WelcomeView
})
