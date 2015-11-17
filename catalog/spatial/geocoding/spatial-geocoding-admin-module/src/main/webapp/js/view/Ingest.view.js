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
        'jquery',
        'backbone',
        'underscore',
        'backbone.marionette',
        'handlebars',
        'icanhaz',
        'text!templates/ingestPage.handlebars',
        './IngestUploadList.view',
        'fileupload'
    ],
    function ($, Backbone, _, Marionette, Handlebars, ich, webService, UploadList) {

        var IngestView = {};
        var checked;
        ich.addTemplate('webService', webService);

        IngestView.Details = Marionette.LayoutView.extend({
             events: {
                'click .start-upload-web' : 'postWebFile',
                'click .clear-list-local' : 'clearListLocal'
            },
            template: 'webService',
            regions: {
                fileUploadListRegion: '.file-upload-region'
            },
            initialize: function () {
                this.modelBinder = new Backbone.ModelBinder();
                this.listenTo(this.model, "change:state", this.update);
                this.listenTo(this.model, "change:progress", this.update);
            },
            update : function () {
                this.render();
            },
            onRender: function () {

                var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name', this.converter);
                var view = this;
                view.modelBinder.bind(this.model, this.$el, bindings);
                view.fileUploadListRegion.show(new UploadList({collection: view.model.collection}));

                this.$('.fileupload').fileupload({
                    url: '/services/content',
                    paramName: 'file',
                    dataType: 'json',
                    maxFileSize: 5000000,
                    formData: {directive: "STORE"},
                    add: function(e, data){
                        var that = this;
                        var model = view.model.buildModelFromFileData(data);
                        checked = $("#create_index_local").is(':checked');

                        model.fileuploadObject = {
                            ref: that,
                            e: e,
                            data: data
                        };

                        // Allow only one item at a time to be uploaded
                        var uploadsInProgress = view.model.collection.findWhere({state : "uploading"});
                        // Allow no duplicate entries
                        var duplicates = view.model.collection.findWhere({name : model.attributes.name});

                        if(typeof duplicates === "undefined" && typeof uploadsInProgress === "undefined") {
                            view.model.collection.add(model);
                            // Check for valid file extension before uploading
                            if(view.isValidGeoNamesFile(data.files[0].name)) {
                                view.model.collection.trigger('startUpload');
                                $.blueimp.fileupload.prototype.options.add.call(model.fileuploadObject.ref, model.fileuploadObject.e, model.fileuploadObject.data);
                            } else {
                                model.set('state', 'failed');
                            }
                        }
                    },
                    done: function(e, data){
                        var contentId = data.files[0].name;
                        var model = view.model.collection.findWhere({name : contentId});
                        var contentFolder = data._response.jqXHR.getResponseHeader("Content-ID");
                        view.postLocalFile(contentFolder, contentId, model);
                    },
                    fail: function(e, data){
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var model = view.model.collection.findWhere(attrs);

                        model.set({
                            state: 'failed',
                            error: data.errorThrown
                        });
                    },
                    progress: function(e, data){
                        var progress = parseInt(data.loaded / data.total * 50, 10);
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var model = view.model.collection.findWhere(attrs);
                        if(model) {
                            model.set('progress', progress);
                        }
                    }
                });

                this.$('.fileupload').fileupload('option', {
                    dropZone: view.$el
                });

                this.setupPopOver('[data-toggle="url-ingest-popover"]', 'Download a GeoNames file from the provided Geonames URL and import it into DDF.');
                this.setupPopOver('[data-toggle="url-popover"]', 'The GeoNames URL to download a GeoNames file from.');
                this.setupPopOver('[data-toggle="local-ingest-popover"]', 'Upload a GeoNames file into DDF.');
                this.setupPopOver('[data-toggle="create-index-popover"]', 'Check to overwrite all previous GeoName entries.');
            },
            onClose: function () {
                this.modelBinder.unbind();
            },
            postWebFile: function () {
                var checked = $("#create_index_web").is(':checked');
                this.model.updateGeoIndexWithUrl(checked);
            },
            postLocalFile: function(resourceUri, id, model) {
                this.model.updateGeoIndexWithFilePath(resourceUri, id, model, checked);
            },
            clearListLocal: function () {
                this.model.collection.reset();
            },
            isValidGeoNamesFile: function(name) {
                var extension = name.split(".");
                return (extension[1] === "zip" || extension[1] === "txt");
            },
            setupPopOver: function(selector, content) {
                var options = {
                    trigger: 'hover',
                    content: content
                };
                this.$el.find(selector).popover(options);
            }
        });
        return IngestView;
    });