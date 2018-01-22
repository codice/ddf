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
    'marionette',
    'underscore',
    'jquery',
    './content-title.hbs',
    'js/CustomElements',
    'js/store',
    'component/unsaved-indicator/workspace/workspace-unsaved-indicator.view'
], function (Marionette, _, $, template, CustomElements, store, UnsavedIndicatorView) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.get('content');
        },
        regions: {
            unsavedIndicator: '.title-saved'
        },
        events: {
            'change input': 'updateWorkspaceName',
            'keyup input': 'updateWorkspaceName',
            'keydown input': 'updateWorkspaceName'
        },
        template: template,
        tagName: CustomElements.register('content-title'),
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this.listenTo(this.model, 'change:currentWorkspace', this.updateIndicator);
            this.listenTo(this.model, 'change:currentWorkspace', this.handleSaved);
        },
        onBeforeShow: function(){
            this.updateIndicator();
        },
        handleSaved: function(){
            var currentWorkspace = this.model.get('currentWorkspace');
            this.$el.toggleClass('is-saved', currentWorkspace ? currentWorkspace.isSaved() : false);
        },
        updateIndicator: function(workspace){
            if (workspace && workspace.changed.currentWorkspace){
                 this.unsavedIndicator.show(new UnsavedIndicatorView({
                    model: this.model.get('currentWorkspace')
                }));
            }
        },
        updateWorkspaceName: function(e){
            this.model.get('currentWorkspace').set('title', e.currentTarget.value);
        }
    });
});
