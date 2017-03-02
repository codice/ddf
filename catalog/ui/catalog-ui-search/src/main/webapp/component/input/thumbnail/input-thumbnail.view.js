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
    './input-thumbnail.hbs',
    '../input.view',
    'component/announcement',
    'js/Common'
], function (Marionette, _, $, template, InputView, announcement, Common) {

    function handleError(){
        announcement.announce({
            title: 'Image Upload Failed',
            message: 'There was an issue loading your image.  Please try again or recheck what you are attempting to upload.',
            type: 'error'
        });
        this.hasUploaded = false;
        this.$el.trigger('change');
        this.render();
    }

    return InputView.extend({
        template: template,
        events: {
            'click button': 'upload',
            'change input': 'handleUpload'
        },
        serializeData: function () {
            return _.extend(this.model.toJSON(), {cid: this.cid});
        },
        handleUpload: function(e){
            var self = this;
            var img = this.$el.find('img')[0];
            var reader = new FileReader();
            reader.onload = function(event){
                img.onload = function(){
                    self.hasUploaded = true;
                    self.$el.trigger('change');
                    self.handleEmpty();
                    self.resizeButton();
                }
                img.onerror = handleError.bind(self);
                img.src = event.target.result;
            }
            reader.onerror = handleError.bind(self);
            reader.readAsDataURL(e.target.files[0]);
        },
        handleValue: function(){
            var self = this;
            var img = this.$el.find('img')[0];
            img.onload = function() {
                self.handleRevert();
                self.resizeButton();
            };
            if (this.model.getValue() && this.model.getValue().constructor === String) {
                img.src = Common.getImageSrc(this.model.getValue());
            }
            this.handleEmpty();
        },
        resizeButton: function(){
            this.$el.find('button').css('height', this.el.querySelector('img').height);
        },
        save: function(){
            var img = this.el.querySelector('img');
            this.model.save(img.src.split(',')[1]);
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
        handleEdit: function () {
            this.$el.toggleClass('is-editing', this.model.isEditing());
            if (!this.model.isEditing()){
                this.hasUploaded = false;
                this.$el.trigger('change');
            }
        },
        handleEmpty: function(){
            if (this.hasUploaded){
                this.$el.toggleClass('is-empty', false);
            } else if (!(this.model.getValue() && this.model.getValue().constructor === String)){
                this.$el.toggleClass('is-empty', true);
            }
        },
        upload: function(){
            this.$el.find('input').click();
        },
        getCurrentValue: function(){
            var img = this.el.querySelector('img');
            return img.src.split(',')[1];
        },
        hasUploaded: false,
        listenForResize: function(){
            $(window).off('resize.' + this.cid).on('resize.' + this.cid, _.throttle(function(event){
                this.resizeButton();
            }.bind(this), 16));
        },
        stopListeningForResize: function(){
            $(window).off('resize.' + this.cid);
        },
        onRender: function(){
            InputView.prototype.onRender.call(this);
            this.listenForResize();
        },
        onDestroy: function(){
            this.stopListeningForResize();
        }
    });
});