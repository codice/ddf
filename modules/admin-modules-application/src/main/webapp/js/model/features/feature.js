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
define(['backbone','underscore'], function (Backbone,_) {

    var Feature = {};
    var featureUrl = '/jolokia/read/org.apache.karaf:type=features,name=root';

    Feature.Model = Backbone.Model.extend({
        urlRoot: function(){
            return featureUrl;
        },
        toJSON: function(){
            var modelJSON = _.clone(this.attributes);
            return _.extend(modelJSON,{
                displayName: modelJSON.name.replace('profile-','')
            });
        }
    });

    Feature.Collection = Backbone.Collection.extend({
        model: Feature.Model,
        url: function() {
            return featureUrl;
        },
        parse: function(resp){
            return resp.value;
        }
    });

    return Feature;
});

