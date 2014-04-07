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
    'text!/installer/templates/application.handlebars',
    'text!/installer/templates/applicationNode.handlebars',
    'text!/installer/templates/details.handlebars'
], function(Backbone, Marionette, AppModel, ich, applicationTemplate, applicationNodeTemplate, detailsTemplate) {
    "use strict";

    ich.addTemplate('applicationTemplate', applicationTemplate);
    ich.addTemplate('applicationNodeTemplate', applicationNodeTemplate);
    ich.addTemplate('detailsTemplate', detailsTemplate);

    var applicationModel = new AppModel.TreeNodeCollection();

    var appResponse = new AppModel.Response();
    appResponse.fetch({
        success: function(model){
            applicationModel.set(model.get("value"));
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
            var bindings = {selected: '#' + this.model.get("name") + ' > [name=selected]'};
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
            details: '#details'
        },

        initialize: function (options) {
            var self = this;
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel, 'next', this.next);
            this.listenTo(this.navigationModel, 'previous', this.previous);
            this.listenTo(applicationModel, "app:hover", function(appSelected, e){
                // verify we have model that matches the selected target (either the checkbox or text)
                if (e.currentTarget.id.lastIndexOf(appSelected.get("name")) === 0){
                    self.details.show(new DetailsView({model: appSelected}));
                }
            });
            this.listenTo(applicationModel, "app:hoverexit", function(){
                self.details.close();
            });
        },
        onRender: function () {
            this.applications.show(new TreeView({collection: this.model}));
        },
        onClose: function () {
            this.stopListening(this.navigationModel);
            this.stopListening(applicationModel);
        },
        next: function () {
            //this is your hook to perform any validation you need to do before going to the next step
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