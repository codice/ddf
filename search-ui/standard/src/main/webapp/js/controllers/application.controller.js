/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define */
var window = this;
define(['jquery',
        'underscore',
        'marionette',
        'application',
        'properties',
        'js/view/SearchControl.view',
        'js/model/source',
        'backbone',
        'poller'
    ], function ($, _, Marionette, app, properties, SearchControl, Source, Backbone, poller) {
        'use strict';
        var document = window.document,
            ApplicationController;

        ApplicationController = Marionette.Controller.extend({
            initialize: function () {
                _.bindAll(this);
            },

            renderApplicationViews: function () {
                var controller = this,
                    mainView = new app.Views.Main({model: new Backbone.Model(properties)}),
                    headerLayout = new app.Views.HeaderLayout(),
                    footerLayout = new app.Views.FooterLayout();

                // Once the main application view has been attached to the DOM, set up the dependent views.
                mainView.on('show', function () {
                    controller.sources = new Source.Collection();
                    controller.sources.fetch({
                         success : function(){
                             controller.renderMainViews(mainView);
                         }
                    });

                    // Poll the server for changes to Sources every 60 seconds -
                    // This matches the DDF SourcePoller polling interval
                    poller.get(controller.sources, { delay: 60000 }).start();
                });

                app.App.headerRegion.show(headerLayout);
                headerLayout.classification.show(new app.Views.HeaderBanner());

                app.App.mainRegion.show(mainView);

                app.App.footerRegion.show(footerLayout);
                footerLayout.classification.show(new app.Views.FooterBanner());

                $(document).ready(function () {
                    document.title = properties.branding;
                });

                //TODO: this hack here is to fix the issue of the main div not resizing correctly
                //when the header and footer are in place
                //remove this code when the correct way to get the div to resize is discovered
                $(window).resize(function() {
                    var height = $('body').height();
                    if(properties.header && properties.header !== '') {
                        height = height - 20;
                    }
                    if(properties.footer && properties.footer !== '') {
                        height = height - 20;
                    }
                    $('#content').height(height);
                });

                $(window).trigger('resize');
            },

             renderMainViews: function () {

                 var searchControlView = new SearchControl.SearchControlLayout({
                     sources : this.sources,
                     el: $('#searchControls'),
                     model: new SearchControl.SearchControlModel(properties)
                 });
                 searchControlView.render();

             }

        });

        return ApplicationController;
    }
);
