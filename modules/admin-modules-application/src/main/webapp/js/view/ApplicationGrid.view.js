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
    'fileupload',
    'perfectscrollbar'
], function(require, Backbone, Marionette, ich, _, $, applicationNew, mvnItemTemplate, fileProgress,
            applicationOutlineButtons, applicationInfo, applicationGrid) {
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

    var disableList = [
        'platform-app',
        'admin-app'
    ];

    // Recursive tree view
    var AppInfoView = Marionette.ItemView.extend({
        template: 'applicationInfo',
        tagName: 'div',
        className: 'appInfo row box',
        itemViewOptions: {},
        events: {
            'click .fa.fa-times.removeApp': 'removeMessage',
            'click .fa.fa-download.installApp': 'installMessage',
            'click .removeConfirm': 'removePrompt',
            'click .installConfirm': 'installPrompt'
        },

        initialize: function () {
            // grab all the child collections from the parent model
            // so that we can render the collection as children of
            // this parent model

            this.modelBinder = new Backbone.ModelBinder();
        },
        onRender: function () {
            this.bind();
        },
        bind: function () {
            var bindings = {};
            this.modelBinder.bind(this.model, this.el, bindings);
        },
        serializeData: function () {
            var that = this;
            disableList.forEach(function(child) {
                if(that.model.attributes.appId === child) {
                    return _.extend(that.model.toJSON(), {isDisabled: true});
                }
            });

            return _.extend(this.model.toJSON(), {isDisabled: false});
        },
        removeMessage: function() {
            var that = this;
            var children = this.model.attributes.dependencies;
            var removeMessage = [];

            if(children.length !== 0) {
                children.forEach(function(child) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.attributes.appId === child) &&
                            (modelChild.attributes.state === 'ACTIVE')) {
                            removeMessage.push(child);
                        }
                    });
                });
                this.model.set({removeMessage: removeMessage});
            }
        },
        installMessage: function() {
            var that = this;
            var parents = this.model.attributes.parents;
            var installMessage = [];

            if(parents.length !== 0) {
                parents.forEach(function(parent) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.attributes.appId === parent) &&
                            (modelChild.attributes.state === 'INACTIVE')) {
                            installMessage.push(parent);
                        }
                    });
                });
                this.model.set({installMessage: installMessage});
            }
        },
        removePrompt: function() {
            var that = this;
            var children = this.model.attributes.dependencies;
            var removeMessage = [];

            if(children.length !== 0) {
                children.forEach(function(child) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.attributes.appId === child) &&
                            (modelChild.attributes.state === 'ACTIVE')) {
                            removeMessage.push(child);
                            modelChild.flagRemove();
                        }
                    });
                });
                this.model.set({removeMessage: removeMessage});
            }
            this.model.flagRemove();
        },
        installPrompt: function() {
            var that = this;
            var parents = this.model.attributes.parents;
            var installMessage = [];

            if(parents.length !== 0) {
                parents.forEach(function(parent) {
                    that.model.collection.each(function(modelChild) {
                        if((modelChild.attributes.appId === parent) &&
                            (modelChild.attributes.state === 'INACTIVE')) {
                            installMessage.push(parent);
                            modelChild.flagRemove();
                        }
                    });
                });
                this.model.set({installMessage: installMessage});
            }
            this.model.flagRemove();
        }
    });

    var AppInfoCollectionView = Marionette.CollectionView.extend({
        itemView: AppInfoView,
        className: 'apps-grid',
        itemViewOptions: {},
        events: {
            'click .fa.fa-times.removeApp': 'removePrompt',
            'click .fa.fa-download.installApp': 'installPrompt'
        },
        modelEvents: {
            'change': 'render'
        },

        initialize: function(options) {
            this.AppShowState = options.AppShowState;
            this.AppShowLayout = options.AppShowLayout;
        },
        showCollection: function(){
            this.collection.each(function(item, index){
                if(this.AppShowState === item.attributes.state) {
                    this.addItemView(item, AppInfoView, index);
                }
            }, this);
            this.toggleLayout();
        },
        addChildView: function(item, collection, options){
            if(this.AppShowState === item.attributes.state) {

            this.closeEmptyView();
            var ItemView = this.getItemView();
            return this.addItemView(item, ItemView, options.index);
            }
        },
        removePrompt: function() {
            this.render();
            this.toggleLayout();
        },
        installPrompt: function() {
            this.render();
            this.toggleLayout();
        },
        toggleLayout: function() {
            if(this.AppShowLayout === BOX_LAYOUT) {
                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            } else {
                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
        }
    });

    var BOX_LAYOUT = 0;
    var ROW_LAYOUT = 1;
    var ACTIVE_STATE = "ACTIVE";
    var INACTIVE_STATE = "INACTIVE";

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
            'click .btn.btn-success.install': 'installAppView',
            'click .btn.btn-primary.remove': 'removeAppView',
            'click .btn.btn-default.toggle': 'toggleView',
            'click .btn.btn-info.cancel': 'refreshView',
            'click button.removeConfirm': 'confirmRemove',
            'click button.installConfirm': 'confirmInstall'
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
                view.appsgrid.show(new AppInfoCollectionView({collection: view.model, AppShowState: ACTIVE_STATE, AppShowLayout: BOX_LAYOUT}));
                view.applicationGridButtons.show(new NewApplicationView({response: view.response}));
                view.$('#application-grid-layout').perfectScrollbar();
            });
        },
        refreshView: function() {
            this.appsgrid.currentView.AppShowLayout = BOX_LAYOUT;
            this.appsgrid.currentView.AppShowState = ACTIVE_STATE;
            this.appsgrid.currentView.render();
            this.gridLayout = BOX_LAYOUT;
            this.gridState = ACTIVE_STATE;

            $("a.fa.fa-times.removeApp").toggleClass("removeHide", true);
            $("a.fa.fa-download.installApp").toggleClass("installHide", true);
            $("a.btn.btn-primary.btn-sm.descriptionButton").toggleClass("descriptionButtonHide", true);
        },
        toggleView: function() {
            if(this.gridLayout === BOX_LAYOUT) {
                this.gridLayout = ROW_LAYOUT;
                this.appsgrid.currentView.AppShowLayout = ROW_LAYOUT;

                this.$("h2").toggleClass("boxDescription", false);
                $("div.appInfo").toggleClass("box", false);
            }
            else {
                this.gridLayout = BOX_LAYOUT;
                this.appsgrid.currentView.AppShowLayout = BOX_LAYOUT;

                this.$("h2").toggleClass("boxDescription", true);
                $("div.appInfo").toggleClass("box", true);
            }
        },
        installAppView: function() {
            this.appsgrid.currentView.AppShowState = INACTIVE_STATE;
            this.appsgrid.currentView.render();
            this.gridState = INACTIVE_STATE;
            if(this.gridLayout === ROW_LAYOUT) {
                this.toggleView();
                this.toggleView();
            }

            $("a.fa.fa-times.removeApp").toggleClass("removeHide", true);
            $("a.fa.fa-download.installApp").toggleClass("installHide", false);
        },
        removeAppView: function() {
            this.appsgrid.currentView.AppShowState = ACTIVE_STATE;
            this.appsgrid.currentView.render();
            this.gridState = ACTIVE_STATE;
            if(this.gridLayout === ROW_LAYOUT) {
                this.toggleView();
                this.toggleView();
            }

            $("a.fa.fa-times.removeApp").toggleClass("removeHide", false);
            $("a.fa.fa-download.installApp").toggleClass("installHide", true);
        },
        confirmRemove: function() {
            this.updateProgress("remove");
        },
        confirmInstall: function() {
            this.updateProgress("install");
        },
        updateProgress: function(action) {
            var that = this;
            if(action === "install") {
                this.progressBarInstall(function(message, percentage) {
                    that.$('.application-status').html(message);
                    that.$(".progress-bar").animate({width: percentage+'%'}, 0, 'swing');
                });
            } else {
                this.progressBarRemove(function(message, percentage) {
                    that.$('.application-status').html(message);
                    that.$(".progress-bar").animate({width: percentage+'%'}, 0, 'swing');
                });
            }
        },
        progressBarInstall: function (message) {
            var that = this;

            var jsonModel = this.model.toJSON();
            var numNodes = this.model.length;

            return this.model.update('install', this.response, message).then(function() {
                that.model.update('read', that.response, message).then(function() {
                    that.model.validateUpdate(jsonModel, numNodes, message, "install");
                    that.setErrorStates();
                    that.appsgrid.currentView.AppShowState = ACTIVE_STATE;
                    that.appsgrid.currentView.render();
                    if(that.gridLayout === ROW_LAYOUT) {
                        that.toggleView();
                        that.toggleView();
                    }
                });
            });
        },
        progressBarRemove: function (message) {
            var that = this;

            var jsonModel = this.model.toJSON();
            var numNodes = this.model.length;

            return this.model.update('remove', this.response, message).then(function() {
                that.model.update('read', that.response, message).then(function() {
                    that.model.validateUpdate(jsonModel, numNodes, message, "remove");
                    that.setErrorStates();
                    if(that.gridLayout === ROW_LAYOUT) {
                        that.toggleView();
                        that.toggleView();
                    }
                });
            });
        },
        setErrorStates: function() {
            var that = this;
            this.model.each(function(child) {
                if(child.attributes.error === true) {
                    that.$('#'+child.attributes.appId+'-name').css('color', 'red');
                }
            });
        }
    });

    return ApplicationView;

});
