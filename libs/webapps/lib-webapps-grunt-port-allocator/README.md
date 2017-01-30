# grunt-port-allocator

Dynamic port allocator for grunt.

## Getting Started

This plugin requires Grunt `~0.4.0`

If you haven't used [Grunt](http://gruntjs.com/) before, be sure
to check out the [Getting Started](http://gruntjs.com/getting-started) guide, as it
explains how to create a [Gruntfile](http://gruntjs.com/sample-gruntfile) as well as
install and use Grunt plugins. Once you're familiar with that
process, you may install this plugin with this command:

    yarn install grunt-port-allocator --save-dev

Once the plugin has been installed, it may be enabled inside
your Gruntfile with this line of JavaScript:

grunt.loadNpmTasks('grunt-port-allocator');

## Port Allocator Task

The follow is a basic example of how to use
`grunt-port-allocator` to wire up various parts of your grunt
configuration together dynamically; the most common use case is
probably running tests.

```javascript
// Gruntfile.js
module.exports = function (grunt) {

  grunt.initConfig({
    ports: {
      phantom: 0,
      express:  0
    },
    express: {
      test: {
        port: '<%= ports.express %>'
      }
    },
    mochaWebdriver: {
      options: {
        expressPort: '<%= ports.express %>'
      },
      phantom: {
        options: {
          phantomPort: '<%= ports.phantom %>'
        }
      }
    }
  });
 
  grunt.registerTask('test',[
    'port:allocator',
    'express:test',
    'mochaWebdriver:phantom'
  ]);

};
```

With the above configuration you should be able to run `grunt
test` more than once without any `EADDRINUSE, Address already in
use` errors.

## Details and Limitations

The port allocation algorithm is as follows:

```
start at port 21000

if you can bind to the port then
  the next 9 ports should also be available for use
else
  try to bind at port + 10, repeat until successful
```

A lot of these number are arbitrary, but they suit our needs.
However, this means that you can only ask for 9 ports at most
from `grunt-port-allocator`. Also, you can still encounter a
range, where a random port out of the 9 is unavailable, so we do
a quick sweep to make sure they are free before returning the 9
ports; of course there is still a race between when the sweep
occurs and when your server binds to the port. The main goal of
this plugin is to prevent different instance of grunt from
clobbering itself. Take a look at `lib/port-allocator.js` for
more information.

