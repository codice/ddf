goog.provide('ol.VectorTile');

goog.require('ol.Tile');
goog.require('ol.TileState');
goog.require('ol.dom');
goog.require('ol.proj.Projection');


/**
 * @constructor
 * @extends {ol.Tile}
 * @param {ol.TileCoord} tileCoord Tile coordinate.
 * @param {ol.TileState} state State.
 * @param {string} src Data source url.
 * @param {ol.format.Feature} format Feature format.
 * @param {ol.TileLoadFunctionType} tileLoadFunction Tile load function.
 */
ol.VectorTile = function(tileCoord, state, src, format, tileLoadFunction) {

  ol.Tile.call(this, tileCoord, state);

  /**
   * @private
   * @type {CanvasRenderingContext2D}
   */
  this.context_ = ol.dom.createCanvasContext2D();

  /**
   * @private
   * @type {ol.format.Feature}
   */
  this.format_ = format;

  /**
   * @private
   * @type {Array.<ol.Feature>}
   */
  this.features_ = null;

  /**
   * @private
   * @type {ol.FeatureLoader}
   */
  this.loader_;

  /**
   * Data projection
   * @private
   * @type {ol.proj.Projection}
   */
  this.projection_;

  /**
   * @private
   * @type {ol.TileReplayState}
   */
  this.replayState_ = {
    dirty: false,
    renderedRenderOrder: null,
    renderedRevision: -1,
    renderedTileRevision: -1,
    replayGroup: null,
    skippedFeatures: []
  };

  /**
   * @private
   * @type {ol.TileLoadFunctionType}
   */
  this.tileLoadFunction_ = tileLoadFunction;

  /**
   * @private
   * @type {string}
   */
  this.url_ = src;

};
ol.inherits(ol.VectorTile, ol.Tile);


/**
 * @return {CanvasRenderingContext2D} The rendering context.
 */
ol.VectorTile.prototype.getContext = function() {
  return this.context_;
};


/**
 * @inheritDoc
 */
ol.VectorTile.prototype.getImage = function() {
  return this.replayState_.renderedTileRevision == -1 ?
      null : this.context_.canvas;
};


/**
 * Get the feature format assigned for reading this tile's features.
 * @return {ol.format.Feature} Feature format.
 * @api
 */
ol.VectorTile.prototype.getFormat = function() {
  return this.format_;
};


/**
 * @return {Array.<ol.Feature>} Features.
 */
ol.VectorTile.prototype.getFeatures = function() {
  return this.features_;
};


/**
 * @return {ol.TileReplayState} The replay state.
 */
ol.VectorTile.prototype.getReplayState = function() {
  return this.replayState_;
};


/**
 * @inheritDoc
 */
ol.VectorTile.prototype.getKey = function() {
  return this.url_;
};


/**
 * @return {ol.proj.Projection} Feature projection.
 */
ol.VectorTile.prototype.getProjection = function() {
  return this.projection_;
};


/**
 * Load the tile.
 */
ol.VectorTile.prototype.load = function() {
  if (this.state == ol.TileState.IDLE) {
    this.setState(ol.TileState.LOADING);
    this.tileLoadFunction_(this, this.url_);
    this.loader_(null, NaN, null);
  }
};


/**
 * @param {Array.<ol.Feature>} features Features.
 * @api
 */
ol.VectorTile.prototype.setFeatures = function(features) {
  this.features_ = features;
  this.setState(ol.TileState.LOADED);
};


/**
 * Set the projection of the features that were added with {@link #setFeatures}.
 * @param {ol.proj.Projection} projection Feature projection.
 * @api
 */
ol.VectorTile.prototype.setProjection = function(projection) {
  this.projection_ = projection;
};


/**
 * @param {ol.TileState} tileState Tile state.
 */
ol.VectorTile.prototype.setState = function(tileState) {
  this.state = tileState;
  this.changed();
};


/**
 * Set the feature loader for reading this tile's features.
 * @param {ol.FeatureLoader} loader Feature loader.
 * @api
 */
ol.VectorTile.prototype.setLoader = function(loader) {
  this.loader_ = loader;
};
