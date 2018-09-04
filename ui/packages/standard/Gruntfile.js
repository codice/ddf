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

module.exports = function(grunt) {
  require('load-grunt-tasks')(grunt, { requireResolution: true })

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    ports: {
      phantom: 0,
      selenium: 0,
      express: 0,
    },

    clean: {
      build: ['target/webapp'],
    },
    cssmin: {
      compress: {
        files: {
          'target/webapp/css/index.css': ['src/main/webapp/css/*.css'],
        },
      },
    },
    less: {
      css: {
        options: {
          cleancss: true,
        },
        files: {
          'src/main/webapp/css/styles.css': 'src/main/webapp/less/styles.less',
        },
      },
    },
    mochaWebdriver: {
      options: {
        autoInstall: true,
        usePromises: true,
        reporter: 'spec',
        timeout: 1000 * 30,
        slow: 10000,
        expressPort: '<%= ports.express %>',
      },
      phantom: {
        src: ['src/test/js/wd/*.js'],
        options: {
          hostname: '127.0.0.1',
          usePhantom: true,
          phantomPort: '<%= ports.phantom %>',
        },
      },
      selenium: {
        src: ['src/test/js/wd/*.js'],
        options: {
          // make sure to start selenium server at host:port first
          hostname: '127.0.0.1',
          port: '<%= ports.selenium %>',
          // mochaWebdriver always starts a selenium server so
          // starting phantomjs instance that will not be used
          phantomPort: '<%= ports.phantom %>',
          usePhantom: true,
        },
      },
      sauce: {
        src: ['src/test/js/wd/*.js'],
        options: {
          autoInstall: false,
          testName: 'Search UI',
          concurrency: 3,
          timeout: 1000 * 60 * 2,
          browsers: [
            {
              platform: 'Windows 7',
              browserName: 'internet explorer',
              version: '9',
            },
            { platform: 'Windows 7', browserName: 'chrome', version: '38' },
            { platform: 'Windows 7', browserName: 'firefox', version: '31' },
          ],
        },
      },
    },
    express: {
      options: {
        port: 8282,
        hostname: '*',
      },
      test: {
        options: {
          port: '<%= ports.express %>',
          script: './test.js',
        },
      },
      server: {
        options: {
          script: './server.js',
        },
      },
    },
    watch: {
      livereload: {
        options: { livereload: true },
        files: ['target/webapp/css/index.css'],
      },
      lessFiles: {
        files: [
          'src/main/webapp/less/*.less',
          'src/main/webapp/less/**/*.less',
          'src/main/webapp/less/***/*.less',
        ],
        tasks: ['less'],
      },
      cssFiles: {
        files: ['src/main/webapp/css/*.css'],
        tasks: ['cssmin'],
      },
    },
  })

  grunt.registerTask('test', [
    'port:allocator',
    'express:test' /*, 'mochaWebdriver:phantom'*/,
  ])
  // grunt.registerTask('test:selenium', ['port:allocator', 'express:test', 'mochaWebdriver:selenium']);
  // grunt.registerTask('test:sauce', ['port:allocator', 'express:test', 'mochaWebdriver:sauce']);

  grunt.registerTask('build', ['less', 'cssmin'])
  grunt.registerTask('default', ['build', 'express:server', 'watch'])
}
