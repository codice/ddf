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
    './workspace-item.view'
], function (Marionette, _, $, CustomElements, WorkspaceItemView) {

    return Marionette.CollectionView.extend({
        tagName: CustomElements.register('workspace-item-collection'),
        childView: WorkspaceItemView,
        onBeforeAddChild: function(childView){
            switch(this.displayType){
                case 'List':
                    childView.activateListDisplay();
                    break;
                case 'Grid':
                    childView.activateGridDisplay();
                    break;
            }
        },
        activateGridDisplay: function(){
            this.displayType = 'Grid';
            this.children.forEach(function (childView) {
                childView.activateGridDisplay();
            });
        },
        activateListDisplay: function(){
            this.displayType = 'List';
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
        },
        displayType: 'Grid'
    });
});