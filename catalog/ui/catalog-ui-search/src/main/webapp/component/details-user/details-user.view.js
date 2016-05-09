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
    'js/store',
    'text!./details-user.hbs',
    'js/CustomElements',
    'component/dropdown/dropdown',
    'component/dropdown/login-form/dropdown.login-form.view'
], function (Marionette, _, $, store, template, CustomElements, DropdownModel, loginForm) {

    return Marionette.LayoutView.extend({
        regions: {
            'userInfo': '#user-info-region'
        },
        setDefaultModel: function(){
            //override
        },
        template: template,
        tagName: CustomElements.register('details-user'),
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        onRender: function () {
            this.userInfo.show(new loginForm({
                model: new DropdownModel()
            }));
        }
    });
});
