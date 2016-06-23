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
    'underscore',
    'jquery',
    '../dropdown.view',
    'text!./dropdown.alerts.hbs',
    'component/alerts/alerts.view',
    'js/store'
], function (_, $, DropdownView, template, ComponentView, store) {

    return DropdownView.extend({
        template: template,
        className: 'is-alerts',
        componentToShow: ComponentView,
        initializeComponentModel: function(){
            //override if you need more functionality
            this.modelForComponent = store.get('user').get('user').get('preferences').get('alerts');
        },
        listenToComponent: function () {
            this.listenTo(this.modelForComponent, 'add', this.render);
            this.listenTo(this.modelForComponent, 'remove', this.render);
            this.listenTo(this.modelForComponent, 'reset', this.render);
        },
        serializeData: function () {
            return this.modelForComponent.toJSON();
        },
        isCentered: true,
        getCenteringElement: function(){
            return this.el.querySelector('.notification-icon');
        },
        hasTail: true
    });

});
