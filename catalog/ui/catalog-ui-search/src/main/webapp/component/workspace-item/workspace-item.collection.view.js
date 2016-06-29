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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'js/CustomElements',
    './workspace-item.view',
    'js/model/user'
], function (Marionette, _, $, CustomElements, WorkspaceItemView, user) {

    var getUser = function () {
         return user.get('user');
    };

    var getPrefs = function () {
         return getUser().get('preferences');
    };

    return Marionette.CollectionView.extend({
        tagName: CustomElements.register('workspace-item-collection'),
        childView: WorkspaceItemView,
        filter: function (workspace) {
            var localStorage = workspace.get('localStorage') || false;
            var owner = workspace.get('owner');
            var user = getUser().get('email');

            switch (getPrefs().get('homeFilter')) {
                case 'Not owned by me':
                    return !localStorage && user !== owner;
                case 'Owned by me':
                    return localStorage || user === owner;
                case 'Owned by anyone':
                default:
                    return true;
            }
        },
        viewComparator: function (workspace) {
            switch (getPrefs().get('homeSort')) {
                case 'Title':
                    return workspace.get('title');
                case 'Last modified':
                default:
                    return workspace.get('modified')
            }
        },
        initialize: function () {
            this.listenTo(getPrefs(), 'change:homeDisplay', this.switchDisplay);
            this.listenTo(getPrefs(), 'change:homeFilter', this.render);
            this.listenTo(getPrefs(), 'change:homeSort', this.render);
        },
        onBeforeAddChild: function(childView){
            switch(getPrefs().get('homeDisplay')){
                case 'List':
                    childView.activateListDisplay();
                    break;
                case 'Grid':
                    childView.activateGridDisplay();
                    break;
            }
        },
        activateGridDisplay: function(){
            this.children.forEach(function (childView) {
                childView.activateGridDisplay();
            });
        },
        activateListDisplay: function(){
            this.children.forEach(function (childView) {
                childView.activateListDisplay();
            });
        },
        switchDisplay: function(model, displayType){
            switch(displayType){
                case 'List':
                    this.activateListDisplay();
                    break;
                case 'Grid':
                    this.activateGridDisplay();
                    break;
            }
        }
    });
});