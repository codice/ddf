/*
 * *
 *  * Copyright (c) Codice Foundation
 *  *
 *  * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  * General Public License as published by the Free Software Foundation, either version 3 of the
 *  * License, or any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 *  * is distributed along with this program and can be found at
 *  * <http://www.gnu.org/licenses/lgpl.html>.
 *  *
 *  *
 */

module.exports = function(grunt) {
    grunt.registerTask('bower-offline-install', 'Offline first Bower install', function() {
        var bower = require('bower');
        var done = this.async();
        grunt.log.debug("Trying to install bower packages offline.");
        bower.commands
            .install([], {'save': false, 'save-dev': false}, {offline: true})
            .on('data', function (data) {
                grunt.log.debug(data);
            })
            .on('error', function (data) {
                grunt.log.debug(data);
                grunt.log.debug("Trying to install bower packages online.");
                bower.commands
                    .install([], {'save': false, 'save-dev': false})
                    .on('data', function (data) {
                        grunt.log.debug(data);
                    })
                    .on('error', function (data) {
                        grunt.log.error(data);
                        done(false);
                    })
                    .on('end', function () {
                        grunt.log.debug("Bower installed online.");
                        done();
                    });
            })
            .on('end', function () {
                grunt.log.debug("Bower installed offline.");
                done();
            });
    });
};
