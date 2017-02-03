module.exports = function (grunt) {
  'use strict';

  var vendorLibs = [
    'bower_components/jquery/dist/jquery.js',
    'bower_components/underscore/underscore.js',
    'bower_components/backbone/backbone.js',
    'bower_components/sinon/index.js'
  ];

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    eslint: {
      target: ['Gruntfile.js', 'backbone.poller.js', 'test/**/*_spec.js']
    },

    jasmine: {
      options: {
        vendor: vendorLibs,
        summary: true,
        display: 'short',
        specs: 'test/spec/**/*.js'
      },
      poller: {
        src: ['backbone.poller.js'],
        options: {
          keepRunner: true,
          junit: {
            path: 'build/junit'
          },
          template: require('grunt-template-jasmine-istanbul'),
          templateOptions: {
            coverage: 'build/coverage/coverage.json',
            report: 'build/coverage',
            thresholds: {
              statements: 98,
              branches: 93,
              functions: 100,
              lines: 98
            }
          }

        }
      },
      'poller-min': {
        src: ['backbone.poller.min.js']
      }
    },

    uglify: {
      poller: {
        options: {
          preserveComments: 'some'
        },
        files: {
          'backbone.poller.min.js': ['backbone.poller.js']
        }
      }
    },

    docco: {
      poller: {
        src: ['backbone.poller.js'],
        options: {
          output: 'docs'
        }
      }
    }

  });

  grunt.loadNpmTasks('grunt-contrib-jasmine');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-docco');
  grunt.loadNpmTasks('grunt-eslint');

  grunt.registerTask('lint', ['eslint']);
  grunt.registerTask('default', ['lint', 'uglify', 'jasmine']);
};
