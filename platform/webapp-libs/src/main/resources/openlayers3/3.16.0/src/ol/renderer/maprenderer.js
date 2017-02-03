goog.provide('ol.RendererType');
goog.provide('ol.renderer.Map');

goog.require('goog.asserts');
goog.require('goog.vec.Mat4');
goog.require('ol');
goog.require('ol.Disposable');
goog.require('ol.events');
goog.require('ol.events.EventType');
goog.require('ol.extent');
goog.require('ol.functions');
goog.require('ol.layer.Layer');
goog.require('ol.renderer.Layer');
goog.require('ol.style.IconImageCache');
goog.require('ol.vec.Mat4');


/**
 * Available renderers: `'canvas'`, `'dom'` or `'webgl'`.
 * @enum {string}
 */
ol.RendererType = {
  CANVAS: 'canvas',
  DOM: 'dom',
  WEBGL: 'webgl'
};


/**
 * @constructor
 * @extends {ol.Disposable}
 * @param {Element} container Container.
 * @param {ol.Map} map Map.
 * @struct
 */
ol.renderer.Map = function(container, map) {

  ol.Disposable.call(this);


  /**
   * @private
   * @type {ol.Map}
   */
  this.map_ = map;

  /**
   * @private
   * @type {Object.<string, ol.renderer.Layer>}
   */
  this.layerRenderers_ = {};

  /**
   * @private
   * @type {Object.<string, ol.events.Key>}
   */
  this.layerRendererListeners_ = {};

};
ol.inherits(ol.renderer.Map, ol.Disposable);


/**
 * @param {olx.FrameState} frameState FrameState.
 * @protected
 */
ol.renderer.Map.prototype.calculateMatrices2D = function(frameState) {
  var viewState = frameState.viewState;
  var coordinateToPixelMatrix = frameState.coordinateToPixelMatrix;
  goog.asserts.assert(coordinateToPixelMatrix,
      'frameState has a coordinateToPixelMatrix');
  ol.vec.Mat4.makeTransform2D(coordinateToPixelMatrix,
      frameState.size[0] / 2, frameState.size[1] / 2,
      1 / viewState.resolution, -1 / viewState.resolution,
      -viewState.rotation,
      -viewState.center[0], -viewState.center[1]);
  var inverted = goog.vec.Mat4.invert(
      coordinateToPixelMatrix, frameState.pixelToCoordinateMatrix);
  goog.asserts.assert(inverted, 'matrix could be inverted');
};


/**
 * @param {ol.layer.Layer} layer Layer.
 * @protected
 * @return {ol.renderer.Layer} layerRenderer Layer renderer.
 */
ol.renderer.Map.prototype.createLayerRenderer = goog.abstractMethod;


/**
 * @inheritDoc
 */
ol.renderer.Map.prototype.disposeInternal = function() {
  for (var id in this.layerRenderers_) {
    this.layerRenderers_[id].dispose();
  }
};


/**
 * @param {ol.Map} map Map.
 * @param {olx.FrameState} frameState Frame state.
 * @private
 */
ol.renderer.Map.expireIconCache_ = function(map, frameState) {
  ol.style.IconImageCache.getInstance().expire();
};


/**
 * @param {ol.Coordinate} coordinate Coordinate.
 * @param {olx.FrameState} frameState FrameState.
 * @param {function(this: S, (ol.Feature|ol.render.Feature),
 *     ol.layer.Layer): T} callback Feature callback.
 * @param {S} thisArg Value to use as `this` when executing `callback`.
 * @param {function(this: U, ol.layer.Layer): boolean} layerFilter Layer filter
 *     function, only layers which are visible and for which this function
 *     returns `true` will be tested for features.  By default, all visible
 *     layers will be tested.
 * @param {U} thisArg2 Value to use as `this` when executing `layerFilter`.
 * @return {T|undefined} Callback result.
 * @template S,T,U
 */
ol.renderer.Map.prototype.forEachFeatureAtCoordinate = function(coordinate, frameState, callback, thisArg,
        layerFilter, thisArg2) {
  var result;
  var viewState = frameState.viewState;
  var viewResolution = viewState.resolution;

  /**
   * @param {ol.Feature|ol.render.Feature} feature Feature.
   * @param {ol.layer.Layer} layer Layer.
   * @return {?} Callback result.
   */
  function forEachFeatureAtCoordinate(feature, layer) {
    goog.asserts.assert(feature !== undefined, 'received a feature');
    var key = goog.getUid(feature).toString();
    var managed = frameState.layerStates[goog.getUid(layer)].managed;
    if (!(key in frameState.skippedFeatureUids && !managed)) {
      return callback.call(thisArg, feature, managed ? layer : null);
    }
  }

  var projection = viewState.projection;

  var translatedCoordinate = coordinate;
  if (projection.canWrapX()) {
    var projectionExtent = projection.getExtent();
    var worldWidth = ol.extent.getWidth(projectionExtent);
    var x = coordinate[0];
    if (x < projectionExtent[0] || x > projectionExtent[2]) {
      var worldsAway = Math.ceil((projectionExtent[0] - x) / worldWidth);
      translatedCoordinate = [x + worldWidth * worldsAway, coordinate[1]];
    }
  }

  var layerStates = frameState.layerStatesArray;
  var numLayers = layerStates.length;
  var i;
  for (i = numLayers - 1; i >= 0; --i) {
    var layerState = layerStates[i];
    var layer = layerState.layer;
    if (ol.layer.Layer.visibleAtResolution(layerState, viewResolution) &&
        layerFilter.call(thisArg2, layer)) {
      var layerRenderer = this.getLayerRenderer(layer);
      if (layer.getSource()) {
        result = layerRenderer.forEachFeatureAtCoordinate(
            layer.getSource().getWrapX() ? translatedCoordinate : coordinate,
            frameState, forEachFeatureAtCoordinate, thisArg);
      }
      if (result) {
        return result;
      }
    }
  }
  return undefined;
};


/**
 * @param {ol.Pixel} pixel Pixel.
 * @param {olx.FrameState} frameState FrameState.
 * @param {function(this: S, ol.layer.Layer): T} callback Layer
 *     callback.
 * @param {S} thisArg Value to use as `this` when executing `callback`.
 * @param {function(this: U, ol.layer.Layer): boolean} layerFilter Layer filter
 *     function, only layers which are visible and for which this function
 *     returns `true` will be tested for features.  By default, all visible
 *     layers will be tested.
 * @param {U} thisArg2 Value to use as `this` when executing `layerFilter`.
 * @return {T|undefined} Callback result.
 * @template S,T,U
 */
ol.renderer.Map.prototype.forEachLayerAtPixel = function(pixel, frameState, callback, thisArg,
        layerFilter, thisArg2) {
  var result;
  var viewState = frameState.viewState;
  var viewResolution = viewState.resolution;

  var layerStates = frameState.layerStatesArray;
  var numLayers = layerStates.length;
  var i;
  for (i = numLayers - 1; i >= 0; --i) {
    var layerState = layerStates[i];
    var layer = layerState.layer;
    if (ol.layer.Layer.visibleAtResolution(layerState, viewResolution) &&
        layerFilter.call(thisArg2, layer)) {
      var layerRenderer = this.getLayerRenderer(layer);
      result = layerRenderer.forEachLayerAtPixel(
          pixel, frameState, callback, thisArg);
      if (result) {
        return result;
      }
    }
  }
  return undefined;
};


/**
 * @param {ol.Coordinate} coordinate Coordinate.
 * @param {olx.FrameState} frameState FrameState.
 * @param {function(this: U, ol.layer.Layer): boolean} layerFilter Layer filter
 *     function, only layers which are visible and for which this function
 *     returns `true` will be tested for features.  By default, all visible
 *     layers will be tested.
 * @param {U} thisArg Value to use as `this` when executing `layerFilter`.
 * @return {boolean} Is there a feature at the given coordinate?
 * @template U
 */
ol.renderer.Map.prototype.hasFeatureAtCoordinate = function(coordinate, frameState, layerFilter, thisArg) {
  var hasFeature = this.forEachFeatureAtCoordinate(
      coordinate, frameState, ol.functions.TRUE, this, layerFilter, thisArg);

  return hasFeature !== undefined;
};


/**
 * @param {ol.layer.Layer} layer Layer.
 * @protected
 * @return {ol.renderer.Layer} Layer renderer.
 */
ol.renderer.Map.prototype.getLayerRenderer = function(layer) {
  var layerKey = goog.getUid(layer).toString();
  if (layerKey in this.layerRenderers_) {
    return this.layerRenderers_[layerKey];
  } else {
    var layerRenderer = this.createLayerRenderer(layer);
    this.layerRenderers_[layerKey] = layerRenderer;
    this.layerRendererListeners_[layerKey] = ol.events.listen(layerRenderer,
        ol.events.EventType.CHANGE, this.handleLayerRendererChange_, this);

    return layerRenderer;
  }
};


/**
 * @param {string} layerKey Layer key.
 * @protected
 * @return {ol.renderer.Layer} Layer renderer.
 */
ol.renderer.Map.prototype.getLayerRendererByKey = function(layerKey) {
  goog.asserts.assert(layerKey in this.layerRenderers_,
      'given layerKey (%s) exists in layerRenderers', layerKey);
  return this.layerRenderers_[layerKey];
};


/**
 * @protected
 * @return {Object.<string, ol.renderer.Layer>} Layer renderers.
 */
ol.renderer.Map.prototype.getLayerRenderers = function() {
  return this.layerRenderers_;
};


/**
 * @return {ol.Map} Map.
 */
ol.renderer.Map.prototype.getMap = function() {
  return this.map_;
};


/**
 * @return {string} Type
 */
ol.renderer.Map.prototype.getType = goog.abstractMethod;


/**
 * Handle changes in a layer renderer.
 * @private
 */
ol.renderer.Map.prototype.handleLayerRendererChange_ = function() {
  this.map_.render();
};


/**
 * @param {string} layerKey Layer key.
 * @return {ol.renderer.Layer} Layer renderer.
 * @private
 */
ol.renderer.Map.prototype.removeLayerRendererByKey_ = function(layerKey) {
  goog.asserts.assert(layerKey in this.layerRenderers_,
      'given layerKey (%s) exists in layerRenderers', layerKey);
  var layerRenderer = this.layerRenderers_[layerKey];
  delete this.layerRenderers_[layerKey];

  goog.asserts.assert(layerKey in this.layerRendererListeners_,
      'given layerKey (%s) exists in layerRendererListeners', layerKey);
  ol.events.unlistenByKey(this.layerRendererListeners_[layerKey]);
  delete this.layerRendererListeners_[layerKey];

  return layerRenderer;
};


/**
 * Render.
 * @param {?olx.FrameState} frameState Frame state.
 */
ol.renderer.Map.prototype.renderFrame = ol.nullFunction;


/**
 * @param {ol.Map} map Map.
 * @param {olx.FrameState} frameState Frame state.
 * @private
 */
ol.renderer.Map.prototype.removeUnusedLayerRenderers_ = function(map, frameState) {
  var layerKey;
  for (layerKey in this.layerRenderers_) {
    if (!frameState || !(layerKey in frameState.layerStates)) {
      this.removeLayerRendererByKey_(layerKey).dispose();
    }
  }
};


/**
 * @param {olx.FrameState} frameState Frame state.
 * @protected
 */
ol.renderer.Map.prototype.scheduleExpireIconCache = function(frameState) {
  frameState.postRenderFunctions.push(
    /** @type {ol.PostRenderFunction} */ (ol.renderer.Map.expireIconCache_)
  );
};


/**
 * @param {!olx.FrameState} frameState Frame state.
 * @protected
 */
ol.renderer.Map.prototype.scheduleRemoveUnusedLayerRenderers = function(frameState) {
  var layerKey;
  for (layerKey in this.layerRenderers_) {
    if (!(layerKey in frameState.layerStates)) {
      frameState.postRenderFunctions.push(
        /** @type {ol.PostRenderFunction} */ (this.removeUnusedLayerRenderers_.bind(this))
      );
      return;
    }
  }
};


/**
 * @param {ol.LayerState} state1 First layer state.
 * @param {ol.LayerState} state2 Second layer state.
 * @return {number} The zIndex difference.
 */
ol.renderer.Map.sortByZIndex = function(state1, state2) {
  return state1.zIndex - state2.zIndex;
};
