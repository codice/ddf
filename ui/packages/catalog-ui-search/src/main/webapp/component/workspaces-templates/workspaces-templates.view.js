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
    './workspaces-templates.hbs',
    'js/CustomElements',
    'js/store',
    'component/loading/loading.view',
    'js/Transitions',
    'component/property/property.view',
    'component/property/property',
    'properties',
    'behaviors/region.behavior'
], function (wreqr, Marionette, _, $, template, CustomElements, store, LoadingView, 
    Transitions, PropertyView, PropertyModel, properties) {

    var triggers = {
        expand: 'homeTemplates:expand',
        close: 'homeTemplates:close'
    };

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspaces-templates'),
        events: {
            'click .home-templates-choices-choice': 'createNewWorkspace',
            'click .home-templates-header-button': 'toggleExpansion',
            'click .adhoc-go': 'startAdhocSearch',
            'keyup .adhoc-search': 'handleAdhocKeyup'
        },
        behaviors() {
            this.adhocSearchModel = new PropertyModel({
                value: [''],
                label: '',
                type: 'STRING',
                showValidationIssues: false,
                showLabel: false,
                placeholder: 'Search ' + properties.branding + ' ' + properties.product,
                isEditing: true
            });
            return {
                region: {
                    regions: [
                        {
                            selector: '.adhoc-search',
                            view: PropertyView,
                            viewOptions: {
                                model: this.adhocSearchModel
                            }
                        }
                    ]
                }
            }
        },  
        focus: function(){
            
        },
        handleAdhocKeyup(event) {
            switch(event.keyCode){
                case 13:
                    this.startAdhocSearch();
                break;
                default:
                break;
            }
        },
        startAdhocSearch: function(){
            this.prepForCreateNewWorkspace();
            store.get('workspaces').createAdhocWorkspace(this.adhocSearchModel.get('value')[0]);
        },
        prepForCreateNewWorkspace: function(){
            var loadingview = new LoadingView();
            store.get('workspaces').once('sync', function(workspace, resp, options){
                loadingview.remove();
                wreqr.vent.trigger('router:navigate', {
                    fragment: 'workspaces/'+workspace.id,
                    options: {
                        trigger: true
                    }
                });
            });
            this.close();
        },
        createNewWorkspace: function(e){
            this.prepForCreateNewWorkspace();
            switch($(e.currentTarget).attr('data-template')) {
                case 'blank':
                    store.get('workspaces').createWorkspace();
                    break;
                case 'local':
                    store.get('workspaces').createLocalWorkspace();
                    break;
                case 'all':
                    store.get('workspaces').createAllWorkspace();
                    break;
                case 'geo':
                    store.get('workspaces').createGeoWorkspace();
                    break;
                case 'latest':
                    store.get('workspaces').createLatestWorkspace();
                    break;
                default:
                    store.get('workspaces').createWorkspace();
                    break;
            }
        },
        toggleExpansion: function() {
            if (this.$el.hasClass('is-expanded')) {
                this.close();
            } else {
                this.expand();
            }
        },
        expand: function(){
            this.$el.find('.home-templates-header-button-closed').addClass('is-hidden');
            this.$el.find('.home-templates-header-button-expanded').removeClass('is-hidden');
            this.$el.addClass('is-expanded');
            this.triggerMethod(triggers.expand);
        },
        close: function(){
            this.$el.find('.home-templates-header-button-closed').removeClass('is-hidden');
            this.$el.find('.home-templates-header-button-expanded').addClass('is-hidden');
            this.$el.removeClass('is-expanded');
            this.$el.animate({
                scrollTop: 0
            }, Transitions.coreTransitionTime);
            this.triggerMethod(triggers.close);
        }
    });
});
