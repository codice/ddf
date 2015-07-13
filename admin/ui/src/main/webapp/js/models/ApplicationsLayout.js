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
    var removeUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/removeApplication/';

    var versionRegex = /([^0-9]*)([0-9]+.*$)/;

    // Applications.TreeNode
    // ---------------------

    // Represents a node in the application tree where children are dependent on their parent being
    // installed. This node can have zero or more children (which are also 'Applications.Treenode`
    // nodes themselves).
    Applications.TreeNode = Backbone.Model.extend({
        defaults: function () {
            return {
                chosenApp: false,
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
            changeObj.chosenApp = false;
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

        // Flag used to signify an application that was selected by the user to undergo a
        // certain action whether it be to be started or stopped
        toggleChosenApp: function () {
            this.set({chosenApp: true});
        },

        // Creates a promise for the start action of an application
        startAction: function(statusUpdate) {
            var promise;
            promise = this.start(statusUpdate);
            return promise;
        },

        // Creates a promise for the stop action of an application
        stopAction: function(statusUpdate) {
            var promise;
            promise = this.stop(statusUpdate);
            return promise;
        },

        removeAction: function(statusUpdate) {
            var promise;
            promise = this.removeCall(statusUpdate);
            return promise;
        },

        // Checks to see if the given application is in the INACTIVE state which is used
        // prior to starting an application
        startInactive: function() {
            return (this.get('state') === "INACTIVE");
        },

        // Checks to see if the given application is in the ACTIVE state which is used
        // prior to stopping an application
        stopActive: function() {
            return (this.get('state') === "ACTIVE");
        },

        // Performs the actual AJAX call to start the current application. Takes a status
        // function to keep anyone who cares informed about each step being performed.
        start: function(statusUpdate) {
            if (this.startInactive()) {
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

        // Performs the actual AJAX call to stop the current application. Takes a status
        // function to keep anyone who cares informed about each step being performed.
        stop: function(statusUpdate) {
            if (this.stopActive()) {
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

        removeCall: function(statusUpdate){
            var name = this.get('name');
            var type = 'GET';
            var url = '';

            var ajaxArgs = {};

            statusUpdate('Removing ' + name);
            url = removeUrl + name;

            url = url + '/';

            ajaxArgs.type = type;
            ajaxArgs.url = url;
            ajaxArgs.dataType = 'JSON';

            return $.ajax(ajaxArgs);
        },

        // Verifies that the applications that needed to be started or stopped
        // are in fact in the proper state in the system
        validateUpdatedNode: function(jsonModel, failList, action) {
            var that = this;
            if(action === "start") {
                this.collection.each(function(child) {
                    if((jsonModel.appId === child.attributes.appId) &&
                        (child.attributes.state !== 'ACTIVE')) {
                        failList.push(that.get('appId'));
                        child.attributes.error = true;
                    } else {
                        child.attributes.chosenApp = false;
                    }
                });
            } else {
                this.collection.each(function(child) {
                    if((jsonModel.appId === child.attributes.appId) &&
                        (child.attributes.state !== 'INACTIVE')) {
                        failList.push(that.get('appId'));
                        child.attributes.error = true;
                    } else {
                        child.attributes.chosenApp = false;
                    }
                });
            }
        },
        getAppKey: function(){
            return this.get('name') + '-' + this.get('version');
        }
    });

    // Applications.TreeNodeCollection
    // -------------------------------

    // Represents a collection of application nodes. Note that each of the `Applications.Treenode`
    // elements can be recursive nodes.
    Applications.TreeNodeCollection = Backbone.Collection.extend({
        model: Applications.TreeNode,
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/Applications/',

        comparator : function(model){
            return  model.get('displayName');
        },

        // Reading the collection can be performed using a normal fetch (through the
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
            } else if (method === 'start'){ // this is a save of the model (CUD)
                return this.updateAction(statusUpdate, "start");
            } else if (method === 'stop'){
                return this.updateAction(statusUpdate, "stop");
            } else if(method === 'remove'){
                return this.removeAction(statusUpdate, 'remove');
            }
        },

        removeAction: function(statusUpdate){
            var that = this;
            var promiseArr = [];

            var appDependents;
            var theChosenApp;
            var finalList = [];


            this.each(function(child) {
                if(child.get('chosenApp') === true) {
                    appDependents = child.get('dependencies');
                    theChosenApp = child.get('appId');
                }
            });

            if(appDependents.length > 0) {
                this.each(function(child) {
                    if(appDependents.indexOf(child.get('appId')) !== -1) {
                        finalList.push(child.get('appId'));
                    }
                });
            }
            finalList.push(theChosenApp);


            // Determine the total number of actions to be performed so that we can provide
            // a percent complete in the `statusUpdate` method.
            var count = 0;
            var totalCount = finalList.length;
            var internalStatusUpdate = function(message) {
                if (typeof statusUpdate !== 'undefined') {
                    statusUpdate(message, count/totalCount*100);
                }
                count++;
            };

            finalList.forEach(function(finalApps) {
                that.each(function(app) {
                    if(app.get('appId') === finalApps) {
                        promiseArr.push(app.removeAction(internalStatusUpdate));
                    }
                });
            });

            return Q.all(promiseArr);

        },

        // Performs the application of the user-selected changes to the application dependency
        // trees (each element of this collection is the root of one dependency tree). This save
        // method accepts a `statusUpdate` function which will be called with `(message, percentComplete)`
        // to keep the caller aware of the current status.
        updateAction: function(statusUpdate, action) {
            var that = this;
            var promiseArr = [];

            var appDependents;
            var theChosenApp;
            var finalList = [];

            // Find app that was selected to be started/stopped
            this.each(function(child) {
                if(child.get('chosenApp') === true) {
                    if(action === "start") {
                        appDependents = child.get('parents');
                    } else {
                        appDependents = child.get('dependencies');
                    }
                    theChosenApp = child.get('appId');
                }
            });

            // Create list of apps that will be started/stopped based on dependencies
            if(appDependents.length > 0) {
                this.each(function(child) {
                    if(appDependents.indexOf(child.get('appId')) !== -1) {
                        if(((action === "start") && (child.get('state') === 'INACTIVE')) ||
                           ((action === "stop") && (child.get('state') === 'ACTIVE')) ) {
                            finalList.push(child.get('appId'));
                        }
                    }
                });
            }
            finalList.push(theChosenApp);

            // Determine the total number of actions to be performed so that we can provide
            // a percent complete in the `statusUpdate` method.
            var count = 0;
            var totalCount = finalList.length;
            var internalStatusUpdate = function(message) {
                if (typeof statusUpdate !== 'undefined') {
                    statusUpdate(message, count/totalCount*100);
                }
                count++;
            };

            finalList.forEach(function(finalApps) {
                that.each(function(app) {
                    if(app.get('appId') === finalApps) {
                        if(action === "start") {
                            promiseArr.push(app.startAction(internalStatusUpdate));
                        } else {
                            promiseArr.push(app.stopAction(internalStatusUpdate));
                        }
                    }
                });
            });

            return Q.all(promiseArr);
        },

        // verify the start/stop actions
        validateUpdate: function(jsonModel, numNodes, statusUpdate, action) {
            var that = this;
            var failList = [];

            jsonModel.forEach(function(child, index) {
                if(child.chosenApp === true) {
                    that.models[index].validateUpdatedNode(child, failList, action);
                }
            });

            var donePercent = (numNodes - failList.length)/numNodes*100;

            if(failList.length > 0) {
                if(failList.length === 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        if(action === 'start') {
                            statusUpdate('An application failed to start.', donePercent);
                        } else {
                            statusUpdate('An application failed to stop.', donePercent);
                        }
                    }
                } else if(failList.length > 1) {
                    if (typeof statusUpdate !== 'undefined') {
                        if(action === 'start') {
                            statusUpdate('Several applications failed to start.', donePercent);
                        } else {
                            statusUpdate('Several applications failed to stop.', donePercent);
                        }
                    }
                }
            } else {
                if (typeof statusUpdate !== 'undefined') {
                    if(action === 'start') {
                        statusUpdate('Start complete.', 100);
                    } else if(action === 'stop'){
                        statusUpdate('Stop complete.', 100);
                    } else if(action === 'remove'){
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
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/Applications/'
    });

    return Applications;

});
