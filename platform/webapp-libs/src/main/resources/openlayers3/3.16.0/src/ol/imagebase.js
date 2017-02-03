goog.provide('ol.ImageBase');
goog.provide('ol.ImageState');

goog.require('goog.asserts');
goog.require('ol.events.EventTarget');
goog.require('ol.events.EventType');
goog.require('ol.Attribution');


/**
 * @enum {number}
 */
ol.ImageState = {
  IDLE: 0,
  LOADING: 1,
  LOADED: 2,
  ERROR: 3
};


/**
 * @constructor
 * @extends {ol.events.EventTarget}
 * @param {ol.Extent} extent Extent.
 * @param {number|undefined} resolution Resolution.
 * @param {number} pixelRatio Pixel ratio.
 * @param {ol.ImageState} state State.
 * @param {Array.<ol.Attribution>} attributions Attributions.
 */
ol.ImageBase = function(extent, resolution, pixelRatio, state, attributions) {

  ol.events.EventTarget.call(this);

  /**
   * @private
   * @type {Array.<ol.Attribution>}
   */
  this.attributions_ = attributions;

  /**
   * @protected
   * @type {ol.Extent}
   */
  this.extent = extent;

  /**
   * @private
   * @type {number}
   */
  this.pixelRatio_ = pixelRatio;

  /**
   * @protected
   * @type {number|undefined}
   */
  this.resolution = resolution;

  /**
   * @protected
   * @type {ol.ImageState}
   */
  this.state = state;

};
ol.inherits(ol.ImageBase, ol.events.EventTarget);


/**
 * @protected
 */
ol.ImageBase.prototype.changed = function() {
  this.dispatchEvent(ol.events.EventType.CHANGE);
};


/**
 * @return {Array.<ol.Attribution>} Attributions.
 */
ol.ImageBase.prototype.getAttributions = function() {
  return this.attributions_;
};


/**
 * @return {ol.Extent} Extent.
 */
ol.ImageBase.prototype.getExtent = function() {
  return this.extent;
};


/**
 * @param {Object=} opt_context Object.
 * @return {HTMLCanvasElement|Image|HTMLVideoElement} Image.
 */
ol.ImageBase.prototype.getImage = goog.abstractMethod;


/**
 * @return {number} PixelRatio.
 */
ol.ImageBase.prototype.getPixelRatio = function() {
  return this.pixelRatio_;
};


/**
 * @return {number} Resolution.
 */
ol.ImageBase.prototype.getResolution = function() {
  goog.asserts.assert(this.resolution !== undefined, 'resolution not yet set');
  return this.resolution;
};


/**
 * @return {ol.ImageState} State.
 */
ol.ImageBase.prototype.getState = function() {
  return this.state;
};


/**
 * Load not yet loaded URI.
 */
ol.ImageBase.prototype.load = goog.abstractMethod;
