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
/* global define */
define([
    'icanhaz',
    'underscore',
    'marionette',
    'backbone',
    'jquery',
    './../../Modal',
    './UploadList.view',
    'text!templates/application/modals/updateApplicationModal.handlebars'
],
function (ich,_,Marionette,Backbone,$,Modal,UploadList, updateApplicationModal) {

    ich.addTemplate('updateApplicationModal',updateApplicationModal);

    var UpdateApplicationModal = Modal.extend({
        template: 'updateApplicationModal',
        initialize: function(){
            this.stateModel = new Backbone.Model({
                state: 'start'
            });
            this.listenTo(this.stateModel, 'change', this.stateChanged);
        },
        regions: {
            fileUploadListRegion:'.file-upload-region'
        },
        onRender: function(){
            var view = this;
            var collection = new Backbone.Collection();
            view.listenTo(collection, 'uploading', view.fileUploadStart);
            view.fileUploadListRegion.show(new UploadList({collection: collection}));
            this.$('.fileupload').fileupload({
                url: '/services/application/update',
                dataType: 'json',
                maxFileSize: 5000000,
                maxNumberOfFiles: 1,
                add: function(e, data){
                    // this overrides the add to use our own model to controll when the upload actually happens.
                    var that = this;
                    var model = view.buildModelFromFileData(data);
                    // we need to pass the parameters along to the model so it knows what to do when it submits.
                    model.fileuploadObject = {
                        ref: that,
                        e: e,
                        data: data
                    };
                    collection.reset([model]);
                },
                done: function(e, data){
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = collection.findWhere(attrs);
                    model.set('state', 'done');
                    view.markDialogCompleted();
                    view.stateModel.set('state','done');
                },
                fail: function(e, data){
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = collection.findWhere(attrs);
                    model.set({
                        state: 'failed',
                        error: data.errorThrown
                    });
                    view.stateModel.set('state','start');  // lets start over.
                },

                progress: function(e, data){
                    var progress = parseInt(data.loaded / data.total * 100, 10);
                    var attrs = {};
                    attrs.name = data.files[0].name;
                    var model = collection.findWhere(attrs);
                    if(model) {
                        model.set('progress', progress);
                    }
                }
            });
            view.$('.fileupload').fileupload('option', {
                dropZone: view.$el
            });
        },

        onBeforeClose: function(){
            this.$('.fileupload').fileupload('destroy');
        },

        fileUploadStart: function(){
            this.stateModel.set('state','uploading');
        },

        stateChanged: function(){
            var state = this.stateModel.get('state');
            if(state === 'uploading' || state === 'done'){
                this.$('.fileinput-button').attr('disabled', 'disabled');
            } else {
                this.$('.fileinput-button').removeAttr('disabled');
            }
        },

        buildModelFromFileData: function(data){
            var name = data.files[0].name.toLowerCase();
            var type = data.files[0].type;
            if(type === ''){
                if(name.lastIndexOf('kar') === (name.length - 3)){
                    type = 'KAR';
                } else if(name.lastIndexOf('jar') === (name.length - 3)){
                    type = 'JAR';
                }
            }
            return new Backbone.Model({
                name: data.files[0].name,
                size : data.files[0].size,
                type : type,
                state : 'start',
                error : data.errorThrown,
                progress: parseInt(data.loaded / data.total * 100, 10)
            });
        },

        markDialogCompleted: function(){
            this.$('.updateAppCancel').hide();
            this.$('.updateAppClose').show();
        }
    });

    return UpdateApplicationModal;
});