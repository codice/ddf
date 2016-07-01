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
var webpack = require('webpack');

module.exports = function (grunt) {
    require('load-grunt-tasks')(grunt);
    grunt.loadTasks('src/main/grunt/tasks');

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        clean: {
            build: ['target/webapp']
        },
        bower: { install: {} },
        sed: {
            imports: {
                path: 'target/webapp/lib/bootswatch/cyborg',
                pattern: '@import url\\("//fonts.googleapis.com/css\\?family=Roboto:400,700"\\);',
                replacement: '@import url("../../lato/css/lato.min.css");',
                recursive: true
            }
        },
        webpack: {
            options: require('./webpack.config'),
            start: {
                devtool: 'eval',
                watch: true
            },
            build: {
                plugins: [
                    new webpack.optimize.UglifyJsPlugin({
                        compress: {
                            warnings: false
                        },
                        output: {
                            comments: false
                        }
                    })
                ]
            }
        },
        cssmin: {
            compress: {
                files: {
                    "target/webapp/css/index.css": ["src/main/webapp/css/*.css"]
                }
            }
        },
        less: {
            css: {
                options: {
                    cleancss: true
                },
                files: {
                    "src/main/webapp/css/styles.css":"src/main/webapp/less/styles.less"
                }
            }
        },
        jshint: {
            all: {
                src: [
                    'Gruntfile.js',
                    'src/main/webapp/js/**/*.js',
                    'src/main/webapp/config.js',
                    'src/main/webapp/main.js',
                    'src/main/webapp/properties.js',
                    'src/test/js/**/*.js'
                ]
            },
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

                reporter: require('jshint-stylish'),

                // options here to override JSHint defaults
                globals: {
                    console: true,
                    module: true,
                    define: true
                }
            }
        },
        simplemocha: {
            options: {
                fullTrace: true
            },
            test: {
                src: ['src/main/webapp/**/*.spec.js']
            }
        },
        express: {
            options: {
                port: 8282,
                hostname: '*'
            },
            server: {
                options: {
                    server: './server.js'
                }
            }
        },
        watch: {
            jsFiles: {
                files: ['<%= jshint.all.src %>'],
                tasks: ['jshint']
            },
            livereload : {
                options : {livereload :true},
                files : ['target/webapp/css/index.css']
            },
            lessFiles: {
                files: [
                    'src/main/webapp/component/**/*.less',
                    'src/main/webapp/less/**/*.less'
                ],
                tasks: ['less']
            },
            cssFiles : {
                files :['src/main/webapp/css/*.css'],
                tasks : ['cssmin']
            },
            bowerFile: {
                files: ['src/main/webapp/bower.json'],
                tasks: ['bower']
            }
        }
    });

    grunt.registerTask('test:unit', ['simplemocha:test']);

    grunt.registerTask('build:part', [
        'bower-offline-install',
        'sed',
        'less',
        'cssmin',
        'jshint'
    ]);

    grunt.registerTask('build', [
        'build:part',
        'webpack:build'
    ]);

    grunt.registerTask('default', [
        'build:part',
        'webpack:start',
        'express:server',
        'watch'
    ]);
};
