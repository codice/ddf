/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global require*/
/*jshint browser: true */

if (typeof define !== 'function') {
    var define = require('amdefine')(module);
}

define([
        'backbone.marionette',
        'jquery',
        'backbone'
    ],
    function (Marionette, $, Backbone) {
        "use strict";
        var Ingest = {};

        Ingest.DetailModel = Backbone.Model.extend({
            defaults: {
                "url" : "http://download.geonames.org/export/dump/allCountries.zip",
                "state" : "start",
                "progress" : 0
            },
            collection : new Backbone.Collection(),

            updateGeoIndexWithUrl: function(checked) {
                var url = this.get("url").split('/').join('!/');
                url = "/jolokia/exec/org.codice.ddf.spatial.admin.module.service.Geocoding:service=geocoding/updateGeoIndexWithUrl/" + url + "/" + checked;
                this.set('state', 'uploading-geonames');
                this.set("progress",0);
                this.post(url,this);
                return false;
            },
            updateGeoIndexWithFilePath: function(resourceUri, id, model, checked) {
                var url = "/jolokia/exec/org.codice.ddf.spatial.admin.module.service.Geocoding:service=geocoding/updateGeoIndexWithFilePath/" + id + "/" + resourceUri + "/" + checked;
                this.post(url,model);
                return false;
            },
            startPollingModel: function(model) {
               (function poll() {
                    var currentProgress = model.get("progress") ;

                    if(currentProgress !== 100 && model.get("state") !== "failed" && model.get("state") !== "failed-geonames" ) {
                        setTimeout(function() {
                            $.ajax({
                                url : "/jolokia/exec/org.codice.ddf.spatial.admin.module.service.Geocoding:service=geocoding/progressCallback/",
                                success: function(data) {
                                    var progress = parseInt(data.value);
                                    model.set('progress', progress);
                                },
                                error: function() {
                                    model.set('progress', 100);
                                    if(model.get('state') === 'uploading-geonames') {
                                        model.set('state', 'failed-geonames');
                                    } else {
                                        model.set('state', 'failed');
                                    }
                                },
                                dataType: "json",
                                complete: poll
                            });
                        }, 500);
                    }
                })();
            },
            buildModelFromFileData: function(data){
                var dataModel = new Backbone.Model({
                    name: data.files[0].name,
                    size : data.files[0].size,
                    type : data.files[0].type,
                    state : 'start',
                    error : data.errorThrown,
                    progress: parseInt(data.loaded / data.total * 50, 10)
                });
                return dataModel;
             },
            post: function(url, model) {
                var isModel = (model === this);

                $.ajax({
                  url: url,
                  dataType: 'json',
                  success: function(data) {
                    if(isModel) {
                        if(data.value) {
                            model.set('state', 'done-geonames');
                        } else {
                            model.set('state', 'failed-geonames');
                        }
                    } else {
                        if(data.value) {
                            model.set('state', 'done');
                        } else {
                            model.set('state', 'failed');
                        }
                    }
                  },
                  error: function() {
                    if(model === this) {
                        model.set("state", "failed-geonames");
                    } else {
                        model.set("state", "failed");
                    }
                  }
                });

                this.startPollingModel(model);
            }
        });
        return Ingest;
    });