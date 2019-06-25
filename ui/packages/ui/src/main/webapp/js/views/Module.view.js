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

import * as React from 'react'
import { iframeResizer } from 'iframe-resizer'
define([
  'backbone.marionette',
  'jquery',
  'js/application',
  'js/modules/Application.module.js',
  'js/modules/Configuration.module.js',
  'js/modules/Installer.module.js',
  'templates/tabs.handlebars',
  'templates/moduleTab.handlebars',
], function(
  Marionette,
  $,
  Application,
  ApplicationModule,
  ConfigurationModule,
  InstallerModule,
  tabs,
  moduleTab
) {
  var ModuleView = Marionette.Layout.extend({
    template: tabs,
    className: 'relative full-height',
    regions: {
      tabs: '#tabs',
      tabContent: '#tabContent',
    },
    events: {
      'shown.bs.tab': 'tabShown',
    },
    tabShown: function(event) {
      if (this.$el.find('#tabs').find(event.target).length === 0) {
        return
      }
      var id = event.target.getAttribute('data-id')
      this.tabs.currentView.children.each(function(childView) {
        childView.$el.toggleClass('active', childView.model.id === id)
      })
      this.tabContent.currentView.children.each(function(childView) {
        childView.$el.toggleClass('active in', childView.model.id === id)
      })
    },
    onRender: function() {
      this.tabs.show(
        new Marionette.CollectionView({
          tagName: 'ul',
          className: 'nav nav-pills nav-stacked',
          collection: this.model.get('value'),
          itemView: Marionette.ItemView.extend({
            tagName: 'li',
            template: moduleTab,
            events: {
              click: 'setHeader',
            },
            setHeader: function() {
              $('#pageHeader').html(this.model.get('name'))
              this.model.set('active', true)
            },
            onRender: function() {
              if (this.model.get('active')) {
                this.$el.addClass('active')
                $('#pageHeader').html(this.model.get('name'))
              }
            },
          }),
        })
      )
      this.tabContent.show(
        new Marionette.CollectionView({
          tagName: 'div',
          className: 'tab-content full-height',
          collection: this.model.get('value'),
          itemView: Marionette.ItemView.extend({
            template() {
              return <React.Fragment />
            },
            tagName: 'div',
            className: 'tab-pane',
            initialize: function() {
              this.listenTo(this.model, 'change:active', this.onRender)
            },
            attachIframeResizer: function() {
              setTimeout(
                function() {
                  this.$('iframe').ready(
                    function() {
                      iframeResizer(null, 'iframe')
                    }.bind(this)
                  )
                }.bind(this),
                0
              )
            },
            loadContent: function() {
              if (this.model.get('loaded')) {
                return
              }
              this.model.set('loaded', true)
              var view = this
              //this dynamically requires in our modules based on wherever the model says they could be found
              //check if we already have the module
              if (
                this.model.get('iframeLocation') &&
                this.model.get('iframeLocation') !== ''
              ) {
                this.$el.html(
                  '<iframe src="' +
                    this.model.get('iframeLocation') +
                    '"></iframe>'
                )
              } else {
                if (Application.App[this.model.get('name')]) {
                  //the require([]) function uses setTimeout internally to make this call asynchronously
                  //we need to do the same thing here so that everything is in place when the module starts
                  setTimeout(function() {
                    Application.App[view.model.get('name')].start()
                  }, 0)
                } else if (this.module && this.module.start) {
                  setTimeout(function() {
                    view.module.start()
                  }, 0)
                } else {
                  let module
                  switch (this.model.get('jsLocation')) {
                    case 'js/modules/Application.module.js':
                      module = ApplicationModule
                      break
                    case 'js/modules/Configuration.module.js':
                      module = ConfigurationModule
                      break
                    case 'js/modules/Installer.module.js':
                      module = InstallerModule
                      break
                    default:
                      console.log('todo: how do we handle this?')
                      break
                  }
                  if (module && module.start) {
                    module.start()
                    view.module = module
                  }
                  if (
                    this.model.get('cssLocation') &&
                    this.model.get('cssLocation') !== ''
                  ) {
                    require(['' + this.model.get('cssLocation')], function(
                      css
                    ) {
                      $(
                        "<style type='text/css'> " + css + ' </style>'
                      ).appendTo('head')
                    })
                  }
                }
              }
              this.attachIframeResizer()
            },
            onRender: function() {
              this.$el.attr('id', this.model.get('id'))
              if (this.model.get('active')) {
                this.$el.addClass('active')
                this.loadContent()
              }
            },
            onClose: function() {
              //stop the module
              if (Application.App[this.model.get('name')]) {
                Application.App[this.model.get('name')].stop()
              } else if (this.module && this.module.stop) {
                this.module.stop()
              }
              //remove the region where the module is rendered
              Application.App.removeRegion(this.model.get('id'))
            },
          }),
        })
      )
    },
  })

  return ModuleView
})
