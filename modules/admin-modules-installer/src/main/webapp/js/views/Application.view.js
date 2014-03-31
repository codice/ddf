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
define(function (require) {

    var Backbone = require('backbone'),
        Marionette = require('marionette'),
        AppModel = require('/installer/js/models/Applications.js'),
        ich = require('icanhaz');

    ich.addTemplate('applicationTemplate', require('text!/installer/templates/application.handlebars'));
    ich.addTemplate('applicationNodeTemplate', require('text!/installer/templates/applicationNode.handlebars'));

//    var sampleAppData = [
//        {
//            application: {name: "application-1", version: "1.0.0", description: "Application 1 description"},
//            children: [{
//                application: {name: "application-1a", version: "1.0.0", description: "Application 1a description"},
//                children: [],
//                parent: []}],
//            parent: [{}]
//        },
//        {
//            application: {name: "application-2", version: "1.0.0", description: "Application 2 description"},
//            children: [
//                {
//                    application: {name: "application-2a", version: "1.0.0", description: "Application 2a description"},
//                    children: [],
//                    parent: []
//                },
//                {
//                    application: {name: "application-2b", version: "1.0.0", description: "Application 2b description"},
//                    children: [],
//                    parent: []
//                }
//            ],
//            parent: []
//        }
//    ];

    var sampleAppData = [
        {
            "description": "DDF Administration Tools",
            "name": "admin-app",
            "children": [],
            "version": "1.0.0.ALPHA1-SNAPSHOT"
        },
        {
            "description": null,
            "name": "ddf-231-SNAPSHOT",
            "children": [],
            "version": "0.0.0"
        },
        {
            "description": "OpenDJ Embedded LDAP",
            "name": "opendj-embedded",
            "children": [],
            "version": "1.0.0"
        },
        {
            "description": "DDF platform boot features",
            "name": "platform-app",
            "children": [
                {
                    "description": "DDF Catalog application default installations",
                    "name": "catalog-app",
                    "children": [
                        {
                            "description": "DDF Content application default installations",
                            "name": "content-app",
                            "children": [],
                            "version": "2.3.1-SNAPSHOT"
                        },
                        {
                            "description": "UI for searching over DDF. Contains a standard version (3D globe) and a simple version (text page).",
                            "name": "search-ui-app",
                            "children": [],
                            "version": "2.3.1.ALPHA2-SNAPSHOT"
                        },
                        {
                            "description": "Catalog Provider with locally Embedded Solr Server, implemented using Solr 4.6.0.",
                            "name": "solr-app",
                            "children": [],
                            "version": "2.4.0-SNAPSHOT"
                        },
                        {
                            "description": "DDF Spatial Services application default installations",
                            "name": "spatial-app",
                            "children": [],
                            "version": "2.3.1-SNAPSHOT"
                        }
                    ],
                    "version": "2.3.1.ALPHA2-SNAPSHOT"
                },
                {
                    "description": "HTTP Proxy",
                    "name": "codice-httpproxy",
                    "children": [],
                    "version": "1.0.0.ALPHA1"
                },
                {
                    "description": "DDF Security Services application default installations",
                    "name": "security-services-app",
                    "children": [],
                    "version": "2.3.1-SNAPSHOT"
                }
            ],
            "version": "2.3.1-SNAPSHOT"
        }
    ];

    // Recursive tree view
    var AppTreeView = Marionette.CompositeView.extend({
        template: 'applicationNodeTemplate',
        tagName: 'ul',
        className: 'app-node',

        initialize: function() {
            "use strict";
            // grab all the child collections from the parent model
            // so that we can render the collection as children of
            // this parent model
            this.collection = this.model.get("children");
            this.modelBinder = new Backbone.ModelBinder();
        },

        onRender: function(){
            this.bind();
        },

        bind: function(){
            var bindings = {selected: '#'+this.model.get("name")+' > [name=selected]'};
            this.modelBinder.bind(this.model, this.el, bindings);
        },
        appendHtml: function(collectionView, itemView) {
            "use strict";
            // ensure we nest the child list inside of
            // the current list item
            collectionView.$("li:first").append(itemView.el);
        }
    });

//    var ItemView = Marionette.ItemView.extend({
//        template: 'applicationNodeTemplate',
//        tagName: 'ul'
//    });

    var TreeView = Marionette.CollectionView.extend({
        itemView: AppTreeView
    });


    var ApplicationView = Marionette.Layout.extend({
        template: 'applicationTemplate',
        tagName: 'div',
        className: 'full-height',
        model: new AppModel.TreeNodeCollection(sampleAppData),
        regions: {
            applications: '#applications'
        },

        initialize: function(options) {
            this.navigationModel = options.navigationModel;
            this.listenTo(this.navigationModel,'next', this.next);
            this.listenTo(this.navigationModel,'previous', this.previous);
        },
        onRender: function() {
            "use strict";
            console.log("I'm in render");
            this.applications.show(new TreeView({collection: this.model}));
        },
        onClose: function() {
            this.stopListening(this.navigationModel);
        },
        next: function() {
            //this is your hook to perform any validation you need to do before going to the next step
            this.navigationModel.nextStep();
        },
        previous: function() {
            //this is your hook to perform any teardown that must be done before going to the previous step
            this.navigationModel.previousStep();
        }
    });

    return ApplicationView;

});