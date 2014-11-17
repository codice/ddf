module.exports = function(grunt) {
    grunt.registerTask('casper', 'Run CasperJS if Phython is installed', function() {
        var which = require('which');
        try {
            grunt.log.debug('Checking for python');
            var pythonPath = which.sync('python');
            if (pythonPath) {
                grunt.log.debug('Found python');
                grunt.task.run('casperjs')
            }
        } catch (e) {
            grunt.log.warn('Python is not installed. Please install Python and ensure that it is in your path to run tests.');
        }
    });
}