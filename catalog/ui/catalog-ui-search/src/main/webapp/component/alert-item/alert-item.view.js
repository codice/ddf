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
/*global define, setTimeout*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./alert-item.hbs',
    'js/CustomElements',
    'js/store',
    'js/Common'
], function (wreqr, Marionette, _, $, template, CustomElements, store, Common) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('alert-item'),
        modelEvents: {},
        events: {
            'click .alert-details': 'expandAlert',
            'click .alert-delete': 'removeModel'
        },
        initialize: function(){
            var modelJSON = this.model.toJSON();
            this.listenTo(store.get('workspaces'), 'remove', this.render);
            var workspace = store.get('workspaces').filter(function(workspace){
                return workspace.get('queries').get(modelJSON.queryId);
            })[0];
            var query;
            if (workspace){
                query = workspace.get('queries').get(modelJSON.queryId);
                this.listenTo(workspace, 'change', this.render);
                this.listenTo(workspace, 'destroy', this.render);
            }
            if (query){
                this.listenTo(query, 'change', this.render);
            }
        },
        removeModel: function(){
            this.model.collection.remove(this.model);
            store.get('user').get('user').get('preferences').savePreferences();
        },
        expandAlert: function(){
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
            wreqr.vent.trigger('router:navigate', {
                fragment: 'alerts/'+this.model.id,
                options: {
                    trigger: true
                }
            });
        },
        serializeData: function(){
            var modelJSON = this.model.toJSON();
            var workspace = store.get('workspaces').filter(function(workspace){
                return workspace.get('queries').get(modelJSON.queryId);
            })[0];
            var query;
            if (workspace){
                query = workspace.get('queries').get(modelJSON.queryId);
            }
            return {
                amount: modelJSON.metacardIds.length,
                when: Common.getMomentDate(modelJSON.when),
                queryName: query ? query.get('title') : 'Unknown Query',
                workspaceName: workspace ? workspace.get('title') : 'Unknown Workspace'
            }
        }
    });
});
