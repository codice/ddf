const allocator = require('../lib/port-allocator.js');

module.exports = function(grunt) {
  grunt.registerTask('port:allocator', function() {
    grunt.config.requires('ports')

    const done = this.async();

    allocator(function(err, ports) {
      if (err) {
        grunt.fail.fatal(err)
      } else {
        const mapToPorts = function(obj) {
          return Object.keys(obj).reduce(function(o, key, i) {
            o[key] = ports[i]
            return o
          }, {})
        };

        grunt.config('ports', mapToPorts(grunt.config('ports')))

        done()
      }
    })
  })
}
