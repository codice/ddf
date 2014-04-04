/*global define */
var window = this;
define(['jquery',
        'underscore',
        'marionette',
        'application',
        'properties',
        'js/view/SearchControl.view',
        'js/model/source',
    ], function ($, _, Marionette, app, properties, SearchControl, Source) {
        'use strict';
        var document = window.document,
            ApplicationController;

        ApplicationController = Marionette.Controller.extend({
            initialize: function () {
                _.bindAll(this);
            },

            renderApplicationViews: function () {
                var controller = this,
                    mainView = new app.Views.Main(),
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

             },

        });

        return ApplicationController;
    }
);
