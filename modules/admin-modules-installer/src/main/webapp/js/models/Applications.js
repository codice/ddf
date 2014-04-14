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
define(function (require) {
    "use strict";

    var Backbone = require('backbone'),
        $ = require('jquery'),
        _ = require('underscore');
    var Applications = {};

    Applications.MvnUrlColl = Backbone.Collection.extend({
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
    var startUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/startApplication/';
    var stopUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/stopApplication/';

    var versionRegex = /([^0-9]*)([0-9]+.*$)/;

    // Applications.TreeNode
    // ---------------------

    // Represents a node in the application tree where children are dependent on their parent being
    // installed. This node can have zero or more children (which are also 'Applications.Treenode`
    // nodes themselves).
    Applications.TreeNode = Backbone.Model.extend({
       defaults: function() {
            return {
                selected: false
            };
       },

       initialize: function(){
           var children = this.get("children");
           var that = this;

           // Some (not properly created) applications features file result in a name that includes the
           // version number - strip that off and move it into the version number.
           this.massageVersionNumbers();
           this.cleanupDisplayName();
           this.updateName();

           // Reflect the current state of the application in the model and keep the
           // state to determine if the user changes it.
           this.set({currentState: this.get("state") === "ACTIVE"});
           this.set({selected: this.get("currentState")});

           // Change the children from json representation to models and include a link
           // in each to their parent.
           if (children){
               this.set({children: new Applications.TreeNodeCollection(children)});
               this.get("children").forEach(function (child) {
                   child.set({parent: that});
               });
           }
           this.listenTo(this, "change", this.updateModel);
       },

       // When the user selects or deselects an application, adjust the rest of the
       // model accordingly - deselects propagate down, selects propagate up.
       updateModel: function(){
         if (this.get("selected")) {
             if (this.get("parent")) {
             this.get("parent").set({selected: true});
             }
         } else if (this.get("children").length){
             this.get("children").forEach(function(child) {
                 child.set({selected: false});
             });
         }
       },

        // Since the name is used for ids in the DOM, remove any periods
        // that might exist - but store in a separate attribute since we need the
        // original name to control the application via the application-service.
        updateName: function() {
            //this.set({name: this.get("name").replace(/\./g,'')});
            this.set({appId: this.get("name").replace(/\./g,'')});
        },

        // Some apps come in having the version number included
        // as part of the app name - e.g. search-app-2.3.1.ALPHA3-SNAPSHOT.
        // This function strips the version from the display name and
        // places it in the version variable so the details show correctly.
        massageVersionNumbers: function() {
            this.set({displayName: this.get("name")});
            if (this.get("version") === "0.0.0") {
                var matches = this.get("name").match(versionRegex);
                if (matches.length === 3) {
                    this.set({displayName: matches[1]});
                    this.set({version: matches[2]});
                }
            }
        },

        // Create a name suitable for display from the application name - camel-case
        // it and remove the dashes.
        cleanupDisplayName: function(){
            var tempName = this.get("displayName"); //.replace(/\./g,'');
            var names = tempName.split('-');
            var dispName = "";
            var that = this;
            _.each(names, function(name) {
                if (dispName.length > 0) {
                    dispName = dispName + " ";
                }
                dispName = dispName + that.capitalizeFirstLetter(name);
            });
            this.set({displayName: dispName});
        },

        // Capitalize and return the first letter of the given string.
       capitalizeFirstLetter: function(string){
           if (string && string !== ""){
               return string.charAt(0).toUpperCase() + string.slice(1);
           }
            return string;
       },

        // Determines whether the user has changed the selection of this model or
        // not - does not check its children.
        isDirty: function() {
            return (this.get("selected") !== this.get("currentState"));
        },

        // Returns the total number of applications that the user has changed
        // the selection status of - includes this node and all of its children.
        countDirty: function() {
            var count = 0;
            if (this.isDirty()) {
                count = 1;
            }
            if (this.get("children").length){
                this.get("children").forEach(function(child) {
                    count += child.countDirty();
                });
            }
            return count;
        },

        // Uninstalls should be performed bottom-up - from the leaf nodes
        // to the parent.
        uninstall: function(statusUpdate) {
            if (this.countDirty() > 0){
                // uninstall all needed children
                if (this.get("children").length){
                    this.get("children").forEach(function(child) {
                        child.uninstall(statusUpdate);
                    });
                }
                // uninstall myself
                if (!this.get("selected") && this.isDirty()) {
                    this.save(statusUpdate);
                }
            }
        },

        // Installs should be performed top-down - from the parent node down
        // through the children.
        install: function(statusUpdate) {
            if (this.countDirty() > 0){
                // install myself
                if (this.get("selected") && this.isDirty()) {
                    this.save(statusUpdate);
                }

                // install my needed children
                if (this.get("children").length){
                    this.get("children").forEach(function(child) {
                        child.install(statusUpdate);
                    });
                }
            }
        },

        // Performs the actual AJAX call to save the current model. Takes a status
        // function to keep anyone who cares informed about each step being performed.
        save: function(statusUpdate){
            if (this.isDirty()) {
                if (this.get("selected")) {
                    statusUpdate("Installing " + this.get("name"));
                    $.ajax({
                        type: "GET",
                        url: startUrl + this.get("name") + '/',
                        dataType: "JSON",
                        async: false
                    });

                } else {
                    statusUpdate("Uninstalling " + this.get("name"));
                    $.ajax({
                        type: "GET",
                        url: stopUrl + this.get("name") + '/',
                        dataType: "JSON",
                        async: false
                    });
                }
            }
        }



    });

    // Applications.TreeNodeCollection
    // -------------------------------

    // Represents a collection of application nodes. Note that each of the `Applications.Treenode`
    // elements can be recursive nodes.
    Applications.TreeNodeCollection = Backbone.Collection.extend({
        model: Applications.TreeNode,
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationTree/',

        // Reading the collection can be perfomed using a normal fetch (through the
        // `Applications.Response` model - then pulling out the values.
        // Saving the state of the selected applications doesn't follow the normal
        // REST model - each application is uninstalled or installed through
        // the application-service.
        sync: function(method, model, options){
            var statusUpdate = options.statusUpdate;
            var thisModel = model;
            if (method === 'read'){
                var appResponse = new Applications.Response();
                appResponse.fetch({
                    success: function(model){
                        thisModel.reset(model.get("value"));
                    }
                });
            } else { // this is a save of the model (CUD)
                this.save(statusUpdate);
            }
        },

        // Performs the application of the user-selected changes to the application dependency
        // trees (each element of this collection is the root of one dependency tree). This save
        // method accepts a `statusUpdate` function which will be called with `(message, percentComplete)`
        // to keep the caller aware of the current status.
        save: function(statusUpdate) {
            // Determine the total number of actions to be performed so that we can provide
            // a percent complete in the `statusUpdate` method.
            var count = 0;
            var totalCount = 0;
            this.each(function(child) {
               totalCount += child.countDirty();
            });

            // Uninstall the apps first
            this.each(function(child) {
                child.uninstall(function(message) {
                    if (typeof statusUpdate !== 'undefined') {
                        statusUpdate(message, count/totalCount*100);
                    }
                    count++;
                });
            });

            // Then install necessary apps
            this.each(function(child) {
                child.install(function(message) {
                    if (typeof statusUpdate !== 'undefined') {
                        statusUpdate(message, count/totalCount*100);
                    }
                    count++;
                });
            });

            if (typeof statusUpdate !== 'undefined') {
                statusUpdate("Total of " + totalCount + " applications installed/uninstalled.", 100);
            }
        }

    });

    // Applications.Response
    // ---------------------

    // Represents the response from the application-service when obtaining the list of all applications
    // on the system.
    Applications.Response = Backbone.Model.extend({
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationTree/'
    });

    return Applications;

});