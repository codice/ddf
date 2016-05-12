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
    'text!./input-thumbnail.hbs',
    '../input.view',
    'js/CustomElements'
], function (Marionette, _, $, template, InputView, CustomElements) {

    return InputView.extend({
        template: template,
        tagName: CustomElements.register('input-thumbnail'),
        attributes: function(){
            return {
                'data-id': this.model.get('id')
            }
        },
        events: {
            'click .input-revert': 'revert',
            'click button': 'upload',
            'change input': 'handleUpload'
        },
        modelEvents: {
            'change:value': 'render'
        },
        regions: {},
        serializeData: function () {
            return _.extend(this.model.toJSON(), {cid: this.cid});
        },
        handleUpload: function(e){
            var self = this;
            var canvas = this.el.querySelector('canvas');
            var button = this.$el.find('button');
            var revert = this.$el.find('.input-revert');
            var ctx = canvas.getContext('2d');
            var reader = new FileReader();
            reader.onload = function(event){
                var img = new Image();
                img.onload = function(){
                    var width = img.width;
                    var height = img.height;
                    var scaleFactor = img.width / (self.el.querySelector('.input-value').clientWidth - 88);
                    if (scaleFactor > 1){
                        width = width / scaleFactor;
                        height = height / scaleFactor;
                    }
                    canvas.width = width;
                    canvas.height = height;
                    button.css('height', height);
                    revert.css('height', Math.max(height, 44));
                    revert.css('line-height', Math.max(height,44) + 'px');
                    ctx.drawImage(img,0,0, width, height);
                    self.hasUploaded = true;
                    self.handleRevert();
                }
                img.src = event.target.result;
            }
            reader.readAsDataURL(e.target.files[0]);
        },
        handleReadOnly: function () {
            this.$el.toggleClass('is-readOnly', this.model.isReadOnly());
        },
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this._editMode);
        },
        handleValue: function(){
            var self = this;
            var canvas = this.el.querySelector('canvas');
            var button = this.$el.find('button');
            var revert = this.$el.find('.input-revert');
            var ctx = canvas.getContext('2d');
            var img = new Image();
            img.onload = function() {
                var width = img.width;
                var height = img.height;
                var scaleFactor = img.width / (self.el.querySelector('.input-value').clientWidth -88);
                if (scaleFactor > 1){
                    width = width / scaleFactor;
                    height = height / scaleFactor;
                }
                canvas.width = width;
                canvas.height = height;
                button.css('height', height);
                revert.css('height', height);
                revert.css('line-height', height + 'px');
                ctx.drawImage(img,0,0, width, height);
                self.handleRevert();
            };
            if (this.model.get('value') && this.model.get('value').substring(0, 4) === 'http') {
                img.src = this.model.get('value');
            } else {
                img.src = "data:image/png;base64,"+this.model.get('value');
            }
        },
        turnOnEditing: function(){
            this._editMode = true;
            this.handleEdit();
        },
        turnOffEditing: function(){
            this._editMode = false;
            this.handleEdit();
        },
        turnOnLimitedWidth: function(){
            this.$el.addClass('has-limited-width');
        },
        revert: function(){
            this.hasUploaded = false;
            this.model.revert();
        },
        save: function(){
            var canvas = this.el.querySelector('canvas');
            this.model.save(canvas.toDataURL('image/png').split(',')[1]);
        },
        focus: function(){
            this.$el.find('input').select();
        },
        hasChanged: function(){
          return this.hasUploaded;
        },
        handleRevert: function(){
            if (this.hasUploaded){
                this.$el.addClass('is-changed');
            } else {
                this.$el.removeClass('is-changed');
            }
        },
        upload: function(){
            this.$el.find('input').click();
        },
        hasUploaded: false,
        _editMode: false
    });
});