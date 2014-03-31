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

    var Backbone = require('backbone');
    //require('backbonerelational');
    var Applications = {};

//    Applications.Node = Backbone.RelationalModel.extend({
//        relations: [{
//            type: Backbone.HasMany,
//            key: 'children',
//            relatedModel: Applications.Root,
//            collectionType: Applications.RootCollection,
//            reverseRelation: {
//                key: 'parent'
//            }
//        }]
//    });
//
//    Applications.Root = Backbone.RelationalModel.extend({
//        relations: [{
//            type: Backbone.HasMany,
//            key: 'children',
//            relatedModel: Applications.Node,
//            collectionType: Applications.NodeCollection,
//            reverseRelation: {
//                key: 'parent'
//            }
//        }]
//    });
//
//    Applications.NodeCollection = Backbone.Collection.extend({
//        model: Applications.Node
//    });
//
//    Applications.RootCollection = Backbone.Collection.extend({
//        model: Applications.Root
//    });

    Applications.TreeNode = Backbone.Model.extend({
       defaults: function() {
            return {
                selected: false
            };
       },

       initialize: function(){
           "use strict";
           var children = this.get("children");
           var that = this;
           this.set({displayName: this.createDisplayName()});
           if (children){
               this.set({children: new Applications.TreeNodeCollection(children)});
               this.get("children").forEach(function (child) {
                   child.set({parent: that});
               });
           }
           this.listenTo(this, "change", this.updateModel);
       },
       updateModel: function(){
           "use strict";
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

       createDisplayName: function(){
           "use strict";
           var names = this.get("name").split('-');
           var dispName = "";
           for (var i=0; i < names.length; i++) {
               if (i > 0) {
                   dispName = dispName + " ";
               }
               dispName = dispName + this.capitalizeFirstLetter(names[i]);
           }
           return dispName;
       },

       capitalizeFirstLetter: function(string){
           "use strict";
           if (string && string !== ""){
               return string.charAt(0).toUpperCase() + string.slice(1);
           }
            return string;
       }
    });

    Applications.TreeNodeCollection = Backbone.Collection.extend({
        model: Applications.TreeNode
    });

    return Applications;

});