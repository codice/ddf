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

        MetricsView.MetricsPage = Marionette.Layout.extend({
            template: 'metricsTemplate',
            initialize: function () {
                _.bindAll(this);
            },
            events: {
                "click #download-summary": "downloadSummary"
            },
            downloadSummary: function (e) {
                e.preventDefault();
                var range = this.$('#range')[0].value;
                var unit = this.$('#unit')[0].value;
                var interval = this.$('#interval')[0].value;
                var endDate = new Date();
                switch (unit) {
                    case 'year':
                        endDate.setUTCMonth(0);
                        endDate.setUTCDate(0);
                        endDate.setUTCHours(0);
                        endDate.setUTCMinutes(0);
                        break;
                    case 'month':
                        endDate.setUTCMonth(endDate.getUTCMonth(), 0);
                        endDate.setUTCHours(0);
                        endDate.setUTCMinutes(0);
                        break;
                    case 'week':
                        endDate.setUTCDate(endDate.getUTCDate() - endDate.getUTCDay());
                        endDate.setUTCHours(0);
                        endDate.setUTCMinutes(0);
                        break;
                    case 'day':
                        endDate.setUTCHours(0);
                        endDate.setUTCMinutes(0);
                        break;
                    case 'hour':
                        endDate.setUTCMinutes(0);
                        break;
                }
                endDate.setUTCSeconds(0);
                endDate.setUTCMilliseconds(0);
                var startDate = new Date(endDate);
                while (range > 0) {
                    switch (unit) {
                        case 'hour':
                            startDate.setUTCHours(startDate.getUTCHours() - 1);
                            break;
                        case 'day':
                            startDate.setUTCDate(startDate.getUTCDate() - 1);
                            break;
                        case 'week':
                            startDate.setUTCDate(startDate.getUTCDate() - 7);
                            break;
                        case 'month':
                            startDate.setUTCMonth(startDate.getUTCMonth() - 1);
                            break;
                        case 'year':
                            startDate.setUTCFullYear(startDate.getUTCFullYear() - 1);
                            break;
                    }
                    range--;
                }
                //endDate.setUTCMinutes(endDate.getUTCMinutes() + endDate.getTimezoneOffset());
                //startDate.setUTCMinutes(startDate.getUTCMinutes() + startDate.getTimezoneOffset());
                console.log(endDate.toISOString().replace('.000Z', 'Z'));
                console.log(startDate.toISOString().replace('.000Z', 'Z'));
                console.log(range + ' ' + unit + ' ' + interval);
                /*global window: false */
                window.location.href = '/services/internal/metrics/report.xls?startDate=' +
                    startDate.toISOString().replace('.000Z', 'Z') + '&endDate=' +
                    endDate.toISOString().replace('.000Z', 'Z') + '&summaryInterval=' + interval;
            }
        });

        return MetricsView;
    });