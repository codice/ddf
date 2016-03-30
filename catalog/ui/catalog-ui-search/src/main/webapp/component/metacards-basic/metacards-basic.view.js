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
    'text!./metacards-basic.hbs',
    'js/CustomElements',
    'js/store'
], function (Marionette, _, $, workspaceBasicTemplate, CustomElements, store) {

    var MetacardBasic = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getSelectedResults();
        },
        template: workspaceBasicTemplate,
        tagName: CustomElements.register('metacards-basic'),
        modelEvents: {
        },
        events: {
        },
        regions: {
        },
        initialize: function (options) {
            if (options.model === undefined){
                this.setDefaultModel();
            }
        },
        serializeData: function(){
            var combinedProperties = {};
            this.model.toJSON().forEach(function(metacardResult){
                var properties = metacardResult.metacard.properties;
                for (var property in properties) {
                    combinedProperties[property]  = combinedProperties[property] || [];
                    combinedProperties[property].push(properties[property]);
                }
            });
            return combinedProperties;
        }
    });

    return MetacardBasic;
});
