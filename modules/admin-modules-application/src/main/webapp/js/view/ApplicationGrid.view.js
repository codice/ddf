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
/*global define*/
/** Main view page for add. **/
define([
    'require',
    'backbone',
    'marionette',
    'icanhaz',
    'underscore',
    'jquery',
    '/applications/js/view/app-grid/AppCardCollection.view.js',
    'text!applicationNew',
    'text!mvnItemTemplate',
    'text!fileProgress',
    'text!applicationOutlineButtons',
    '/applications/js/wreqr.js',
    'fileupload',
    'perfectscrollbar'
], function(require, Backbone, Marionette, ich, _, $, AppCardCollectionView, applicationNew, mvnItemTemplate, fileProgress,
            applicationOutlineButtons, wreqr) {
    "use strict";

    if(!ich.applicationNew) {
        ich.addTemplate('applicationNew', applicationNew);
    }
    if(!ich.mvnItemTemplate) {
        ich.addTemplate('mvnItemTemplate', mvnItemTemplate);
    }
    if(!ich.fileProgress) {
        ich.addTemplate('fileProgress', fileProgress);
    }
    if(!ich.applicationOutlineButtons) {
        ich.addTemplate('applicationOutlineButtons', applicationOutlineButtons);
    }



    var Model = {};

    var MvnUrlColl = Backbone.Collection.extend({
        configUrl: "/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service",
        collectedData: function () {
            var data = {
                type: 'EXEC',
                mbean: 'org.codice.ddf.admin.application.service.ApplicationService:service=application-service',
                operation: 'addApplications'
            };
            data.arguments = [];
            data.arguments.push(this.toJSON());
            return data;
        },
        save: function () {
            var addUrl = [this.configUrl, "addApplications"].join("/");
            var collect = this.collectedData();
            var jData = JSON.stringify(collect);

            return $.ajax({
                type: 'POST',
                contentType: 'application/json',
                data: jData,
                url: addUrl
            });
        }
    });

    var mvnUrlColl = new MvnUrlColl();
    var fileUploadColl = new Backbone.Collection();

    var MvnUrlList = Marionette.CollectionView.extend({
        itemView: Marionette.ItemView.extend({
            tagName: 'li',
            className: 'url-list-item',
            template: 'mvnItemTemplate',
            events: {
                'click .remove-url-link': 'removeUrl',
                'click .editable': 'makeEditable',
                'click .done': 'doneEdit',
                'keypress input[name=value]': 'doneEditKey'
            },
            initialize: function() {
                this.modelBinder = new Backbone.ModelBinder();
            },
            onRender: function() {
                var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                this.modelBinder.bind(this.model, this.$el, bindings);
            },
            removeUrl: function() {
                this.model.collection.remove(this.model);
            },
            makeEditable: function() {
                this.$('.editable').hide();
                this.$('.editing').show();
                this.$('input[name=value]').focus();
            },
            doneEditKey: function(e) {
                if(e.keyCode === 13) {
                    this.doneEdit();
                }
            },
            doneEdit: function() {
                this.$('.editable').show();
                this.$('.editing').hide();
            }
        }),
        tagName: 'ul',
        className: 'url-list'
    });

    var FileUploadList = Marionette.CollectionView.extend({
        itemView: Marionette.ItemView.extend({
            tagName:'li',
            className: 'file-list-item',
            template: 'fileProgress',
            events: {
                'click .cancel-upload-link': 'abort'
            },
            initialize: function() {
                this.listenTo(this.model, 'change', this.render);
            }
        }),
        tagName: 'ul',
        className: 'file-list'
    });

    var NewApplicationView = Marionette.Layout.extend({
        template: 'applicationOutlineButtons',
        regions: {
            urlContainer: '#urlContainer',
            fileContainer: '#fileContainer'
        },
        events: {
            'click #add-url-btn': 'addUrl',
            'keypress #urlField': 'addUrlKey',
            'shown.bs.tab': 'setFocus',
            'click .submit-button': 'saveChanges',
            'click .cancel-button': 'cancelChanges',
            'hidden.bs.modal': 'cancelChanges',
            'click .edit-toggle':'editToggleClicked',
            'change .card-style-toggle .btn': 'toggleDisplayOptions'
        },
        modelEvents: {
            'change': 'modelChanged'
        },
        initialize: function(options) {
            this.response = options.response;
            this.model = new Backbone.Model({
                isEditMode: false,
                displayMode: 'card'
            });
        },
        onRender: function() {
            var view = this;
            this.urlContainer.show(new MvnUrlList({collection: mvnUrlColl}));
            this.fileContainer.show(new FileUploadList({collection: fileUploadColl}));
            _.defer(function() {
                view.$('#fileupload').fileupload({
                    fail: function (e, data) {
                        var attrs = {};
                        attrs.name = data.files[0].name;
                        var fileModel = fileUploadColl.findWhere(attrs);
                        attrs.size = data.files[0].size;
                        attrs.type = data.files[0].type;
                        attrs.fail = true;
                        attrs.error = data.errorThrown;
                        if(fileModel) {
                            fileModel.set(attrs);
                        } else {
                            fileUploadColl.add(new Backbone.Model(attrs));
                        }
                    },
                    progress: function (e, data) {
                        var attrs = {};
                        var progress = parseInt(data.loaded / data.total * 100, 10);
                        attrs.name = data.files[0].name;
                        var fileModel = fileUploadColl.findWhere(attrs);
                        attrs.size = data.files[0].size;
                        attrs.type = data.files[0].type;
                        attrs.loaded = data.loaded;
                        attrs.total = data.total;
                        attrs.progress = progress;
                        if(fileModel) {
                            fileModel.set(attrs);
                        } else {
                            fileUploadColl.add(new Backbone.Model(attrs));
                        }
                    }
                });
                view.$('#fileupload').fileupload('option', {
                    dropZone: view.$el
                });
            });
        },
        setFocus: function() {
            this.$('#urlField').focus();
        },
        addUrlKey: function(e) {
            if(e.keyCode === 13) {
                this.addUrl();
            }
        },
        addUrl: function() {
            var value = this.$("#urlField").val();
            if(value !== '' && value.indexOf('mvn:') !== -1 && value.indexOf('xml/features') !== -1) {
                mvnUrlColl.add(new Backbone.Model({value: this.$("#urlField").val()}));
                this.$("#urlField").val('');
                this.setFocus();
                this.$('.file-fail-text').html('');
            } else {
                this.$('.file-fail-text').html('Please enter a valid Maven URL of the form: mvn:groupId:artifactId/version/xml/features');
            }
        },
        saveChanges: function() {
            var view = this;
            mvnUrlColl.save().success(function() {
                mvnUrlColl.reset();
                view.response.fetch({
                    success: function(model){
                        Model.Collection.set(model.get("value"));
                    }
                });
            });
            fileUploadColl.reset();
        },
        cancelChanges: function() {
            _.defer(function() {
                mvnUrlColl.reset();
                fileUploadColl.reset();
            });
        },
        editToggleClicked: function(evt){
            this.model.set({isEditMode: !this.model.get('isEditMode')});
            this.$(evt.currentTarget).toggleClass('active');
        },
        modelChanged: function(evt){
            this.$(evt.currentTarget).toggleClass('active', this.model.get('isEditMode'));
            wreqr.vent.trigger('app-grid:edit-mode-toggled', this.model.get('isEditMode'));
        },
        toggleDisplayOptions: function(){
            var value = this.$('input[name="display-options"]:checked').val();
            $('.apps-grid').attr('list-type', value); // we should really make this event based.
        }

    });


    var BOX_LAYOUT = 0;
    var ROW_LAYOUT = 1;
    var ACTIVE_STATE = "ACTIVE";
    var INACTIVE_STATE = "INACTIVE";
    var STOP_STATE = "STOP";

    // Main layout view for all the applications
    var ApplicationView = Marionette.Layout.extend({
        template: 'applicationGrid',
        tagName: 'div',
        className: 'full-height well',
        regions: {
            applicationGridButtons: '#application-grid-buttons',
            appsgrid: '#apps-grid',
            appsgridInstalled: '.apps-grid-container.installed',
            appsgridNotInstalled: '.apps-grid-container.not-installed',
            applicationViewCancel: '#application-view-cancel'
        },
        events: {
            'click .btn.btn-success.start': 'startAppView',
            'click .btn.btn-primary.stop': 'stopAppView',
            'click .btn.btn-default.toggle': 'toggleClick',
            'click .btn.btn-success.install': 'installAppView',
            'click .btn.btn-primary.remove': 'removeAppView',
            'change input[name="options"]':'displayOptionChanged',
            'click .btn.btn-info.cancel': 'refreshView',
            'click button.stopAppConfirm': 'confirmStop',
            'click button.startAppConfirm': 'confirmStart',
            'click button.stopAppCancel': 'stopAppView',
            'click button.startAppCancel': 'startAppView'
        },
        initialize: function (options) {
            var self = this;

            this.modelClass = options.modelClass;
            this.showAddUpgradeBtn = options.showAddUpgradeBtn;
            if(this.modelClass) {
                Model.Collection = new this.modelClass.TreeNodeCollection();

                this.gridState = ACTIVE_STATE;
                this.gridLayout = BOX_LAYOUT;

                this.response = new this.modelClass.Response();
                this.model = Model.Collection;
                this.response.fetch({
                    success: function(model) {
                        self.model.set(model.get("value"));
                    }
                });
            }

            this.listenTo(wreqr.vent, 'app-grid:edit-mode-toggled', this.toggleEditMode);
        },
        onRender: function () {
            var view = this;

            _.defer(function() {
                view.appsgridInstalled.show(new AppCardCollectionView({collection: view.model, AppShowState: ACTIVE_STATE}));
                view.appsgridNotInstalled.show(new AppCardCollectionView({collection: view.model, AppShowState: INACTIVE_STATE}));
                view.applicationGridButtons.show(new NewApplicationView({response: view.response}));
                view.$('#application-grid-layout').perfectScrollbar();
            });

            this.listenTo(wreqr.vent, 'toggle:layout', this.toggleView);
        },
        // Default view of the applications
        refreshView: function() {
            wreqr.vent.trigger('toggle:state', ACTIVE_STATE);

            this.toggleState(ACTIVE_STATE);
            this.toggleView(this.gridLayout);
        },
        // Toggle used to change the layout of the applications
        toggleClick: function() {
            if(this.gridLayout === BOX_LAYOUT) {
                wreqr.vent.trigger('toggle:layout', ROW_LAYOUT);
            } else {
                wreqr.vent.trigger('toggle:layout', BOX_LAYOUT);
            }
        },
        // Performs action to change css class to alter the view of the applications
        toggleView: function(layout) {
            this.gridLayout = layout;
            if(layout === BOX_LAYOUT) {
                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            } else {
                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
        },
        // Changes what applications are shown based on the state requested
        // i.e.: Regular view of active applications
        //       Start view of applications that are prepackaged but not running
        //       Stop view of applications that are currently active that can be stopped
        toggleState: function(state) {
            this.gridState = state;
            if(state === ACTIVE_STATE) {
                $("a.fa.fa-times.stopApp").toggleClass("stopAppHide", true);
                $("a.fa.fa-download.startApp").toggleClass("startAppHide", true);
            } else if(state === INACTIVE_STATE) {
                $("a.fa.fa-times.stopApp").toggleClass("stopAppHide", true);
                $("a.fa.fa-download.startApp").toggleClass("startAppHide", false);
            } else {
                $("a.fa.fa-times.stopApp").toggleClass("stopAppHide", false);
                $("a.fa.fa-download.startApp").toggleClass("startAppHide", true);
            }
        },
        startAppView: function() {
            wreqr.vent.trigger('toggle:state', INACTIVE_STATE);

            this.toggleState(INACTIVE_STATE);
            this.toggleView(this.gridLayout);
        },
        stopAppView: function() {
            wreqr.vent.trigger('toggle:state', STOP_STATE);

            this.toggleState(STOP_STATE);
            this.toggleView(this.gridLayout);
        },
        confirmStop: function() {
            this.updateProgress("stop");
        },
        confirmStart: function() {
            this.updateProgress("start");
        },
        updateProgress: function(action) {
            var that = this;
            if(action === "start") {
                this.progressBarStartApp(function(message, percentage) {
                    that.$('.application-status').html(message);
                    that.$(".progress-bar").animate({width: percentage+'%'}, 0, 'swing');
                });
            } else {
                this.progressBarStopApp(function(message, percentage) {
                    that.$('.application-status').html(message);
                    that.$(".progress-bar").animate({width: percentage+'%'}, 0, 'swing');
                });
            }
        },
        progressBarStartApp: function (message) {
            var that = this;

            var jsonModel = this.model.toJSON();
            var numNodes = this.model.length;

            return this.model.update('start', this.response, message).then(function() {
                that.model.update('read', that.response, message).then(function() {
                    that.model.validateUpdate(jsonModel, numNodes, message, "start");
                    that.setErrorStates();
                    that.toggleView(that.gridLayout);
                });
            });
        },
        progressBarStopApp: function (message) {
            var that = this;

            var jsonModel = this.model.toJSON();
            var numNodes = this.model.length;

            return this.model.update('stop', this.response, message).then(function() {
                that.model.update('read', that.response, message).then(function() {
                    that.model.validateUpdate(jsonModel, numNodes, message, "stop");
                    that.setErrorStates();
                    that.toggleView(that.gridLayout);
                });
            });
        },
        setErrorStates: function() {
            var that = this;
            this.model.each(function(child) {
                if(child.get('error') === true) {
                    that.$('#'+child.get('appId')+'-name').css('color', 'red');
                }
            });
        },
        toggleEditMode: function(isEditMode){
            this.$el.toggleClass('edit-mode', isEditMode);
        }
    });

    return ApplicationView;

});
