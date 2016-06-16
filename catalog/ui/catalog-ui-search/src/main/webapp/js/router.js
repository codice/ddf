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
    'component/home/home.view',
    'component/metacard/metacard.view',
    'component/metacard/metacard',
    'js/model/Query',
    'js/cql',
    'js/jquery.whenAll'
], function (wreqr, $, Backbone, Marionette, store, ConfirmationView, Application, ContentView,
             HomeView, MetacardView, metacardInstance, Query, cql) {

    function hideViews() {
        Application.App.workspaceRegion.$el.addClass("is-hidden");
        Application.App.workspacesRegion.$el.addClass("is-hidden");
        Application.App.metacardRegion.$el.addClass("is-hidden");
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
            }
        },
        appRoutes: {
            'workspaces/:id': 'openWorkspace',
            '(?*)': 'home',
            'workspaces(/)': 'workspaces',
            'metacards/:id': 'openMetacard'
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
                    var self = this;
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
                                    prompt: 'Either the metacard has been deleted or you no longer have permission to access it. ',
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
                            metacardInstance.set('currentMetacard', queryForMetacard.get('result').get('results').first());
                            if (Application.App.metacardRegion.currentView === undefined) {
                                Application.App.metacardRegion.show(new MetacardView());
                            }
                            Application.App.metacardRegion.$el.removeClass('is-hidden');
                            self.updateRoute(name, path, args);
                        }
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
            store.get('router').set({
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