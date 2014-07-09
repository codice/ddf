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
        'js/model/source',
        'poller',
        'wreqr',
        // Load non attached libs and plugins
        'datepicker',
        'datepickerOverride',
        'datepickerAddon',
        'multiselect',
        'multiselectfilter'
    ],
    function(Application, Cometd, Marionette, Source, poller, wreqr) {

        Application.App.module('WorkspaceModule.SearchModule', function(SearchModule) {

            SearchModule.sources = new Source.Collection();
            SearchModule.sources.fetch();

            // Poll the server for changes to Sources every 60 seconds -
            // This matches the DDF SourcePoller polling interval
            poller.get(SearchModule.sources, { delay: 60000 }).start();

            wreqr.reqres.setHandler('sources', function () {
                return SearchModule.sources;
            });
        });

});