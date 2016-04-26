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
    'component/home/home.view'
], function (wreqr, $, Backbone, Marionette, store, ConfirmationView, Application, ContentView, HomeView) {

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
            }
        },
        appRoutes: {
            'workspaces/:id': 'openWorkspace',
            '(?*)': 'home',
            'workspaces(/)': 'workspaces'
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
            switch(name){
                case 'openWorkspace':
                    var workspaceId = args[0];
                    if (store.get('workspaces').get(workspaceId)!==undefined) {
                        if (Application.App.workspaceRegion.currentView===undefined) {
                            Application.App.workspaceRegion.show(new ContentView());
                        }
                        store.setCurrentWorkspaceById(workspaceId);
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
                case 'home':
                    if (Application.App.workspacesRegion.currentView===undefined) {
                        Application.App.workspacesRegion.show(new HomeView());
                    }
                    this.updateRoute(name, path, args);
                    break;
                case 'workspaces':
                    if (Application.App.workspacesRegion.currentView===undefined) {
                        Application.App.workspacesRegion.show(new HomeView());
                    }
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