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
    'text!applicationNew',
    'text!mvnItemTemplate',
    'text!fileProgress',
    'text!applicationOutlineButtons',
    'text!applicationInfo',
    'text!applicationGrid',
    '/applications/js/wreqr.js',
    'fileupload',
    'perfectscrollbar'
], function(require, Backbone, Marionette, ich, _, $, applicationNew, mvnItemTemplate, fileProgress,
            applicationOutlineButtons, applicationInfo, applicationGrid, wreqr) {
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
    if(!ich.applicationInfo) {
        ich.addTemplate('applicationInfo', applicationInfo);
    }
    if(!ich.applicationGrid) {
        ich.addTemplate('applicationGrid', applicationGrid);
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
            'hidden.bs.modal': 'cancelChanges'
        },
        initialize: function(options) {
            this.response = options.response;
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
        }
    });

    // List of apps that cannot have any actions performed on them through
    // the applications module
    var disableList = [
        'platform-app',
        'admin-app'
    ];

    // Itemview for each individual application
    var AppInfoView = Marionette.ItemView.extend({
        template: 'applicationInfo',
        tagName: 'div',
        className: 'appInfo row box',
        itemViewOptions: {},
        events: {
            'click .fa.fa-times.stopApp': 'stopMessage',
            'click .fa.fa-download.startApp': 'startMessage',
            'click .stopAppConfirm': 'stopPrompt',
            'click .startAppConfirm': 'startPrompt'
        },
        initialize: function () {
            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function () {
            this.bind();
        },
        bind: function () {
            var bindings = {};
            this.modelBinder.bind(this.model, this.el, bindings);
        },
        // Will disable functionality for certain applications
        serializeData: function () {
            var that = this;
            var disable = false;
            disableList.forEach(function(child) {
                if(that.model.get('appId') === child) {
                    disable = true;
                }
            });

            if(disable === true) {
                return _.extend(this.model.toJSON(), {isDisabled: true});
            } else {
                return _.extend(this.model.toJSON(), {isDisabled: false});
            }
        },
        // Creates a message that gets displayed on the stop prompt displaying
        // any dependent applications that will also be stopped in the process
        stopMessage: function() {
            var that = this;
            var children = this.model.get('dependencies');
            var stopMessage = [];

            if(children.length !== 0) {
                children.forEach(function(child) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.get('appId') === child) &&
                            (modelChild.get('state') === 'ACTIVE')) {
                            stopMessage.push(child);
                        }
                    });
                });
                this.model.set({stopMessage: stopMessage});
            }
        },
        // Creates a message that gets displayed on the start prompt displaying
        // any parent applications that will also be started in the process
        startMessage: function() {
            var that = this;
            var parents = this.model.get('parents');
            var startMessage = [];

            if(parents.length !== 0) {
                parents.forEach(function(parent) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.get('appId') === parent) &&
                            (modelChild.get('state') === 'INACTIVE')) {
                            startMessage.push(parent);
                        }
                    });
                });
                this.model.set({startMessage: startMessage});
            }
        },
        // Only toggle the flag if the stop action is confirmed
        stopPrompt: function() {
            this.stopMessage();
            this.model.toggleChosenApp();
        },
        // Only toggle the flag if the start action is confirmed
        startPrompt: function() {
            this.startMessage();
            this.model.toggleChosenApp();
        }
    });

    // Collection of all the applications
    var AppInfoCollectionView = Marionette.CollectionView.extend({
        itemView: AppInfoView,
        className: 'apps-grid',
        itemViewOptions: {},
        events: {
            'click .fa.fa-times.stopApp': 'stopPrompt',
            'click .fa.fa-download.startApp': 'startPrompt'
        },
        modelEvents: {
            'change': 'render'
        },
        initialize: function(options) {
            this.AppShowState = options.AppShowState;
            this.listenTo(wreqr.vent, 'toggle:layout', this.toggleLayout);
            this.listenTo(wreqr.vent, 'toggle:state', this.toggleState);
        },
        // Shows the applications in the proper state upon a re-render
        showCollection: function(){
            this.collection.each(function(item, index){
                if(this.AppShowState === item.get('state')) {
                    this.addItemView(item, AppInfoView, index);
                }
            }, this);
        },
        addChildView: function(item, collection, options){
            if(this.AppShowState === item.get('state')) {

            this.closeEmptyView();
            var ItemView = this.getItemView();
            return this.addItemView(item, ItemView, options.index);
            }
        },
        stopPrompt: function() {
            this.toggleState(STOP_STATE);
            this.render();
        },
        startPrompt: function() {
            this.toggleState(INACTIVE_STATE);
            this.render();
        },
        // Changes the css layout
        toggleLayout: function(layout) {
            if(layout === BOX_LAYOUT) {
                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            } else {
                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
        },
        // Keeps track of the current view of applications
        toggleState: function(state) {
            if(state === STOP_STATE) {
                this.AppShowState = ACTIVE_STATE;
            } else {
                this.AppShowState = state;
            }
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
        className: 'full-height',
        regions: {
            applicationGridButtons: '#application-grid-buttons',
            appsgrid: '#apps-grid',
            applicationViewCancel: '#application-view-cancel'
        },
        events: {
            'click .btn.btn-success.start': 'startAppView',
            'click .btn.btn-primary.stop': 'stopAppView',
            'click .btn.btn-default.toggle': 'toggleClick',
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
        },
        onRender: function () {
            var view = this;

            _.defer(function() {
                view.appsgrid.show(new AppInfoCollectionView({collection: view.model, AppShowState: ACTIVE_STATE}));
                view.applicationGridButtons.show(new NewApplicationView({response: view.response}));
                view.$('#application-grid-layout').perfectScrollbar();
            });

            this.listenTo(wreqr.vent, 'toggle:layout', this.toggleView);
        },
        // Default view of the applications
        refreshView: function() {
            wreqr.vent.trigger('toggle:state', ACTIVE_STATE);
            this.appsgrid.currentView.render();

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
            this.appsgrid.currentView.render();

            this.toggleState(INACTIVE_STATE);
            this.toggleView(this.gridLayout);
        },
        stopAppView: function() {
            wreqr.vent.trigger('toggle:state', STOP_STATE);
            this.appsgrid.currentView.render();

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
                    that.appsgrid.currentView.AppShowState = ACTIVE_STATE;
                    that.appsgrid.currentView.render();
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
        }
    });

    return ApplicationView;

});
