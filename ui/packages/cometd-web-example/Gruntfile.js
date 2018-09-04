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
module.exports = function (grunt) {

  grunt.initConfig({
    bower: {
      install: {
        options: {
          targetDir: 'src/main/webapp/lib',
          cleanTargetDir: false,
          layout: 'byComponent',
          copy: false,
        }
      }
    },
    watch: {
      all: {
        files: ['Gruntfile.js', 'src/main/webapp/**/*.html', 'src/main/webapp/**/*.js', 'src/main/webapp/**/*.css']
      }
    },
    connect: {
      server: {
        options: {
          base: 'src/main/webapp',
          port: 8000,
          hostname: '*',
          livereload: true
        }
      }
    },
  });

  grunt.loadNpmTasks('grunt-contrib-connect');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-bower-task');
  grunt.registerTask('build', ['bower:install']);
  grunt.registerTask('serve', 'launch webserver and watch tasks', [
    'build', 'connect:server', 'watch'
  ]);

  grunt.registerTask('default', ['serve']);
};
