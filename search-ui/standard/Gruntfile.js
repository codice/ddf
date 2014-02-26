/*global module,require*/

module.exports = function (grunt) {

    var path = require('path');

    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),

        clean: {
          build: ['target/webapp']
        },
        bower: {
            install: {

            }
        },
        copy: {
            // workaround for version numbers in perfect scrollbar minified file names
            perfectscrollbar: {
                files: [
                    {
                        expand: true,
                        cwd: 'target/webapp/lib/perfect-scrollbar/min',
                        src: ['*'],
                        dest: 'target/webapp/lib/perfect-scrollbar/min/',
                        rename: function(dest, src) {
                            return path.join(dest, src.replace(/-\d\.\d\.\d\./,'.'));
                        }
                    }
                ]
            }
        },
        unzip: {
            // workaround for a cesium zip content type detection bug
            cesium: {
                router: function (filepath) {
                    if (grunt.file.doesPathContain('Cesium/', filepath)) {
                        return filepath;
                    } else {
                        return null;
                    }
                },
                src: 'target/webapp/lib/cesium/index.zip',
                dest: 'target/webapp/lib/cesium/'
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
        }
    });

    grunt.registerTask('cesiumclean', function() {
        // workaround for a cesium zip content type detection bug
        grunt.file.delete('target/webapp/lib/cesium/index.zip' , {force: true});
    });

    grunt.loadNpmTasks('grunt-bower-task');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-express');
    grunt.loadNpmTasks('grunt-casperjs');

    //the grunt-zip task interferes with grunt-express, but since grunt loads these tasks in serially, we can
    //just load it in down here after the express task is loaded. DO NOT move this task above the test task or
    //all tests will fail.
    grunt.registerTask('test', ['express:test','casperjs']);
    grunt.loadNpmTasks('grunt-zip');


    grunt.registerTask('build', ['clean', 'bower', 'copy', 'unzip', 'cesiumclean', 'cssmin', 'jshint', 'test']);
    grunt.registerTask('default', ['build','express:server','watch']);

};