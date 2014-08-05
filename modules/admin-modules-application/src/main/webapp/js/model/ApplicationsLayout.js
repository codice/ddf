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
define([
        'backbone',
        'jquery',
        'underscore',
        'q'
    ],
    function (Backbone, $, _, Q) {
    "use strict";

    var Applications = {};

    var startUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/startApplication/';
    var stopUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/stopApplication/';
//    var removeUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/removeApplication/';

    var versionRegex = /([^0-9]*)([0-9]+.*$)/;

    // Applications.TreeNode
    // ---------------------

    // Represents a node in the application tree where children are dependent on their parent being
    // installed. This node can have zero or more children (which are also 'Applications.Treenode`
    // nodes themselves).
    Applications.TreeNode = Backbone.Model.extend({
       defaults: {
            removeFlag: false,
            selected: false
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

        // Since the name is used for ids in the DOM, remove any periods
        // that might exist - but store in a separate attribute since we need the
        // original name to control the application via the application-service.
        updateName: function() {
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



        flagRemove: function () {
           this.set({removeFlag: true});
        },

        installAction: function(statusUpdate) {
            var promise;
            promise = this.start(statusUpdate);
            return promise;
        },

        uninstallAction: function(statusUpdate) {
            var promise;
            promise = this.stop(statusUpdate);
            return promise;
        },

        // Determines whether the user has changed the selection of this model or
        // not - does not check its children.
        installInactive: function() {
            return (this.get('state') === "INACTIVE");
        },

        // Determines whether the user has changed the selection of this model or
        // not - does not check its children.
        removeActive: function() {
            return (this.get('state') === "ACTIVE");
        },

        // Performs the actual AJAX call to save the current model. Takes a status
        // function to keep anyone who cares informed about each step being performed.

        // Installs should be performed top-down - from the parent node down
        // through the children.
        start: function(statusUpdate) {
            if (this.installInactive()) {
                var name = this.get('name');
                var type = 'GET';
                var url = '';

                var ajaxArgs = {};

                statusUpdate('Starting ' + name);
                url = startUrl + name;


                url = url + '/';

                ajaxArgs.type = type;
                ajaxArgs.url = url;
                ajaxArgs.dataType = 'JSON';

                return $.ajax(ajaxArgs);
            }
        },

        // Uninstalls should be performed bottom-up - from the leaf nodes
        // to the parent.
        stop: function(statusUpdate) {
            if (this.removeActive()) {
                var name = this.get('name');
                var type = 'GET';
                var url = '';

                var ajaxArgs = {};

                statusUpdate('Stopping ' + name);
                url = stopUrl + name;

                url = url + '/';

                ajaxArgs.type = type;
                ajaxArgs.url = url;
                ajaxArgs.dataType = 'JSON';

                return $.ajax(ajaxArgs);
            }
        },

        validateUpdatedNode: function(jsonModel, failList, action) {
            var that = this;
             if(action === "install") {
                this.collection.each(function(child) {
                     if((jsonModel.appId === child.attributes.appId) &&
                        (child.attributes.state !== 'ACTIVE')) {
                        failList.push(that.get('appId'));
                        child.attributes.error = true;
                     } else {
                        child.attributes.removeFlag = false;
                     }
                 });
             } else {
                this.collection.each(function(child) {
                     if((jsonModel.appId === child.attributes.appId) &&
                        (child.attributes.state !== 'INACTIVE')) {
                        failList.push(that.get('appId'));
                        child.attributes.error = true;
                     } else {
                        child.attributes.removeFlag = false;
                     }
                 });
             }
        }
    });

    // Applications.TreeNodeCollection
    // -------------------------------

    // Represents a collection of application nodes. Note that each of the `Applications.Treenode`
    // elements can be recursive nodes.
    Applications.TreeNodeCollection = Backbone.Collection.extend({
        model: Applications.TreeNode,
          url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationArray/',

        // Reading the collection can be perfomed using a normal fetch (through the
        // `Applications.Response` model - then pulling out the values.
        // Saving the state of the selected applications doesn't follow the normal
        // REST model - each application is uninstalled or installed through
        // the application-service.
        update: function(method, model, statusUpdate){
            var thisModel = this;
            if (method === 'read'){
                return model.fetch({
                    success: function(data){
                        thisModel.reset(data.get('value'));
                    }
                });
            } else if (method === 'install'){ // this is a save of the model (CUD)
                return this.updateAction(statusUpdate, "install");
            } else {
                return this.updateAction(statusUpdate, "remove");
            }
        },

        // Performs the application of the user-selected changes to the application dependency
        // trees (each element of this collection is the root of one dependency tree). This save
        // method accepts a `statusUpdate` function which will be called with `(message, percentComplete)`
        // to keep the caller aware of the current status.
        updateAction: function(statusUpdate, action) {
            // Determine the total number of actions to be performed so that we can provide
            // a percent complete in the `statusUpdate` method.
            var count = 0;
            var totalCount = 0;

            this.each(function(child) {
                if(child.attributes.removeFlag === true) {
                    totalCount++;
                }
            });

            var promiseArr = [];

            var internalStatusUpdate = function(message) {
                if (typeof statusUpdate !== 'undefined') {
                    statusUpdate(message, count/totalCount*100);
                }
                count++;
            };

            if(action === "install") {
                this.each(function(child) {
                    if(child.attributes.removeFlag === true) {
                        promiseArr.push(child.installAction(internalStatusUpdate));
                    }
                });
            } else {
                this.each(function(child) {
                    if(child.attributes.removeFlag === true) {
                        promiseArr.push(child.uninstallAction(internalStatusUpdate));
                    }

                });
            }
            return Q.all(promiseArr);
        },

        validateUpdate: function(jsonModel, numNodes, statusUpdate, action) {
            var that = this;
            var failList = [];

            jsonModel.forEach(function(child, index) {
                if(child.removeFlag === true) {
                    that.models[index].validateUpdatedNode(child, failList, action);
                }
            });

            var donePercent = (numNodes - failList.length)/numNodes*100;

            if(failList.length > 0) {
                if(failList.length === 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        if(action === 'install') {
                            statusUpdate('An application failed to install.', donePercent);
                        } else {
                            statusUpdate('An application removal failed.', donePercent);
                        }
                    }
                } else if(failList.length > 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        if(action === 'install') {
                            statusUpdate('Several applications failed to install.', donePercent);
                        } else {
                            statusUpdate('Several application removals failed.', donePercent);
                        }
                    }
                }
            } else {
                if (typeof statusUpdate !== 'undefined') {
                    if(action === 'install') {
                        statusUpdate('Install complete.', 100);
                    } else {
                        statusUpdate('Remove complete.', 100);
                    }
                }
            }
        }
    });

    // Applications.Response
    // ---------------------

    // Represents the response from the application-service when obtaining the list of all applications
    // on the system.
    Applications.Response = Backbone.Model.extend({
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationArray/'
    });

    return Applications;

});
