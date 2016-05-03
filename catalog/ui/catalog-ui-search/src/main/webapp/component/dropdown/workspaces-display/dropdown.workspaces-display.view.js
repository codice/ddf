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
    '../dropdown.view',
    'text!./dropdown.workspaces-display.hbs',
    'component/workspaces-display/workspaces-display.view'
], function (Marionette, _, $, DropdownView, template, ComponentView) {

    return DropdownView.extend({
        template: template,
        className: 'is-workspacesDisplay',
        componentToShow: ComponentView,
        initializeComponentModel: function(){
            //override if you need more functionality
            this.modelForComponent = this.model;
        },
        isCentered: true,
        getCenteringElement: function(){
            return this.el.querySelector('.dropdown-text');
        },
        serializeData: function(){
            var modelJSON = this.model.toJSON();
            if (modelJSON.value === 'List') {
                modelJSON.icon = 'fa-th-list'
            } else {
                modelJSON.icon = 'fa-th'
            }
            return modelJSON;
        },
        hasTail: true
    });
});
