goog.provide('ol.ImageTile');

goog.require('goog.asserts');
goog.require('ol.Tile');
goog.require('ol.TileState');
goog.require('ol.events');
goog.require('ol.events.EventType');
goog.require('ol.object');


/**
 * @constructor
 * @extends {ol.Tile}
 * @param {ol.TileCoord} tileCoord Tile coordinate.
 * @param {ol.TileState} state State.
 * @param {string} src Image source URI.
 * @param {?string} crossOrigin Cross origin.
 * @param {ol.TileLoadFunctionType} tileLoadFunction Tile load function.
 */
ol.ImageTile = function(tileCoord, state, src, crossOrigin, tileLoadFunction) {

  ol.Tile.call(this, tileCoord, state);

  /**
   * Image URI
   *
   * @private
   * @type {string}
   */
  this.src_ = src;

  /**
   * @private
   * @type {Image}
   */
  this.image_ = new Image();
  if (crossOrigin !== null) {
    this.image_.crossOrigin = crossOrigin;
  }

  /**
   * @private
   * @type {Object.<number, Image>}
   */
  this.imageByContext_ = {};

  /**
   * @private
   * @type {Array.<ol.events.Key>}
   */
  this.imageListenerKeys_ = null;

  /**
   * @private
   * @type {ol.TileLoadFunctionType}
   */
  this.tileLoadFunction_ = tileLoadFunction;

};
ol.inherits(ol.ImageTile, ol.Tile);


/**
 * @inheritDoc
 */
ol.ImageTile.prototype.disposeInternal = function() {
  if (this.state == ol.TileState.LOADING) {
    this.unlistenImage_();
  }
  if (this.interimTile) {
    this.interimTile.dispose();
  }
  this.state = ol.TileState.ABORT;
  this.changed();
  ol.Tile.prototype.disposeInternal.call(this);
};


/**
 * Get the image element for this tile.
 * @inheritDoc
 * @api
 */
ol.ImageTile.prototype.getImage = function(opt_context) {
  if (opt_context !== undefined) {
    var image;
    var key = goog.getUid(opt_context);
    if (key in this.imageByContext_) {
      return this.imageByContext_[key];
    } else if (ol.object.isEmpty(this.imageByContext_)) {
      image = this.image_;
    } else {
      image = /** @type {Image} */ (this.image_.cloneNode(false));
    }
    this.imageByContext_[key] = image;
    return image;
  } else {
    return this.image_;
  }
};


/**
 * @inheritDoc
 */
ol.ImageTile.prototype.getKey = function() {
  return this.src_;
};


/**
 * Tracks loading or read errors.
 *
 * @private
 */
ol.ImageTile.prototype.handleImageError_ = function() {
  this.state = ol.TileState.ERROR;
  this.unlistenImage_();
  this.changed();
};


/**
 * Tracks successful image load.
 *
 * @private
 */
ol.ImageTile.prototype.handleImageLoad_ = function() {
  if (this.image_.naturalWidth && this.image_.naturalHeight) {
    this.state = ol.TileState.LOADED;
  } else {
    this.state = ol.TileState.EMPTY;
  }
  this.unlistenImage_();
  this.changed();
};


/**
 * Load the image or retry if loading previously failed.
 * Loading is taken care of by the tile queue, and calling this method is
 * only needed for preloading or for reloading in case of an error.
 * @api
 */
ol.ImageTile.prototype.load = function() {
  if (this.state == ol.TileState.IDLE || this.state == ol.TileState.ERROR) {
    this.state = ol.TileState.LOADING;
    this.changed();
    goog.asserts.assert(!this.imageListenerKeys_,
        'this.imageListenerKeys_ should be null');
    this.imageListenerKeys_ = [
      ol.events.listenOnce(this.image_, ol.events.EventType.ERROR,
          this.handleImageError_, this),
      ol.events.listenOnce(this.image_, ol.events.EventType.LOAD,
          this.handleImageLoad_, this)
    ];
    this.tileLoadFunction_(this, this.src_);
  }
};


/**
 * Discards event handlers which listen for load completion or errors.
 *
 * @private
 */
ol.ImageTile.prototype.unlistenImage_ = function() {
  goog.asserts.assert(this.imageListenerKeys_,
      'this.imageListenerKeys_ should not be null');
  this.imageListenerKeys_.forEach(ol.events.unlistenByKey);
  this.imageListenerKeys_ = null;
};
