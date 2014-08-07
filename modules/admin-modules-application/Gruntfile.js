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
/*global module,require*/


module.exports = function (grunt) {

    var which = require('which');

    grunt.initConfig({

        pkg: grunt.file.readJSON('package.json'),

        clean: {
          build: ['target/webapp']
        },
        bower: {
            install: {

            }
        },
        cssmin: {
            compress: {
                files: {
                    "target/webapp/css/index.css": ["src/main/webapp/css/*.css"]
                }
            }
        },
        jshint: {
            files: ['Gruntfile.js', 'src/main/webapp/js/**/*.js', 'src/main/webapp/config.js', 'src/main/webapp/main.js', 'src/main/webapp/properties.js'],
            options: {
                bitwise: true,        // Prohibits the use of bitwise operators such as ^ (XOR), | (OR) and others.
                forin: true,          // Requires all for in loops to filter object's items.
                latedef: true,        // Prohibits the use of a variable before it was defined.
                newcap: true,         // Requires you to capitalize names of constructor functions.
                noarg: true,          // Prohibits the use of arguments.caller and arguments.callee. Both .caller and .callee make quite a few optimizations impossible so they were deprecated in future versions of JavaScript.
                noempty: true,         // Warns when you have an empty block in your code.
                regexp: true,         // Prohibits the use of unsafe . in regular expressions.
                undef: true,          // Prohibits the use of explicitly undeclared variables.
                unused: true,         // Warns when you define and never use your variables.
                maxlen: 250,          // Set the maximum length of a line to 250 characters.  If triggered, the line should be wrapped.
                eqeqeq: true,         // Prohibits the use of == and != in favor of === and !==

                // Relaxing Options
                scripturl: true,      // This option suppresses warnings about the use of script-targeted URLsâ€”such as
                es5: true,             // Tells JSHint that your code uses ECMAScript 5 specific features such as getters and setters.

                // options here to override JSHint defaults
                globals: {
                    console: true,
                    module: true
                }
            }
        },
        watch: {
            jsFiles: {
                files: ['<%= jshint.files %>'],
                tasks: ['jshint']
            },
            livereload : {
                options : {livereload :true},
                files : ['target/webapp/css/index.css'
                    // this one is more dangerous, tends to reload the page if one file changes
                    // probably too annoying to be useful, uncomment if you want to try it out
//                    '<%= jshint.files %>'
                ]
            },
            cssFiles : {
                files :['src/main/webapp/css/*.css'],
                tasks : ['cssmin']
            },
            bowerFile: {
                files: ['src/main/webapp/bower.json'],
                tasks: ['bower']
            }
        },
        casperjs: {
            options: {
                async: {
                    parallel: false
                }
            },
            //this is where the tests would be called from
            files: ['src/test/js/*.js']
        },
        express: {
            options: {
                port: 8282,
                hostname: '*'
            },
            test: {
                options: {
                    port: 8383,
                    server: './test.js'
                }
            },
            server: {
                options: {
                    server: './server.js'
                }
            }
        },
        sed: {
            imports: {
                path: 'target/webapp/lib/bootswatch/flatly',
                pattern: '@import url\\("//fonts.googleapis.com/css\\?family=Lato:400,700,400italic"\\);',
                replacement: '@import url("../../lato/css/lato.min.css");',
                recursive: true
            }
        }
    });

    grunt.loadNpmTasks('grunt-bower-task');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-express');
    grunt.loadNpmTasks('grunt-casperjs');
    grunt.loadNpmTasks('grunt-sed');

    //the grunt-zip task interferes with grunt-express, but since grunt loads these tasks in serially, we can
    //just load it in down here after the express task is loaded. DO NOT move this task above the test task or
    //all tests will fail.
    grunt.registerTask('test', ['express:test','casperjs']);
    grunt.registerTask('bower-offline-install', 'Bower offline install work-around', function() {
        var bower = require('bower');
        var done = this.async();
        grunt.log.writeln("Trying to install bower packages OFFline.");
        bower.commands
            .install([], {save: true}, { offline: true })
             .on('data', function(data){
                grunt.log.write(data);
            })
            .on('error', function(data){
                grunt.log.writeln(data);
                grunt.log.writeln("Trying to install bower packages ONline.");
                bower.commands
                    .install()
                    .on('data', function(data){
                        grunt.log.write(data);
                    })
                    .on('error', function(data){
                        grunt.log.write(data);
                        done(false);
                    })
                    .on('end', function () {
                        grunt.log.write("Bower installed online.");
                        done();
                    });
            })
            .on('end', function () {

                grunt.log.writeln("Bower installed offline.");
               done();
            });
    });

    var buildTasks = ['clean', 'bower-offline-install', 'sed:imports', 'cssmin', 'jshint'];

    try {
        grunt.log.writeln('Checking for python');
        var pythonPath = which.sync('python');
        if(pythonPath) {
            grunt.log.writeln('Found python');
            buildTasks = ['clean', 'bower-offline-install', 'sed:imports', 'cssmin', 'jshint', 'test'];
        }
    } catch (e) {
        grunt.log.writeln('Python is not installed. Please install Python and ensure that it is in your path to run tests.');
    }

    grunt.registerTask('build', buildTasks);
    grunt.registerTask('default', ['build','express:server','watch']);

};
