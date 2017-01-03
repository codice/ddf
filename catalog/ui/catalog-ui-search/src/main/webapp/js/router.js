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
/*global define, window, setTimeout, location*/

define([
    'wreqr',
    'jquery',
    'backbone',
    'marionette',
    'js/store',
    'component/confirmation/confirmation.view',
    'application',
    'component/content/content.view',
    'component/workspaces/workspaces.view',
    'component/metacard/metacard.view',
    'component/metacard/metacard',
    'js/model/Query',
    'js/cql',
    'component/alert/alert',
    'component/alert/alert.view',
    'component/ingest/ingest.view',
    'component/router/router',
    'component/singletons/user-instance',
    'component/upload/upload',
    'component/upload/upload.view',
    'component/navigator/navigator.view',
    'component/singletons/slideout.left.view-instance.js'
], function (wreqr, $, Backbone, Marionette, store, ConfirmationView, Application, ContentView,
             HomeView, MetacardView, metacardInstance, Query, cql, alertInstance, AlertView,
            IngestView, router, user, uploadInstance, UploadView,
            NavigatorView, SlideoutLeftViewInstance) {

    function toggleNavigator() {
        SlideoutLeftViewInstance.updateContent(new NavigatorView());
        SlideoutLeftViewInstance.open();
    }

    function hideViews() {
        Application.App.workspaceRegion.$el.addClass("is-hidden");
        Application.App.workspacesRegion.$el.addClass("is-hidden");
        Application.App.metacardRegion.$el.addClass("is-hidden");
        Application.App.alertRegion.$el.addClass("is-hidden");
        Application.App.ingestRegion.$el.addClass('is-hidden');
        Application.App.uploadRegion.$el.addClass('is-hidden');
    }

    var Router = Marionette.AppRouter.extend({
        controller: {
            openWorkspace: function(){
                //console.log('route to specific workspace:'+workspaceId);
            },
            home: function(){
               // console.log('route to workspaces home');
            },
            workspaces: function(){
                //console.log('route to workspaces home');
            },
            openMetacard: function(){
                //console.log('route to specific metacard:'+metacardId);
            },
            openAlert: function(){
                //console.log('route to specific alert:'+alertId);
            },
            openIngest: function(){
                //console.log('route to ingest');
            },
            openUpload: function(){
                //console.log('route to upload');
            }
        },
        appRoutes: {
            'workspaces/:id': 'openWorkspace',
            '(?*)': 'home',
            'workspaces(/)': 'workspaces',
            'metacards/:id': 'openMetacard',
            'alerts/:id': 'openAlert',
            'ingest(/)': 'openIngest',
            'uploads/:id': 'openUpload'
        },
        initialize: function(){
            this.listenTo(wreqr.vent, 'router:navigate', this.handleNavigate);
            /*
               HACK:  listeners for the router aren't setup (such as the onRoute or controller)
                    until after initialize is done.  SetTimeout (with timeout of 0) pushes this
                    navigate onto the end of the current execution queue
             */
            setTimeout(function(){
                var currentFragment = location.hash;
                Backbone.history.fragment = undefined;
                this.navigate(currentFragment, {trigger: true});
            }.bind(this), 0);
        },
        handleNavigate: function(args){
              this.navigate(args.fragment, args.options);
        },
        onRoute: function(name, path, args){
            hideViews();
            var self = this;
            var queryForMetacards, queryForMetacard;
            switch(name){
                case 'openWorkspace':
                    var workspaceId = args[0];
                    if (store.get('workspaces').get(workspaceId)!==undefined) {
                        if (Application.App.workspaceRegion.currentView===undefined) {
                            Application.App.workspaceRegion.show(new ContentView());
                        }
                        store.setCurrentWorkspaceById(workspaceId);
                        Application.App.workspaceRegion.$el.removeClass('is-hidden');
                        this.updateRoute(name, path, args);
                    } else {
                        toggleNavigator();
                        this.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Either the workspace has been deleted or you no longer have permission to access it.  ' +
                                'Please use the left navigation to go somewhere else.',
                                yes: 'Okay'
                            }),
                            'change:choice',
                            function(){
                            });
                    }
                    break;
                case 'openMetacard':
                    var metacardId = args[0];
                    queryForMetacard = new Query.Model({
                        cql: cql.write({
                          type: 'AND',
                          filters: [{
                              type: '=',
                              value: metacardId,
                              property: '"id"'
                          }, {
                              type: 'ILIKE',
                              value: '*',
                              property: '"metacard-tags"'
                          }]
                        })
                    });
                    if (metacardInstance.get('currentQuery')){
                        metacardInstance.get('currentQuery').cancelCurrentSearches();
                    }
                    queryForMetacard.startSearch();
                    metacardInstance.set({
                        'currentMetacard': undefined,
                        'currentResult': queryForMetacard.get('result'),
                        'currentQuery': queryForMetacard
                    });
                    if (Application.App.metacardRegion.currentView === undefined) {
                        Application.App.metacardRegion.show(new MetacardView());
                    }
                    Application.App.metacardRegion.$el.removeClass('is-hidden');
                    self.updateRoute(name, path, args);
                    break;
                case 'openAlert':
                    var alertId = args[0];
                    var alert = user.get('user').get('preferences').get('alerts').get(alertId);
                    if (!alert) {
                        toggleNavigator();
                        self.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Alert unable to be found.  Please use the left navigation to go somewhere else.',
                                yes: 'Okay'
                            }),
                            'change:choice',
                            function () {
                            });
                    } else {
                        queryForMetacards = new Query.Model({
                            cql: cql.write({
                                type: 'OR',
                                filters: alert.get('metacardIds').map(function(metacardId){
                                    return {
                                        type: '=',
                                        value: metacardId,
                                        property: '"id"'
                                    };
                                })
                            })
                        });
                        if (alertInstance.get('currentQuery')){
                            alertInstance.get('currentQuery').cancelCurrentSearches();
                        }
                        queryForMetacards.startSearch();
                        alertInstance.set({
                            currentResult: queryForMetacards.get('result'),
                            currentAlert: alert,
                            currentQuery: queryForMetacards
                        });
                        if (Application.App.alertRegion.currentView === undefined) {
                            Application.App.alertRegion.show(new AlertView());
                        }
                        Application.App.alertRegion.$el.removeClass('is-hidden');
                        var workspace = store.get('workspaces').filter(function(workspace){
                            return workspace.get('queries').get(alert.get('queryId'));
                        })[0];
                        if (workspace){
                            store.setCurrentWorkspaceById(workspace.id);
                        }
                        self.updateRoute(name, path, args);
                    }
                    break;
                case 'openUpload':
                    var uploadId = args[0];
                    var upload = user.get('user').get('preferences').get('uploads').get(uploadId);
                    if (!upload) {
                        toggleNavigator();
                        self.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Upload unable to be found.  Please use the left navigation to go somewhere else.',
                                yes: 'Okay'
                            }),
                            'change:choice',
                            function () {
                            });
                    } else {
                        queryForMetacards = new Query.Model({
                            cql: cql.write({
                                type: 'OR',
                                filters: upload.get('uploads').filter(function(file){
                                    return file.id;
                                }).map(function(file){
                                    return {
                                        type: '=',
                                        value: file.id,
                                        property: '"id"'
                                    };
                                }).concat({
                                    type: '=',
                                    value: '-1',
                                    property: '"id"'
                                })
                            })
                        });
                        if (uploadInstance.get('currentQuery')){
                            uploadInstance.get('currentQuery').cancelCurrentSearches();
                        }
                        queryForMetacards.startSearch();
                        uploadInstance.set({
                            currentResult: queryForMetacards.get('result'),
                            currentUpload: upload,
                            currentQuery: queryForMetacards
                        });
                        uploadInstance.trigger('change:currentUpload', upload);
                        if (Application.App.uploadRegion.currentView === undefined) {
                            Application.App.uploadRegion.show(new UploadView());
                        }
                        Application.App.uploadRegion.$el.removeClass('is-hidden');
                        self.updateRoute(name, path, args);
                    }
                    break;
                case 'openIngest':
                    if (Application.App.ingestRegion.currentView === undefined){
                        Application.App.ingestRegion.show(new IngestView());
                    }
                    Application.App.ingestRegion.$el.removeClass('is-hidden');
                    this.updateRoute(name, path, args);
                    break;
                case 'home':
                    if (Application.App.workspacesRegion.currentView===undefined) {
                        Application.App.workspacesRegion.show(new HomeView());
                    }
                    Application.App.workspacesRegion.$el.removeClass('is-hidden');
                    this.updateRoute(name, path, args);
                    break;
                case 'workspaces':
                    if (Application.App.workspacesRegion.currentView===undefined) {
                        Application.App.workspacesRegion.show(new HomeView());
                    }
                    Application.App.workspacesRegion.$el.removeClass('is-hidden');
                    this.updateRoute(name, path, args);
                    break;
            }
        },
        updateRoute: function(name, path, args){
            router.set({
                name: name,
                path: path,
                args: args
            });
            $(window).trigger('resize');
            wreqr.vent.trigger('resize');
        }
    });

    return new Router();
});
