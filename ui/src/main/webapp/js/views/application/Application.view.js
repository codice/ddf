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
    'text!applicationTemplate',
    'text!applicationNodeTemplate',
    'text!detailsTemplate',
    'text!applicationNew',
    'text!mvnItemTemplate',
    'text!fileProgress',
    'fileupload',
    'perfectscrollbar'
], function(require, Backbone, Marionette, ich, _, $, applicationTemplate, applicationNodeTemplate, detailsTemplate, applicationNew, mvnItemTemplate, fileProgress) {
    "use strict";

    if(!ich.applicationTemplate) {
        ich.addTemplate('applicationTemplate', applicationTemplate);
    }
    if(!ich.applicationNodeTemplate) {
        ich.addTemplate('applicationNodeTemplate', applicationNodeTemplate);
    }
    if(!ich.detailsTemplate) {
        ich.addTemplate('detailsTemplate', detailsTemplate);
    }
    if(!ich.applicationNew) {
        ich.addTemplate('applicationNew', applicationNew);
    }
    if(!ich.mvnItemTemplate) {
        ich.addTemplate('mvnItemTemplate', mvnItemTemplate);
    }
    if(!ich.fileProgress) {
        ich.addTemplate('fileProgress', fileProgress);
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
        template: 'applicationNew',
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

    //hard coded list of applications to disable selection for
    var disableList = [
        'platform-app',
        'admin-app'
    ];

    // Recursive tree view
    var AppTreeView = Marionette.CompositeView.extend({
        template: 'applicationNodeTemplate',
        tagName: 'ul',
        className: 'app-node',
        itemViewOptions: {},

        initialize: function (options) {
            // grab all the child collections from the parent model
            // so that we can render the collection as children of
            // this parent model
            this.collection = this.model.get("children");
            this.modelBinder = new Backbone.ModelBinder();

            // Set the enableApplicationRemoval flag for the current tree node
            this.enableApplicationRemoval = options.enableApplicationRemoval;

            // Add the enableApplicationRemoval flag to the itemViewOptions so
            // that it will be passed to ItemViews constructed for child nodes
            this.itemViewOptions.enableApplicationRemoval = options.enableApplicationRemoval;
        },

        events: {
            'mouseover .appitem': 'hoveringApp',
            'mouseout .appitem': 'leavingApp' ,
            'click input.appitem': 'toggleSelectedFlag',
            'click a.fa-minus-circle': 'toggleRemoveFlag'
        },

        hoveringApp: function(e) {
            Model.Collection.trigger("app:hover", this.model, e);
        },

        isAppRemovalEnabled: function() {
            if (this.enableApplicationRemoval) {
                return true;
            }

            return false;
        },

        leavingApp: function() {
            Model.Collection.trigger("app:hoverexit");
        },

        toggleSelectedFlag: function() {
            this.model.updateModel();
        },

        toggleRemoveFlag: function(toggleEvent) {
            toggleEvent.preventDefault();

            if (this.isAppRemovalEnabled() && !this.$el.hasClass('disabled')) {
                this.model.toggleRemoveFlag();
            }
        },

        // Add data from the view to the model data that is passed to
        // the template.
        serializeData: function () {
            // Get the data from the model
            var data = Backbone.Marionette.CompositeView.prototype.serializeData.apply(this);

            // Add view-specific data
            if (this.isAppRemovalEnabled()) {
                data.showRemoveBtn = true;
            }

            return data;
        },

        onRender: function () {
            this.bind();

            var appId = this.model.get('appId');
            if(_.indexOf(disableList, appId) !== -1) {
                this.$('#' + appId + 'cb').attr('disabled', true);
                this.$('#' + appId + '-delete-icon').addClass('fa-minus-circle-disabled');
                this.$el.addClass('disabled');
            }
        },

        bind: function () {
            var removeFlagConverter = function (direction, value) {
                // if removeFlag is true, add the "fa-minus-circle-selected"
                // class to the element's class list, otherwise remove it
                return value ? "fa-minus-circle-selected" : "";
            };

            var startStopCheckboxSelector = '#' + this.model.get("appId") + ' > [name=selected]';
            var bindings = {};
            bindings.selected = startStopCheckboxSelector;

            if (this.isAppRemovalEnabled()) {
                bindings.removeFlag = [{ selector: '[name=removeFlag]',
                                         elAttribute: 'class',
                                         converter: removeFlagConverter },
                                       // When removeFlag is true, set the
                                       // checkbox disabled property to true
                                       { selector: startStopCheckboxSelector,
                                         elAttribute: 'disabled' }];
            }

            this.modelBinder.bind(this.model, this.el, bindings);
        },

        appendHtml: function (collectionView, itemView) {
            // ensure we nest the child list inside of
            // the current list item
            collectionView.$("li:first").append(itemView.el);
        }
    });

    var TreeView = Marionette.CollectionView.extend({
        itemView: AppTreeView,

        itemViewOptions: {},

        initialize: function(options) {
            // Pass the enableApplicationRemoval flag to the AppTreeView
            // initialize function by adding it to the itemViewOptions
            this.itemViewOptions.enableApplicationRemoval = options.enableApplicationRemoval;
            this.itemViewOptions.navigationModel = options.navigationModel;

        }
    });

    var setErrorState = function (view, model) {
        if(model.get('error')) {
            view.$('#'+model.get('appId')).css('color', 'red');
        }
        if(model.get('children')) {
            model.get('children').each(function(child) {
                setErrorState(view, child);
            });
        }
    };

    var ApplicationView = Marionette.Layout.extend({
        template: 'applicationTemplate',
        tagName: 'div',
        className: 'full-height',
        regions: {
            applications: '#apps-tree',
            details: '#details',
            newApplication: '#new-app-container'
        },

        initialize: function (options) {
            var self = this;
            this.selectedProfile = options.selectedProfile;
            this.modelClass = options.modelClass;
            this.enableApplicationRemoval = options.enableApplicationRemoval;
            this.showAddUpgradeBtn = options.showAddUpgradeBtn;
            if(this.modelClass) {
                Model.Collection = new this.modelClass.TreeNodeCollection();

                this.response = new this.modelClass.Response();
                this.model = Model.Collection;
                this.response.fetch({
                    success: function(model) {
                        self.model.set(model.get("value"));
                        self.model.setSelectedBasedOnProfile(self.selectedProfile);
                        self.trigger('collection:loaded');
                    }
                });
            }
            this.listenTo(this.model, "app:hover", function(appSelected, e){
                // multiple hover events are fired for a given element (itself and it's parents)
                if (e.currentTarget.id.lastIndexOf(appSelected.get("appId")) === 0){
                    self.details.show(new DetailsView({model: appSelected}));
                }
            });
            this.listenTo(this.model, "app:hoverexit", function(){
                self.details.close();
            });
        },
        onRender: function () {
            var view = this;

            _.defer(function() {
                view.applications.show(new TreeView({collection: view.model, enableApplicationRemoval: view.enableApplicationRemoval}));
                if(view.showAddUpgradeBtn !== false) {
                    view.newApplication.show(new NewApplicationView({response: view.response}));
                }
                view.$('#wrapper').perfectScrollbar();
            });
        },
        onClose: function () {
            this.stopListening(this.model);
            this.$('#wrapper').perfectScrollbar('destroy');
        },
        countSelected: function(treenodes) {
            var count = 0;
            var view = this;
            _.each(treenodes, function(node) {
                if (node.get("selected")) {
                    count++;
                    var children = node.get("children");
                    if (children.length > 0){
                        count += view.countSelected(children);
                    }
                }
            });
            return count;
        },
        installApp: function(statusFunc){
            var that = this;

            //save off the model before we make any chances
            var jsonModel = this.model.toJSON();
            var numNodes = this.model.numNodesChanged();

            // Update each application based on the user selections
            return this.model.sync('update', this.response, statusFunc).then(function() {
                // Update from the server
                that.model.sync('read', that.response, statusFunc).then(function() {
                    // Check if anything failed
                    that.model.validateInstall(jsonModel, numNodes, statusFunc);
                    that.setErrorStates();
                });
            });
        },
        setErrorStates: function() {
            var that = this;
            this.model.each(function(child) {
                setErrorState(that, child);
            });
        }
    });

    var DetailsView = Marionette.ItemView.extend({
        template: 'detailsTemplate',
        tagName: 'div'
    });

    return ApplicationView;

});
