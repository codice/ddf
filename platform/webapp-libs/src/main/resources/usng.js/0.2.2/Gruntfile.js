module.exports = function(grunt){

  // Load grunt mocha task
  grunt.loadNpmTasks('grunt-mocha-test');

  grunt.initConfig({

    // Mocha
    mochaTest: {
      test: {
        options: {
        	reporter: 'spec',
        	captureFile: 'results.txt',
        	quiet: false,
        	clearRequireCache: false
      	},
        src: ['tests/**/*.js']
      }
    }
  });

  grunt.registerTask('default', ['mochaTest']);
};