/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
        'backbone',
        'jquery',
        'underscore',
        'marionette',
        'handlebars',
        'icanhaz',
        'text!templates/metrics.handlebars'
    ],
    function (Backbone, $, _, Marionette, Handlebars, ich, metricsTemplate) {

        var MetricsView = {};
        ich.addTemplate('metricsTemplate', metricsTemplate);

        Handlebars.registerHelper('titleizeCamelCase', function (camelCase) {
            // insert a space before all caps
            return camelCase.replace(/([A-Z])/g, ' $1')
                // uppercase the first character
                .replace(/^./, function (str) {
                    return str.toUpperCase();
                });
        });

        MetricsView.MetricsPage = Marionette.ItemView.extend({
            template: 'metricsTemplate',
            initialize: function () {
                _.bindAll(this);
                this.modelBinder = new Backbone.ModelBinder();
                this.listenTo(this.model, 'nested-change', this.updateAndRender);
            },
            updateAndRender: function () {
                this.model.get('summaryParams').summarize();
                this.render();
            },
            converter: function (direction, bindValue) {
                switch (direction) {
                    case 'ViewToModel':
                        return bindValue.toLowerCase().replace('s', '');
                    case 'ModelToView':
                        return bindValue.toString();
                }
            },
            onRender: function () {
                var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name', this.converter);
                this.modelBinder.bind(this.model.get('summaryParams'), this.$el, bindings);
            },
            onClose: function () {
                this.modelBinder.unbind();
            }
        });

        return MetricsView;
    });