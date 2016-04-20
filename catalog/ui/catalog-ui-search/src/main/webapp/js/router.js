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
/*global define, window*/

define([
    'wreqr',
    'jquery',
    'marionette',
    'js/store',
    'component/confirmation/confirmation.view'
], function (wreqr, $, Marionette, store, ConfirmationView) {

    var Router = Marionette.AppRouter.extend({
        controller: {
            openWorkspace: function(workspaceId){
                console.log('route to specific workspace:'+workspaceId);
            },
            home: function(){
                console.log('route to workspaces home');
            },
            workspaces: function(){
                console.log('route to workspaces home');
            }
        },
        appRoutes: {
            'workspaces/:id(?*)': 'openWorkspace',
            '(?*)': 'home',
            'workspaces(/)': 'workspaces'
        },
        initialize: function(){
            this.listenTo(wreqr.vent, 'router:navigate', this.handleNavigate);
        },
        handleNavigate: function(args){
              this.navigate(args.fragment, args.options);
        },
        onRoute: function(name, path, args){
            switch(name){
                case 'openWorkspace':
                    var workspaceId = args[0];
                    if (store.get('workspaces').get(workspaceId)!==undefined) {
                        store.setCurrentWorkspaceById(workspaceId);
                        this.updateRoute(name, path, args);
                    } else {
                        this.listenTo(ConfirmationView.generateConfirmation({
                                prompt: 'Workspace has been deleted.  To access this workspace, ' +
                                'please restore it first.',
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
                    this.updateRoute(name, path, args);
                    break;
                case 'workspaces':
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