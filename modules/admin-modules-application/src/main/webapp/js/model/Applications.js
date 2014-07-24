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
    'use strict';

    var Backbone = require('backbone'),
        $ = require('jquery'),
        _ = require('underscore'),
        Q = require('q');
    var Applications = {};

    var startUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/startApplication/';
    var stopUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/stopApplication/';
    var removeUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/removeApplication/';

    var versionRegex = /([^0-9]*)([0-9]+.*$)/;

    // Applications.TreeNode
    // ---------------------

    // Represents a node in the application tree where children are dependent on their parent being
    // installed. This node can have zero or more children (which are also 'Applications.Treenode`
    // nodes themselves).
    Applications.TreeNode = Backbone.Model.extend({
       defaults: function() {
            return {
                removeFlag: false,
                selected: false
            };
       },

       initialize: function(){
           var children = this.get('children');
           var that = this;
           var changeObj = {};

           // Some (not properly created) applications features file result in a name that includes the
           // version number - strip that off and move it into the version number.
           this.massageVersionNumbers();
           this.cleanupDisplayName();
           this.updateName();
           this.updateDescription();

           // Reflect the current state of the application in the model and keep the
           // state to determine if the user changes it.
           changeObj.selected = changeObj.currentState = this.get('state') === 'ACTIVE';
           changeObj.removeFlag = false;
           changeObj.error = false;

           // Change the children from json representation to models and include a link
           // in each to their parent.
           if (children) { 
               changeObj.children = new Applications.TreeNodeCollection(children);
               this.set(changeObj);
               this.get('children').forEach(function (child) {
                   child.set({parent: that});
               });
           } else {
               this.set(changeObj);
           }
       },

       // When the user selects or deselects an application, adjust the rest of the
       // model accordingly - deselects propagate down, selects propagate up.
       updateModel: function() { 
         if (this.get('selected')) {
             if (this.get('parent')) {
                this.get('parent').set({selected: true});
             }
         } else if (this.get('children').length){
             this.get('children').forEach(function(child) {
                 child.set({selected: false});
             });
         }
       },

       toggleRemoveFlag: function () {
           // Because the view to model bindings are recursive, invoking
           // this function on a leaf node will implicitly invoke it on 
           // the leaf's parent nodes. This check prevents the parent node
           // invokations from having any effect.
           if (this.get('children') && this.get('children').length === 0) {
               if (this.get('removeFlag')) {
                   this.set({removeFlag: false});  
                   
                   // Reset the selected flag to its original state
                   this.set({selected: this.get('currentState')});
               } else {
                   this.set({removeFlag: true}); 
                   this.set({selected: false});
               }
           }
       }, 

        // Since the name is used for ids in the DOM, remove any periods
        // that might exist - but store in a separate attribute since we need the
        // original name to control the application via the application-service.
        updateName: function() {
            //this.set({name: this.get('name').replace(/\./g,'')});
            this.set({appId: this.get('name').replace(/\./g,'')});
        },

        // Some apps come in having the version number included
        // as part of the app name - e.g. search-app-2.3.1.ALPHA3-SNAPSHOT.
        // This function strips the version from the display name and
        // places it in the version variable so the details show correctly.
        massageVersionNumbers: function() {
            var changeObj = {};
            changeObj.displayName = this.get('name');
            if (this.get('version') === '0.0.0') {
                var matches = this.get('name').match(versionRegex);
                if (matches.length === 3) {
                    changeObj.displayName = matches[1];
                    changeObj.version = matches[2];
                }
            }
            this.set(changeObj);
        },

        // Create a name suitable for display. First attempts to parse a display name from
        // the description (expecting a form of "application description::display name". If that
        // doesn't yield a display name, then it extracts it from the application name - camel-case
        // it and remove the dashes.
        cleanupDisplayName: function(){
            var changeObj = {};

            if (this.has('description')) {
                var desc = this.get('description');
                var values = desc.match(/(.*)::(.*)/);
                if (values !== null) {
                    if (values.length >= 3) {   // 0=whole string, 1=description, 2=display name
                        changeObj.description = values[1];
                        if (values[2].length > 0) { // handle empty title - use default below
                            changeObj.displayName = values[2];
                        }
                    }
                }
            }

            if (typeof changeObj.displayName === 'undefined') {
                var tempName = this.get('displayName'); //.replace(/\./g,'');
                var names = tempName.split('-');
                var workingName = '';
                var that = this;
                _.each(names, function(name) {
                    if (workingName.length > 0) {
                        workingName = workingName + ' ';
                    }
                    workingName = workingName + that.capitalizeFirstLetter(name);
                });
                changeObj.displayName = workingName;
            }

            this.set(changeObj);
        },

        // Capitalize and return the first letter of the given string.
       capitalizeFirstLetter: function(string){
           if (string && string !== ''){
               return string.charAt(0).toUpperCase() + string.slice(1);
           }
            return string;
       },

        // If the description has multiple paragraphs (separated by new line characters), build
        // an array of each paragraph for the details template to display.
        updateDescription: function(){
            if (this.has('description')) {
                var descArray = this.get('description').split('\\n');
                this.set('paragraphs', descArray);
            }
        },

        // Determines whether the user has changed the selection of this model or
        // not - does not check its children.
        isDirty: function() {
            // if user has flagged the application for removal OR
            // if the user has requested stop the application
            return (this.get('removeFlag') || 
                    this.get('selected') !== this.get('currentState'));
        },

        // Returns the total number of applications that the user has changed
        // the selection status of - includes this node and all of its children.
        countDirty: function() {
            var count = 0;
            if (this.isDirty()) {
                count = 1;
            }
            if (this.get('children').length){
                this.get('children').forEach(function(child) {
                    count += child.countDirty();
                });
            }
            return count;
        },

        // Uninstalls should be performed bottom-up - from the leaf nodes
        // to the parent.
        uninstall: function(statusUpdate) {
            var that = this;
            if (this.countDirty() > 0){
                var promiseArr = [];
                var uninstallSelf = function() {
                    if (!that.get('selected') && that.isDirty()) {
                        return that.save(statusUpdate);
                    }
                };
                // uninstall all needed children
                if (this.get('children').length){
                    this.get('children').forEach(function(child) {
                        var promise = child.uninstall(statusUpdate);
                        if(promise) {
                            promiseArr.push(promise);
                        }
                    });
                }
                if(promiseArr.length > 0) {
                    return Q.all(promiseArr).then(uninstallSelf);
                } else {
                    return uninstallSelf();
                }
            }
        },

        // Installs should be performed top-down - from the parent node down
        // through the children.
        install: function(statusUpdate) {
            var that = this;
            if (this.countDirty() > 0){
                // install myself
                var promise;
                var installChildren = function() {
                    var promiseArr = [];
                    if (that.get('children').length){
                        that.get('children').forEach(function(child) {
                            var cPromise = child.install(statusUpdate);
                            if(cPromise) {
                                promiseArr.push(cPromise);
                            }
                        });
                    }
                    return Q.all(promiseArr);
                };

                if (this.get('selected') && this.isDirty()) {
                    promise = this.save(statusUpdate);
                }

                if(promise) {
                    return promise.then(installChildren);
                } else {
                    return installChildren();
                }
            }
        },

        // Performs the actual AJAX call to save the current model. Takes a status
        // function to keep anyone who cares informed about each step being performed.
        save: function(statusUpdate){
            if (this.isDirty()) {
                var name = this.get('name');
                var type = 'GET';
                var url = '';

                var ajaxArgs = {};

                if (this.get('selected')) {
                    statusUpdate('Starting ' + name);
                    url = startUrl + name;
                } else if (this.get('removeFlag')) { // implies !selected is also true 
                    statusUpdate('Removing ' + name);
   
                    // Escape '/' characters of the URI of the application
                    // that is to be removed with '!/' and append it to the 
                    // GET URL (for Jolokia)
                    url = removeUrl + this.attributes.uri.replace(/\//g, "!/");
                } else { // if !selected
                    statusUpdate('Stopping ' + name);
                    url = stopUrl + name;
                }
                
                url = url + '/';

                ajaxArgs.type = type;
                ajaxArgs.url = url;
                ajaxArgs.dataType = 'JSON';

                return $.ajax(ajaxArgs);
            }
        },

        validateInstall: function(jsonModel, failList) {
            if(jsonModel.selected && this.get('state') !== 'ACTIVE') {
                failList.push(this.get('appId'));
                this.set({error: true});
            }
            this.get('children').each(function(child, index) {
                child.validateInstall(jsonModel.children.at(index).toJSON(), failList);
            });
        },

        setSelectedBasedOnProfile: function(installProfile){
            var self = this;
            if( _.indexOf(installProfile.get('defaultApplications'), self.get('appId')) > -1){
                self.set('selected', true);
            }

            if(self.get('children')){
                self.get('children').each(function(childModel){
                    childModel.setSelectedBasedOnProfile(installProfile);
                });
            }
        },

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
        sync: function(method, model, statusUpdate){
            var thisModel = this;
            if (method === 'read'){
                return model.fetch({
                    success: function(data){
                        thisModel.reset(data.get('value'));
                    }
                });
            } else { // this is a save of the model (CUD)
                return this.save(statusUpdate);
            }
        },

        numNodesChanged: function() {
            var totalCount = 0;
            this.each(function(child) {
                totalCount += child.countDirty();
            });
            return totalCount;
        },

        // Performs the application of the user-selected changes to the application dependency
        // trees (each element of this collection is the root of one dependency tree). This save
        // method accepts a `statusUpdate` function which will be called with `(message, percentComplete)`
        // to keep the caller aware of the current status.
        save: function(statusUpdate) {
            // Determine the total number of actions to be performed so that we can provide
            // a percent complete in the `statusUpdate` method.
            var that = this;
            var count = 0;
            var totalCount = this.numNodesChanged();

            var promiseArr = [];

            var internalStatusUpdate = function(message) {
                if (typeof statusUpdate !== 'undefined') {
                    statusUpdate(message, count/totalCount*100);
                }
                count++;
            };

            // this.each will iterate over each of the root-level 
            // applications. The install and uninstall functions
            // will process all of the children of the applications
            // passed to them 
            this.each(function(child) {
                promiseArr.push(child.uninstall(internalStatusUpdate));
            });

            return Q.all(promiseArr).then(function() {
                var promiseArr2 = [];
                // Then install necessary apps
                that.each(function(child) {
                    promiseArr2.push(child.install(internalStatusUpdate));
                });
                return Q.all(promiseArr2);
            });


        },

        validateInstall: function(jsonModel, numNodes, statusUpdate) {
            var failList = [];

            this.each(function(child, index) {
                child.validateInstall(jsonModel[index], failList);
            });

            var donePercent = (numNodes - failList.length)/numNodes*100;

            if(failList.length > 0) {
                if(failList.length === 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        statusUpdate('An application failed to install.', donePercent);
                    }
                } else if(failList.length > 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        statusUpdate('Several applications failed to install.', donePercent);
                    }
                }
            } else {
                if (typeof statusUpdate !== 'undefined') {
                    statusUpdate('', 100);
                }
            }
        },

        // loops through all nodes and their children and marks them selected if they exist in the installation profile provided.
        setSelectedBasedOnProfile: function(installProfile){
            var self = this;
            if(installProfile){
                self.each(function(model){
                    model.setSelectedBasedOnProfile(installProfile);
                });
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
