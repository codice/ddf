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
        'text!templates/metrics.handlebars',
        'text!templates/details.handlebars',
        'text!templates/summary.handlebars'
    ],
    function (Backbone, $, _, Marionette, Handlebars, ich, metricsTemplate, detailsTemplate, summaryTemplate) {

        var MetricsView = {};
        ich.addTemplate('metricsTemplate', metricsTemplate);
        ich.addTemplate('detailsTemplate', detailsTemplate);
        ich.addTemplate('summaryTemplate', summaryTemplate);

        Handlebars.registerHelper('titleizeCamelCase', function (camelCase) {
            // insert a space before all caps
            return camelCase.replace(/([A-Z])/g, ' $1')
                // uppercase the first character
                .replace(/^./, function (str) {
                    return str.toUpperCase();
                });
        });

        MetricsView.Summary = Marionette.ItemView.extend({
            template: 'summaryTemplate',

            initialize: function () {
                _.bindAll(this);
                this.modelBinder = new Backbone.ModelBinder();
                this.listenTo(this.model, 'change', this.updateAndRender);
            },
            updateAndRender: function () {
                this.model.summarize();
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
                this.modelBinder.bind(this.model, this.$el, bindings);
            },
            onClose: function () {
                this.modelBinder.unbind();
            }
        });

        MetricsView.Details = Marionette.ItemView.extend({
            template: 'detailsTemplate',
            initialize: function () {
                this.listenTo(this.model, 'sync', this.render);
            }
        });

        MetricsView.MetricsPage = Marionette.Layout.extend({
            template: 'metricsTemplate',
            regions: {
                details: '#details-region',
                summary: '#summary-region'
            },
            onShow: function () {
                this.details.show(new MetricsView.Details({model: this.model.get('details')}));
                this.summary.show(new MetricsView.Summary({model: this.model.get('summary')}));
            }
        });

        return MetricsView;
    });