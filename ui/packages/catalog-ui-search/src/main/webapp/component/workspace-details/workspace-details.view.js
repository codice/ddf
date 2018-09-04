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
    './workspace-details.hbs',
    'js/CustomElements',
    'moment',
    'component/singletons/user-instance',
    'component/unsaved-indicator/workspace/workspace-unsaved-indicator.view'
], function (wreqr, Marionette, _, $, template, CustomElements, moment, user, UnsavedIndicatorView) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspace-details'),
        regions: {
            unsavedIndicator: '.title-indicator',
        },
        initialize: function(options){
            this.listenTo(user.get('user').get('preferences'), 'change:homeDisplay', this.handleDisplayPref);
        },
        onRender: function(){
            this.handleDisplayPref();
        },
        onBeforeShow: function(){
            this.unsavedIndicator.show(new UnsavedIndicatorView({
                model: this.model
            }));
        },
        handleDisplayPref: function(){
            this.$el.toggleClass('as-list', user.get('user').get('preferences').get('homeDisplay') === 'List');
        },
        serializeData: function() {
            var workspacesJSON = this.model.toJSON();
            workspacesJSON.niceDate = moment(workspacesJSON['metacard.modified']).fromNow();
            workspacesJSON.owner = workspacesJSON['metacard.owner'] || 'Guest';
            return workspacesJSON;
        },
    });
});
