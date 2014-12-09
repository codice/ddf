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
        'js/view/Modal',
        './IngestUploadList.view',
        'text!templates/ingest/ingestModal.handlebars',
        'js/model/Metacard',
        'wreqr',
        'js/view/ingest/MetacardEdit.view'
    ],
    function (ich,_,Marionette,Backbone,$,Modal,UploadList, ingestModalTemplate, Metacard, wreqr, MetacardEdit) {
        ich.addTemplate('ingestModalTemplate',ingestModalTemplate);
        var IngestModal = Modal.extend({
            template: 'ingestModalTemplate',
            events: {
                'click .start-upload':'startUpload',
                'click .remove-upload': 'removeUpload'
            },
            initialize: function(){
                this.stateModel = new Backbone.Model({
                    state: 'start'
                });

                this.collection = new Backbone.Collection();
                this.listenTo(this.collection, "add", this.checkIfDialogComplete);
                this.listenTo(this.collection, "remove", this.checkIfDialogComplete);
                this.listenTo(wreqr.vent, 'ingest:metacard', this.metacardIngested);
                this.listenTo(wreqr.vent, 'ingest:metacardEditDone', this.enableButtons);
            },
            regions: {
                fileUploadListRegion:'.file-upload-region',
                metacardEditRegion: '.metacard-edit-region'
            },
            onRender: function(){
                var view = this;
                this.toggleModalButtons(true);
                view.fileUploadListRegion.show(new UploadList({collection: view.collection}));
                $.ajaxSetup({
                    headers: {directive: "STORE_AND_PROCESS"}
                });
                this.$('.fileupload').fileupload({
                    url: '/services/content',
                    paramName: 'file',
                    dataType: 'json',
                    maxFileSize: 5000000,
                    maxNumberOfFiles: 1,
                    add: function(e, data){
// this overrides the add to use our own model to control when the upload actually happens.
                        var that = this;
                        var model = view.buildModelFromFileData(data);
// we need to pass the parameters along to the model so it knows what to do when it submits.
                        model.fileuploadObject = {
                            ref: that,
                            e: e,
                            data: data
                        };
                        view.collection.reset([model]);
                        view.checkIfDialogComplete();
                    },
                    done: function(e, data){
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var model = view.collection.findWhere(attrs);
                        model.set('state', 'done');
                        view.checkIfDialogComplete();
                        view.stateModel.set('state','done');
                        view.checkIfDialogComplete();

                        var metacard = new Metacard.Metacard(data.result);
                        wreqr.vent.trigger('ingest:metacard', metacard);
                    },
                    fail: function(e, data){
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var model = view.collection.findWhere(attrs);
                        model.set({
                            state: 'failed',
                            error: data.errorThrown
                        });
                        view.stateModel.set('state','start'); // lets start over.
                        view.checkIfDialogComplete();
                    },
                    progress: function(e, data){
                        var progress = parseInt(data.loaded / data.total * 100, 10);
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var model = view.collection.findWhere(attrs);
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
            checkIfDialogComplete: function(){
                var complete = true;
                this.collection.each(function(item) {
                    if (item.get('state') !== 'done') {
                        complete = false;
                    }
                });

                this.toggleModalButtons(complete);
            },
            startUpload: function() {
                this.collection.trigger('startUpload');
            },
            toggleModalButtons: function(showUpload) {
                this.$('.uploadFields').toggleClass('hideButtonGroup', showUpload);
                this.$('.okCancelFields').toggleClass('hideButtonGroup', !showUpload);
            },
            metacardIngested: function(metacard) {
                this.metacardEditRegion.show(new MetacardEdit.MetacardEditView({model: metacard}));
                this.$('button').prop('disabled', true);
                this.$('.fileinput-button').attr('disabled', 'disabled');
            },
            enableButtons: function() {
                this.$('button').prop('disabled', false);
                this.$('.fileinput-button').removeAttr('disabled');
            }
        });
        return IngestModal;
    });