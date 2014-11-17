module.exports = function(grunt) {
    grunt.registerTask('bower-offline-install', 'Offline first Bower install', function() {
        var bower = require('bower');
        var done = this.async();
        grunt.log.debug("Trying to install bower packages offline.");
        bower.commands
            .install([], {save: true}, {offline: true})
            .on('data', function (data) {
                grunt.log.debug(data);
            })
            .on('error', function (data) {
                grunt.log.debug(data);
                grunt.log.debug("Trying to install bower packages online.");
                bower.commands
                    .install()
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