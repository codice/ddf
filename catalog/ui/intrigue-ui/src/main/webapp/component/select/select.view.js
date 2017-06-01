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
    './select.hbs',
    'js/CustomElements'
], function (Marionette, _, $, template, CustomElements) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('select'),
        attributes: function(){
            return {
                'data-hits': this.model.get('hits'),
                'data-help': this.model.get('help')
            };
        },
        serializeData: function(){
            var modelJSON = this.model.toJSON();
            if (modelJSON.label.constructor === Array){
                modelJSON.label = modelJSON.label.join(' | ');
            }
            return modelJSON;
        }
    });
});
