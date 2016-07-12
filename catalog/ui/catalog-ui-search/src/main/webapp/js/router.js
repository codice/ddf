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
    'component/recent/recent',
    'component/recent/recent.view',
    'component/router/router',
    'component/singletons/user-instance',
    'js/jquery.whenAll'
], function (wreqr, $, Backbone, Marionette, store, ConfirmationView, Application, ContentView,
             HomeView, MetacardView, metacardInstance, Query, cql, alertInstance, AlertView,
            recentInstance, RecentView, router, user) {

    function hideViews() {
        Application.App.workspaceRegion.$el.addClass("is-hidden");
        Application.App.workspacesRegion.$el.addClass("is-hidden");
        Application.App.metacardRegion.$el.addClass("is-hidden");
        Application.App.alertRegion.$el.addClass("is-hidden");
        Application.App.recentRegion.$el.addClass("is-hidden");
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
            openRecent: function(){
                //console.log('route to recent uploads');
            }
        },
        appRoutes: {
            'workspaces/:id': 'openWorkspace',
            '(?*)': 'home',
            'workspaces(/)': 'workspaces',
            'metacards/:id': 'openMetacard',
            'alerts/:id': 'openAlert',
            'recent(/)': 'openRecent'
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
                        this.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Either the workspace has been deleted or you no longer have permission to access it. ',
                                yes: 'Go to Workspaces home screen'
                            }),
                            'change:choice',
                            function(){
                                wreqr.vent.trigger('router:navigate', {
                                    fragment: 'workspaces',
                                    options: {
                                        trigger: true
                                    }
                                });
                            });
                    }
                    break;
                case 'openMetacard':
                    var metacardId = args[0];
                    var queryForMetacard = new Query.Model({
                        cql: cql.write({
                            type: '=',
                            value: metacardId,
                            property: '"id"'
                        })
                    });
                    $.whenAll.apply(this, queryForMetacard.startSearch()).always(function(){
                        if (queryForMetacard.get('result').get('results').length === 0) {
                            self.listenTo(ConfirmationView.generateConfirmation({
                                    prompt: 'Metacard(s) unable to be found.  ' +
                                        'This could be do to unavailable sources, deletion of the metacard, or lack of permissions to view the metacard.',
                                    yes: 'Go to Workspaces home screen'
                                }),
                                'change:choice',
                                function(){
                                    wreqr.vent.trigger('router:navigate', {
                                        fragment: 'workspaces',
                                        options: {
                                            trigger: true
                                        }
                                    });
                                });
                        } else {
                            metacardInstance.set({
                                'currentMetacard': queryForMetacard.get('result').get('results').first(),
                                'currentResult': queryForMetacard.get('result')
                            });
                            if (Application.App.metacardRegion.currentView === undefined) {
                                Application.App.metacardRegion.show(new MetacardView());
                            }
                            Application.App.metacardRegion.$el.removeClass('is-hidden');
                            self.updateRoute(name, path, args);
                        }
                    });
                    break;
                case 'openAlert':
                    var alertId = args[0];
                    var alert = user.get('user').get('preferences').get('alerts').get(alertId);
                    if (!alert) {
                        self.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Alert unable to be found.  ',
                                yes: 'Go to Workspaces home screen'
                            }),
                            'change:choice',
                            function () {
                                wreqr.vent.trigger('router:navigate', {
                                    fragment: 'workspaces',
                                    options: {
                                        trigger: true
                                    }
                                });
                            });
                    } else {
                        var queryForMetacards = new Query.Model({
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
                        $.whenAll.apply(this, queryForMetacards.startSearch()).always(function(){
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
                        });
                    }
                    break;
                case 'openRecent':
                    $.get('/search/catalog/internal/metacards/recent').then(function(response){
                        var queryForMetacards = new Query.Model({
                            cql: cql.write({
                                type: 'OR',
                                filters: response.map(function(metacardId){
                                    return {
                                        type: '=',
                                        value: metacardId,
                                        property: '"id"'
                                    };
                                })
                            })
                        });
                        $.whenAll.apply(this, queryForMetacards.startSearch()).always(function(){
                            recentInstance.set({
                                currentResult: queryForMetacards.get('result'),
                                currentQuery: queryForMetacards
                            });
                            if (Application.App.recentRegion.currentView === undefined) {
                                Application.App.recentRegion.show(new RecentView());
                            }
                            Application.App.recentRegion.$el.removeClass('is-hidden');
                            self.updateRoute(name, path, args);
                        });
                    });
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