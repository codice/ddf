module.exports = function(grunt) {
  grunt.loadTasks('../../main/js/tasks')

  grunt.initConfig({
    ports: {
      random: 0,
      express: 0,
    },
    express: {
      port: '<%= ports.express %>',
    },
  })

  grunt.registerTask('port:check', function() {
    const config = grunt.config('ports');

    if (config.express === 0) {
      grunt.fail.fatal('grunt-port-allocator failed to allocate ports.')
    }
  })

  grunt.registerTask('default', ['port:allocator', 'port:check'])
}
