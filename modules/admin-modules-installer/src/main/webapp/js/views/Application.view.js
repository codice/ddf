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
/** Main view page for add. */
define([
    'backbone',
    'marionette',
    '/installer/js/models/Applications.js',
    'icanhaz',
    'underscore',
    'text!/installer/templates/application.handlebars',
    'text!/installer/templates/applicationNode.handlebars',
    'text!/installer/templates/details.handlebars',
    'text!/installer/templates/applicationNew.handlebars',
    'text!/installer/templates/mvnUrlItem.handlebars',
    'text!/installer/templates/fileProgress.handlebars',
    'fileupload'
], function(Backbone, Marionette, AppModel, ich, _, applicationTemplate, applicationNodeTemplate, detailsTemplate, applicationNew, mvnItemTemplate, fileProgress) {
    "use strict";

    ich.addTemplate('applicationTemplate', applicationTemplate);
    ich.addTemplate('applicationNodeTemplate', applicationNodeTemplate);
    ich.addTemplate('detailsTemplate', detailsTemplate);
    ich.addTemplate('applicationNew', applicationNew);
    ich.addTemplate('mvnItemTemplate', mvnItemTemplate);
    ich.addTemplate('fileProgress', fileProgress);

    var applicationModel = new AppModel.TreeNodeCollection();

    var appResponse = new AppModel.Response();
    appResponse.fetch({
        success: function(model){
            applicationModel.set(model.get("value"));
        }
    });

    var mvnUrlColl = new AppModel.MvnUrlColl();
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
            },
            abort: function() {
                this.model.abort();
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
                            fileModel.abort = _.bind(data.abort, data);
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
            if(this.$("#urlField").val() !== '') {
                mvnUrlColl.add(new Backbone.Model({value: this.$("#urlField").val()}));
                this.$("#urlField").val('');
                this.setFocus();

            }
        },
        saveChanges: function() {
            mvnUrlColl.save().success(function() {
                mvnUrlColl.reset();
                appResponse.fetch({
                    success: function(model){
                        applicationModel.set(model.get("value"));
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

    // Recursive tree view
    var AppTreeView = Marionette.CompositeView.extend({
        template: 'applicationNodeTemplate',
        tagName: 'ul',
        className: 'app-node',

        initialize: function () {
            // grab all the child collections from the parent model
            // so that we can render the collection as children of
            // this parent model
            this.collection = this.model.get("children");
            this.modelBinder = new Backbone.ModelBinder();
        },

        events: {
            'mouseover .appitem': 'hoveringApp',
            'mouseout .appitem': 'leavingApp'
        },

        hoveringApp: function(e) {
            applicationModel.trigger("app:hover", this.model, e);
        },

        leavingApp: function() {
            applicationModel.trigger("app:hoverexit");
        },

        onRender: function () {
            this.bind();
        },

        bind: function () {
            //var bindings = {selected: '#' + this.model.get("name") + ' > [name=selected]'};
            var bindings = {selected: '#' + this.model.get("appId") + ' > [name=selected]'};
            this.modelBinder.bind(this.model, this.el, bindings);
        },

        appendHtml: function (collectionView, itemView) {
            // ensure we nest the child list inside of
            // the current list item
            collectionView.$("li:first").append(itemView.el);
        }
    });

    var TreeView = Marionette.CollectionView.extend({
        itemView: AppTreeView
    });


    var ApplicationView = Marionette.Layout.extend({
        template: 'applicationTemplate',
        tagName: 'div',
        className: 'full-height',
        model: applicationModel,
        regions: {
            applications: '#apps-tree',
            details: '#details',
            newApplication: '#new-app-container'
        },

        initialize: function (options) {
            var self = this;
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel, 'next', this.next);
            this.listenTo(this.navigationModel, 'previous', this.previous);
            this.listenTo(applicationModel, "app:hover", function(appSelected, e){
                // multiple hover events are fired for a given element (itself and it's parents)
                if (e.currentTarget.id.lastIndexOf(appSelected.get("appId")) === 0){
                    self.details.show(new DetailsView({model: appSelected}));
                }
            });
            this.listenTo(applicationModel, "app:hoverexit", function(){
                self.details.close();
            });
        },
        onRender: function () {
            var view = this;
            this.applications.show(new TreeView({collection: this.model}));
            this.newApplication.show(new NewApplicationView());
            _.defer(function() {
                view.$('#wrapper').perfectScrollbar();
            });
        },
        onClose: function () {
            this.stopListening(this.navigationModel);
            this.stopListening(applicationModel);
            this.$('#wrapper').perfectScrollbar('destroy');
        },
        next: function () {
        //leaving this code in here as a starting point for the person that implements this
//            var sleep = function(millis, callback) {
//                setTimeout(function() {
//                    callback();
//                }, millis);
//            };
//            var view = this;
//            //this is your hook to perform any validation you need to do before going to the next step
//            var install1 = function() {
//                view.navigationModel.trigger('block');
//                view.navigationModel.nextStep("Installing Catalog", 0);
//                sleep(1000, install2);
//            };
//            var install2 = function() {
//                view.navigationModel.nextStep("Installing Solr", 25);
//                sleep(1000, install3);
//            };
//            var install3 = function() {
//                view.navigationModel.nextStep("Installing UI", 50);
//                sleep(1000, install4);
//            };
//            var install4 = function() {
//                view.navigationModel.nextStep("Installing LDAP", 75);
//                sleep(1000, install5);
//            };
//            var install5 = function() {
//                view.navigationModel.nextStep("Finished App Install", 100);
//                view.navigationModel.trigger('unblock');
//            };
//            install1();

            this.navigationModel.nextStep();
        },
        previous: function () {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        }
    });

    var DetailsView = Marionette.ItemView.extend({
        template: 'detailsTemplate',
        tagName: 'div'
    });

    return ApplicationView;

});