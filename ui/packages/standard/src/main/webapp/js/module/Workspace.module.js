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
        'js/view/Workspace.view',
        'js/model/Workspace',
        'wreqr',
        'poller',
        'js/model/source',
        'underscore',
        'properties',
        // Load non attached libs and plugins
        'datepicker',
        'datepickerOverride',
        'datepickerAddon',
        'multiselect',
        'multiselectfilter'
    ],
    function(Application, Cometd, Marionette, WorkspaceView, Workspace, wreqr, poller, Source, _, properties) {

        Application.App.module('WorkspaceModule', function(WorkspaceModule) {

            var setTypes = function () {
                var allTypes = [];
                if (_.size(properties.typeNameMapping) > 0) {
                    _.each(properties.typeNameMapping, function(value, key) {
                        if (_.isArray(value)) {
                            allTypes.push({
                                name: key,
                                value: value.join(',')
                            });
                        }
                    });
                } else {
                    allTypes = _.chain(WorkspaceModule.sources.map(function (source) {
                        return source.get('contentTypes');
                    }))
                    .flatten()
                    .filter(function (element) {
                        return element.name !== '';
                    })
                    .sortBy(function (element) {
                        return element.name.toUpperCase();
                    })
                    .uniq(false, function (type) {
                        return type.name;
                    })
                    .map(function (element) {
                        element.value = element.name;
                        return element;
                    })
                    .value();
                }
                WorkspaceModule.types.set(allTypes);
            };

            WorkspaceModule.sources = new Source.Collection();
            WorkspaceModule.sources.fetch().done(function() {
                setTypes();
            });

            WorkspaceModule.types = new Source.Types();

            // Poll the server for changes to Sources every 60 seconds -
            // This matches the DDF SourcePoller polling interval
            poller.get(WorkspaceModule.sources, { delay: 60000 }).start();

            wreqr.reqres.setHandler('workspace:getsources', function () {
                return WorkspaceModule.sources;
            });

            this.listenTo(WorkspaceModule.sources, 'change', setTypes);

            wreqr.reqres.setHandler('workspace:gettypes', function () {
                return WorkspaceModule.types;
            });

            WorkspaceModule.workspaces = new Workspace.WorkspaceResult();
            WorkspaceModule.workspaces.fetch();

            wreqr.reqres.setHandler('workspace:getworkspaces', function () {
                return WorkspaceModule.workspaces;
            });

            var workspaceView = new WorkspaceView.PanelLayout({model: WorkspaceModule.workspaces});

            var Controller = Marionette.Controller.extend({

                initialize: function(options){
                    this.region = options.region;
                },

                show: function(){
                    this.region.show(workspaceView);
                }

            });

            WorkspaceModule.addInitializer(function(){
                WorkspaceModule.contentController = new Controller({
                    region: Application.App.controlPanelRegion
                });
                WorkspaceModule.contentController.show();
            });
        });

    });