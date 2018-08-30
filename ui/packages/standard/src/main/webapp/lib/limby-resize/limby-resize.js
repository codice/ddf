/* 
 * MIT License
 *  You may use this code as long as you retain this notice.  Use at your own risk! :)
 *  https://github.com/danschumann/limby-resize
 */
;(function() {
  var _ = require('underscore')
  var debug = require('debug')('limbyResizer')

  var LimbyResizer
  LimbyResizer = function(config) {
    if (!(this instanceof LimbyResizer)) return new LimbyResizer(config)

    debug('constructor')

    // Disable if they didn't configure a image resizing module to use
    if (!config.imagemagick && !config.canvas)
      throw new Error(
        "You must initialize limby-resizer with config.(canvas|imagemagick) = require('canvas|imagemagick')"
      )

    this.config = config

    if (config.imagemagick) require('./lib/imagemagick')(this)
    else require('./lib/canvas')(this)
  }

  LimbyResizer.prototype.resize = function(filePath, options) {
    var width, height
    width = (options && options.width) || this.config.width
    height = (options && options.height) || this.config.height

    if (width && !height)
      throw new Error('If you specify width, you must also specify height')
    if (!width && height)
      throw new Error('If you specify height, you must also specify width')

    return this._resize(
      filePath,
      _.extend(_.clone(options), { width: width, height: height })
    )
  }

  module.exports = LimbyResizer
})()
