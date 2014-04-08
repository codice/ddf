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
    _ = require('underscore');
    var Applications = {};

    var versionRegex = /([^0-9]*)([0-9]+.*$)/;
    Applications.TreeNode = Backbone.Model.extend({
       defaults: function() {
            return {
                selected: false
            };
       },

       initialize: function(){
           var children = this.get("children");
           var that = this;
           this.massageVersionNumbers();
           this.cleanupDisplayName(); //set({displayName: this.createDisplayName()});
           this.updateName();
           if (children){
               this.set({children: new Applications.TreeNodeCollection(children)});
               this.get("children").forEach(function (child) {
                   child.set({parent: that});
               });
           }
           this.listenTo(this, "change", this.updateModel);
       },
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

       capitalizeFirstLetter: function(string){
           if (string && string !== ""){
               return string.charAt(0).toUpperCase() + string.slice(1);
           }
            return string;
       }
    });

    Applications.TreeNodeCollection = Backbone.Collection.extend({
        model: Applications.TreeNode
    });

    Applications.Response = Backbone.Model.extend({
        url: '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/ApplicationTree/'
    });

    return Applications;

});