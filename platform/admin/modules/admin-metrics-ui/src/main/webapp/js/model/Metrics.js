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
        'moment',
        'backboneassociation'
    ],
    function (Backbone, moment) {
        "use strict";
        var Metrics = {};

        Metrics.DetailModel = Backbone.AssociatedModel.extend({
            urlRoot: '/services/internal/metrics/',
            initialize: function () {
                this.fetch();
            }
        });

        Metrics.SummaryModel = Backbone.AssociatedModel.extend({
            defaults: {
                'ranges': [
                    '1',
                    '2',
                    '3',
                    '6'
                ],
                'units': [
                    'hour',
                    'day',
                    'week',
                    'month',
                    'year'
                ],
                'intervals': [
                    'minute',
                    'hour',
                    'day',
                    'week',
                    'month'
                ],
                'range': '1',
                'unit': 'hour',
                'interval': 'minute'
            },
            formatDate: function (startDate) {
                return startDate.toDate().toISOString().replace('.000Z', 'Z');
            },
            summarize: function () {
                var endDate = moment();
                endDate.utc();
                var unit = this.get('unit');
                endDate.startOf(unit);
                var startDate = moment(endDate);
                startDate.utc();
                startDate.subtract(this.get('range'), unit + 's');
                this.set('download', '/services/internal/metrics/report.xls?startDate=' +
                    this.formatDate(startDate) + '&endDate=' + this.formatDate(endDate) +
                    '&summaryInterval=' + this.get('interval'));
            },
            initialize: function () {
                this.summarize();
            }
        });

        Metrics.MetricsModel = Backbone.AssociatedModel.extend({
            initialize: function () {
                this.set({'summary': new Metrics.SummaryModel()});
                this.set({'details': new Metrics.DetailModel()});
            },
            relations: [
                {
                    type: Backbone.One,
                    key: 'summary',
                    relatedModel: Metrics.SummaryModel
                },
                {
                    type: Backbone.One,
                    key: 'details',
                    relatedModel: Metrics.DetailModel
                }
            ]
        });
        return Metrics;

    });