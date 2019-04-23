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
import { iframeResizer } from 'iframe-resizer'
define([
  'require',
  'backbone.marionette',
  'backbone',
  'components/iframe/iframe.view.js',
  'components/application-services/application-services.view',
  'components/features/features.view',
  'components/system-information/system-information.view',
  './tab-content.hbs',
  'js/wreqr.js',
  'js/CustomElements',
], function(
  require,
  Marionette,
  Backbone,
  IFrameView,
  ApplicationServicesView,
  FeaturesView,
  SystemInformationView,
  template,
  wreqr,
  CustomElements
) {
  return Marionette.Layout.extend({
    template: template,
    tagName: CustomElements.register('tab-content'),
    className: 'tab-pane fade',
    regions: {
      tabContentInner: '.tab-content-inner',
    },
    initialize: function(options) {
      this.applicationModel = options.applicationModel
      this.listenTo(wreqr.vent, 'application:tabShown', this.handleTabShown)
      this.listenTo(wreqr.vent, 'application:tabHidden', this.handleTabHidden)
    },
    onBeforeRender: function() {
      this.$el.attr('id', this.model.get('id'))
    },
    onRender: function() {
      const view = this;
      const iframeLocation = view.model.get('iframeLocation');
      const jsLocation = view.model.get('javascriptLocation');
      if (jsLocation) {
        let newView
        switch (jsLocation) {
          case 'components/application-services/application-services.view':
            newView = ApplicationServicesView
            break
          case 'components/features/features.view.js':
            newView = FeaturesView
            break
          case 'components/system-information/system-information.view.js':
            newView = SystemInformationView
            break
          default:
            console.log('todo: what do we do?')
            break
        }
        newView = new newView({ model: view.applicationModel })
        view.tabContentInner.show(newView)
      } else if (iframeLocation) {
        view.tabContentInner.show(
          new IFrameView({
            model: new Backbone.Model({ url: iframeLocation }),
          })
        )
      }
    },
    handleTabHidden: function(id) {
      if (id === this.model.id) {
        this.$el.removeClass('active in')
      }
    },
    handleTabShown: function(id) {
      if (id === this.model.id) {
        iframeResizer(null, 'iframe')
        if (
          this.tabContentInner.currentView &&
          this.tabContentInner.currentView.focus
        ) {
          this.tabContentInner.currentView.focus()
        }
      }
    },
  });
})
