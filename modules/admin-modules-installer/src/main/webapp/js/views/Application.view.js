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
    'underscore',
    '/installer/js/models/Applications.js',
    'icanhaz',
    'text!/installer/templates/application.handlebars',
    'text!/installer/templates/applicationNode.handlebars',
    'text!/installer/templates/details.handlebars',
    'text!/installer/templates/applicationNew.handlebars',
    'text!/installer/templates/mvnUrlItem.handlebars'
], function(Backbone, Marionette, _, AppModel, ich, applicationTemplate,
            applicationNodeTemplate, detailsTemplate, applicationNew, mvnItemTemplate) {
    "use strict";

    ich.addTemplate('applicationTemplate', applicationTemplate);
    ich.addTemplate('applicationNodeTemplate', applicationNodeTemplate);
    ich.addTemplate('detailsTemplate', detailsTemplate);
    ich.addTemplate('applicationNew', applicationNew);
    ich.addTemplate('mvnItemTemplate', mvnItemTemplate);

    var applicationModel = new AppModel.TreeNodeCollection();

    var appResponse = new AppModel.Response();
    appResponse.fetch({
        success: function(model){
            applicationModel.set(model.get("value"));
        }
    });

    var mvnUrlColl = new AppModel.MvnUrlColl();

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

    var NewApplicationView = Marionette.Layout.extend({
        template: 'applicationNew',
        regions: {
            urlContainer: '#urlContainer'
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
            _.defer(function() {
                view.$('#app-upload-btn').fileupload({
                    url: '',
                    dataType: 'json'
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
        },
        cancelChanges: function() {
            mvnUrlColl.reset();
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

        next: function () {
            var view = this;
            view.navigationModel.trigger('block');

            view.installApp();

        //leaving this code in here as a starting point for the person that implements this

            view.navigationModel.trigger('unblock');
            this.navigationModel.nextStep();
        },
        previous: function () {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        },

        installApp: function(){
            var view = this;
            // Update each application based on the user selections
            applicationModel.sync('update', applicationModel, {statusUpdate: view.navigationModel.nextStep});

            // Update the view of the application tree based on the results of the previous save
            applicationModel.fetch();
            this.navigationModel.nextStep();
        }
    });

    var DetailsView = Marionette.ItemView.extend({
        template: 'detailsTemplate',
        tagName: 'div'
    });

    return ApplicationView;

});