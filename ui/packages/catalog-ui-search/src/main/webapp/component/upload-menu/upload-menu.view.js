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
    './upload-menu.hbs',
    'js/CustomElements',
    'js/store',
    'component/upload/upload',
    'js/Common',
    'component/router/router'
], function (wreqr, Marionette, _, $, template, CustomElements, store, uploadInstance, Common, router) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('upload-menu'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.handleRoute();
        },
        handleRoute: function(){
            if (router.toJSON().name === 'openUpload'){
                this.model = uploadInstance.get('currentUpload');
                this.render();
            }
        },
        serializeData: function(){
            return {
                when: Common.getMomentDate(uploadInstance.get('currentUpload').get('sentAt'))
            };
        }
    });
});
