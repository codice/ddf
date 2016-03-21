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
define([
    'jquery',
    'underscore',
    'marionette',
    'backbone',
    'properties',
    'wreqr',
    'js/model/Filter',
    'text!templates/filter/filter.item.handlebars',
    'bootstrapselect'
], function ($, _, Marionette, Backbone, Properties, wreqr, Filter, filterItemTemplate) {
    'use strict';
    var FilterItemView = Marionette.ItemView.extend({
        className: 'item',
        tagName: 'tr',
        template: filterItemTemplate,
        events: {
            'click .remove': 'removePressed',
            'change .hasDatepicker': 'dateChanged',
            'click .draw': 'drawClicked'
        },
        modelEvents: {
            'change:fieldName': 'fieldNameChanged',
            'change:fieldOperator': 'fieldOperatorChanged',
            'change:geoType': 'render',
            'EndExtent': 'drawingStopped'
        },
        collectionEvents: {
            'remove': 'render',
            'add': 'render'
        },
        initialize: function () {
            this.modelbinder = new Backbone.ModelBinder();
        },
        templateHelpers: function () {
            return {
                fields: this.options.fields,
                operations: Properties.filters.OPERATIONS,
                isOnlyFilter: this.collection.length === 1,
                isValidGeo: this.model.isValidGeo()
            };
        },
        removePressed: function () {
            this.model.trigger('removePressed', this.model);
            if (this.model.get('fieldName') === 'anyGeo') {
                // TODO when we add multishape support we need to adjust this.
                wreqr.vent.trigger('search:drawend');
            }
        },
        onRender: function () {
            var view = this;
            view.modelbinder.bind(view.model, view.$el);
            view.$('.query-dropdown').selectpicker();
            view.$('.date-field').datetimepicker({
                dateFormat: $.datepicker.ATOM,
                timeFormat: 'HH:mm:ss.lz',
                separator: 'T',
                timezoneIso8601: true,
                useLocalTimezone: true,
                showHour: true,
                showMinute: true,
                showSecond: false,
                showMillisec: false,
                showTimezone: false,
                minDate: new Date(100, 0, 2),
                maxDate: new Date(9999, 11, 30)
            });
            var geoType = this.model.get('geoType'), fieldName = this.model.get('fieldName');
            if (fieldName === 'anyGeo') {
                if (geoType === 'bbox') {
                    wreqr.vent.trigger('search:drawend');
                    wreqr.vent.trigger('search:bboxdisplay', this.model);
                } else if (geoType === 'circle') {
                    wreqr.vent.trigger('search:drawend');
                    wreqr.vent.trigger('search:circledisplay', this.model);
                } else if (geoType === 'polygon') {
                    wreqr.vent.trigger('search:drawend');
                    wreqr.vent.trigger('search:polydisplay', this.model);
                }
            }
        },
        onBeforeClose: function () {
            this.unbindForm();
        },
        unbindForm: function () {
            this.modelbinder.unbind();
            this.$('.date-field').datetimepicker('destroy');
        },
        fieldNameChanged: function () {
            this.unbindForm();
            var curFieldName = this.model.get('fieldName');
            var field = _.findWhere(this.options.fields, { name: curFieldName });
            this.model.set('fieldType', field.type);
            this.model.set('fieldOperator', Properties.filters.OPERATIONS[field.type][0]);
            this.render();
        },
        fieldOperatorChanged: function () {
            this.unbindForm();
            this.render();    // just rerender.
        },
        dateChanged: function (evt) {
            var view = this;
            var elem = view.$(evt.currentTarget);
            var value = $.trim(elem.val());
            var hasValue = false;
            if (value && value !== '') {
                hasValue = true;
            }
            elem.parent().toggleClass('has-value', hasValue);
        },
        toggleRemoveButton: function (removeButtonFlag) {
            this.$('.btn.remove').toggle(removeButtonFlag);
        },
        drawClicked: function () {
            this.$('.draw').attr('disabled', 'disabled');
            var geoType = this.model.get('geoType');
            if (geoType === 'bbox') {
                wreqr.vent.trigger('search:drawend');
                wreqr.vent.trigger('search:drawbbox', this.model);
            } else if (geoType === 'circle') {
                wreqr.vent.trigger('search:drawend');
                wreqr.vent.trigger('search:drawcircle', this.model);
            } else if (geoType === 'polygon') {
                wreqr.vent.trigger('search:drawend');
                wreqr.vent.trigger('search:drawpoly', this.model);
            }
        },
        drawingStopped: function () {
            this.$('.draw').removeAttr('disabled', false);
            this.render();
        }
    });
    return FilterItemView;
});
