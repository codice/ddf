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
/*global define, alert*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./input-bulk.hbs',
    'js/CustomElements',
    'component/input/input.view',
    'component/multivalue/multivalue.view'
], function (Marionette, _, $, template, CustomElements, InputView, MultivalueView) {

    return InputView.extend({
        className: 'is-bulk',
        template: template,
        regions: {
            otherInput: '.input-other'
        },
        events: {
            'change select': 'handleChange'
        },
        onRender: function () {
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.handleOther();
            this.handleBulk();
        },
        onBeforeShow: function () {
            this.otherInput.show(new MultivalueView({
                model: this.model
            }));
        },
        handleChange: function () {
            this.handleOther();
        },
        handleOther: function () {
            if (this.$el.find(':selected[data-bulkcustom]').length > 0) {
                this.$el.addClass('is-other');
            } else {
                this.$el.removeClass('is-other');
            }
        },
        handleBulk: function(){
            if (this.model.isHomogeneous()){
                this.turnOffBulk();
            }
        },
        turnOffBulk: function(){
            this.$el.addClass('is-homogeneous')
        },
        hasChanged: function(){
            if (this.model.isHomogeneous()) {
                return this.otherInput.currentView.hasChanged();
            } else if (this.$el.find(':selected[data-bulkdefault]').length > 0) {
                return false;
            } else {
                return true;
            }
        },
        getCurrentValue: function(){
            if (this.model.isHomogeneous()) {
                return this.otherInput.currentView.getCurrentValue();
            } else if (this.$el.find(':selected[data-bulkdefault]').length > 0) {
                return false;
            } else if (this.$el.find(':selected[data-bulkcustom]').length > 0) {
                return this.otherInput.currentView.getCurrentValue();
            } else {
                return this.model.get('values')[this.$el.find(':selected').val()].value;
            }
        }
    });
});