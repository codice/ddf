/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./home-templates.hbs',
    'js/CustomElements',
    'js/store',
    'component/loading/loading.view',
    'js/router'
], function (wreqr, Marionette, _, $, template, CustomElements, store, LoadingView, router) {

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
        },
        template: template,
        tagName: CustomElements.register('home-templates'),
        modelEvents: {
        },
        events: {
            'click .home-templates-choices-choice': 'createNewWorkspace'
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
        },
        createNewWorkspace: function(){
            var loadingview = new LoadingView();
            store.get('workspaces').once('sync', function(workspace, resp, options){
                loadingview.remove();
                router.navigate('workspaces/'+workspace.id, {trigger: true});
            });
            store.get('workspaces').createWorkspace();
        }
    });
});
