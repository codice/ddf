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

var chai = require('chai');
require('underscore');
var Backbone = require('backbone');
Backbone.$ = require('jquery');
require('backbone.marionette');
var ingestModel = require('../main/webapp/js/model/Ingest');

describe('GeoNames Ingest Model', function(){
    var mockFile = {
        files : [
            { name: "file.zip",
                size : 1024,
                type : "zip"
            }
        ],
        state: "start",
        loaded: 0,
        errorThrown: "",
        total: 1024
    };
    var model = ingestModel.DetailModel.extend().__super__;
    var modelDataFile = model.buildModelFromFileData(mockFile).attributes;

    describe('Defaults', function() {
        var defaults = model.defaults;
        it('should have http://download.geonames.org/export/dump/allCountries.zip as a default url', function(){
            chai.assert.equal(defaults.url,"http://download.geonames.org/export/dump/allCountries.zip");
        });

        it('should have a start state', function(){
            chai.assert.equal(defaults.state,"start");
        });

        it('should have 0 progress', function(){
            chai.assert.equal(defaults.progress,0);
        });
    });

    describe('Build Data from Mock File Object', function() {

        it('should have the same filename ', function(){
            chai.assert.equal(modelDataFile.name,"file.zip");
        });

        it('should have a start state', function(){
            chai.assert.equal(modelDataFile.state,"start");
        });

        it('should have a zip file type', function(){
            chai.assert.equal(modelDataFile.type,"zip");
        });

        it('should be 1024 in size', function(){
            chai.assert.equal(modelDataFile.size,1024);
        });

    });

});
