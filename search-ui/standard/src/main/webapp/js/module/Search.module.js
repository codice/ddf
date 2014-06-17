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
/* global define */
define(['application',
        'cometdinit',
        'marionette',
        'js/view/Search.view',
        'js/model/source',
        'poller',
        // Load non attached libs and plugins
        'datepicker',
        'datepickerOverride',
        'datepickerAddon',
        'multiselect',
        'multiselectfilter'
    ],
    function(Application, Cometd, Marionette, Search, Source, poller) {

        Application.App.module('SearchModule', function(SearchModule) {

            this.sources = new Source.Collection();
            this.sources.fetch();

            // Poll the server for changes to Sources every 60 seconds -
            // This matches the DDF SourcePoller polling interval
            poller.get(this.sources, { delay: 60000 }).start();

            var searchView = new Search.SearchLayout({
                sources: this.sources
            });

            var Controller = Marionette.Controller.extend({

                initialize: function(options){
                    this.region = options.region;
                },

                show: function(){
                    this.region.show(searchView);
                }

            });

            SearchModule.addInitializer(function(){
                SearchModule.contentController = new Controller({
                    region: Application.App.searchRegion
                });
                SearchModule.contentController.show();
            });
        });

});