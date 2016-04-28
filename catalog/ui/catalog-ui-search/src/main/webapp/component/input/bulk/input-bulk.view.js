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
    '../input.view',
    '../thumbnail/input-thumbnail.view',
    '../date/input-date.view',
    'component/input/input',
    'component/input/thumbnail/input-thumbnail',
    'component/input/date/input-date',
], function (Marionette, _, $, template, CustomElements, InputView, ThumbnailInputView, DateInputView,
        InputModel, ThumbnailInputModel, DateInputModel) {

    return InputView.extend({
        template: template,
        tagName: CustomElements.register('input-bulk'),
        regions: {
            otherInput: '.input-other'
        },
        events: {
            'click .input-revert': 'revert',
            'change select': 'handleChange'
        },
        onRender: function(){
            switch (this.model.get('type')) {
                case 'DATE':
                    this._otherInputModel = new DateInputModel(this.model.attributes);
                    this._otherInputView = new DateInputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
                case 'STRING':
                    this._otherInputModel = new InputModel(this.model.attributes);
                    this._otherInputView = new InputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
                case 'GEOMETRY':
                    this._otherInputModel = new InputModel(this.model.attributes);
                    this._otherInputView = new InputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
                case 'XML':
                    this._otherInputModel = new InputModel(this.model.attributes);
                    this._otherInputView = new InputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
                case 'BINARY':
                    this._otherInputModel = new ThumbnailInputModel(this.model.attributes);
                    this._otherInputView = new ThumbnailInputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
                default:
                    this._otherInputModel = new InputModel(this.model.attributes);
                    this._otherInputView = new InputView({
                        model: this._otherInputModel
                    });
                    this._otherInputView.turnOnEditing();
                    this.otherInput.show(this._otherInputView);
                    break;
            }
            this.handleEdit();
            this.handleReadOnly();
            this.handleValue();
            this.handleRevert();
            this.handleOther();
        },
        handleRevert: function(){
            if (this.$el.find(':selected[data-bulkdefault]').length > 0){
                this.$el.removeClass('is-changed');
            } else {
                this.$el.addClass('is-changed');
            }
        },
        handleChange: function(){
            this.handleRevert();
            this.handleOther();
        },
        handleOther: function(){
            if (this.$el.find(':selected[data-bulkcustom]').length > 0){
                this.$el.addClass('is-other');
            } else {
                this.$el.removeClass('is-other');
            }
        }
    });
});