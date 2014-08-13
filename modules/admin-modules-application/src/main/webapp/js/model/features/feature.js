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
//define(['backbone','underscore'], function (Backbone,_) {
define(['backbone','jquery','underscore'], function (Backbone,$,_) {

    var Feature = {};

    var featureUrl = '/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/AllFeatures';
    var featureByAppUrl = '/jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/findApplicationFeatures/';
    var installUrl = '/jolokia/exec/org.apache.karaf:type=features,name=root/installFeature(java.lang.String)/';
    var uninstallUrl = '/jolokia/exec/org.apache.karaf:type=features,name=root/uninstallFeature(java.lang.String)/';



    Feature.Model = Backbone.Model.extend({
        initialize: function(options) {
            _.bindAll(this);
            this.name = options.name;
        },
        urlRoot: function(){
            return featureUrl;
        },
        install: function(){
            return $.ajax({
                type: 'GET',
                url: installUrl + this.name,
                dataType: 'JSON'
            });
        },
        uninstall: function(){
            return $.ajax({
                type: 'GET',
                url: uninstallUrl + this.name,
                dataType: 'JSON'
            });
        }

    });

    Feature.Collection = Backbone.Collection.extend({
        model: Feature.Model,
        initialize: function(options){
            this.type = options.type;
            this.appName = options.appName;
        },
        url: function() {
            if(this.type !== 'all'){
                return featureByAppUrl + this.appName;
            }else{
                return featureUrl;
            }

        },
        parse: function(resp){
            return resp.value;
        }
    });

    return Feature;
});

