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

module.exports = function(grunt) {
  require('load-grunt-tasks')(grunt, { requireResolution: true })

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

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
    express: {
      options: {
        port: 8282,
        hostname: '*',
      },
      test: {
        options: {
          port: 8888,
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

  grunt.registerTask('build', ['less', 'cssmin'])
  grunt.registerTask('default', ['build', 'express:server', 'watch'])
}
