goog.provide('ol.RasterOperationType');
goog.provide('ol.source.Raster');
goog.provide('ol.source.RasterEvent');
goog.provide('ol.source.RasterEventType');

goog.require('goog.asserts');
goog.require('goog.vec.Mat4');
goog.require('ol.ImageCanvas');
goog.require('ol.TileQueue');
goog.require('ol.dom');
goog.require('ol.events');
goog.require('ol.events.Event');
goog.require('ol.events.EventType');
goog.require('ol.ext.pixelworks');
goog.require('ol.extent');
goog.require('ol.layer.Image');
goog.require('ol.layer.Tile');
goog.require('ol.object');
goog.require('ol.renderer.canvas.ImageLayer');
goog.require('ol.renderer.canvas.TileLayer');
goog.require('ol.source.Image');
goog.require('ol.source.State');
goog.require('ol.source.Tile');


/**
 * Raster operation type. Supported values are `'pixel'` and `'image'`.
 * @enum {string}
 */
ol.RasterOperationType = {
  PIXEL: 'pixel',
  IMAGE: 'image'
};


/**
 * @classdesc
 * A source that transforms data from any number of input sources using an array
 * of {@link ol.RasterOperation} functions to transform input pixel values into
 * output pixel values.
 *
 * @constructor
 * @extends {ol.source.Image}
 * @fires ol.source.RasterEvent
 * @param {olx.source.RasterOptions} options Options.
 * @api
 */
ol.source.Raster = function(options) {

  /**
   * @private
   * @type {*}
   */
  this.worker_ = null;

  /**
   * @private
   * @type {ol.RasterOperationType}
   */
  this.operationType_ = options.operationType !== undefined ?
      options.operationType : ol.RasterOperationType.PIXEL;

  /**
   * @private
   * @type {number}
   */
  this.threads_ = options.threads !== undefined ? options.threads : 1;

  /**
   * @private
   * @type {Array.<ol.renderer.canvas.Layer>}
   */
  this.renderers_ = ol.source.Raster.createRenderers_(options.sources);

  for (var r = 0, rr = this.renderers_.length; r < rr; ++r) {
    ol.events.listen(this.renderers_[r], ol.events.EventType.CHANGE,
        this.changed, this);
  }

  /**
   * @private
   * @type {CanvasRenderingContext2D}
   */
  this.canvasContext_ = ol.dom.createCanvasContext2D();

  /**
   * @private
   * @type {ol.TileQueue}
   */
  this.tileQueue_ = new ol.TileQueue(
      function() {
        return 1;
      },
      this.changed.bind(this));

  var layerStatesArray = ol.source.Raster.getLayerStatesArray_(this.renderers_);
  var layerStates = {};
  for (var i = 0, ii = layerStatesArray.length; i < ii; ++i) {
    layerStates[goog.getUid(layerStatesArray[i].layer)] = layerStatesArray[i];
  }

  /**
   * The most recently rendered state.
   * @type {?ol.SourceRasterRenderedState}
   * @private
   */
  this.renderedState_ = null;

  /**
   * The most recently rendered image canvas.
   * @type {ol.ImageCanvas}
   * @private
   */
  this.renderedImageCanvas_ = null;

  /**
   * @private
   * @type {olx.FrameState}
   */
  this.frameState_ = {
    animate: false,
    attributions: {},
    coordinateToPixelMatrix: goog.vec.Mat4.createNumber(),
    extent: null,
    focus: null,
    index: 0,
    layerStates: layerStates,
    layerStatesArray: layerStatesArray,
    logos: {},
    pixelRatio: 1,
    pixelToCoordinateMatrix: goog.vec.Mat4.createNumber(),
    postRenderFunctions: [],
    size: [0, 0],
    skippedFeatureUids: {},
    tileQueue: this.tileQueue_,
    time: Date.now(),
    usedTiles: {},
    viewState: /** @type {olx.ViewState} */ ({
      rotation: 0
    }),
    viewHints: [],
    wantedTiles: {}
  };

  ol.source.Image.call(this, {});

  if (options.operation !== undefined) {
    this.setOperation(options.operation, options.lib);
  }

};
ol.inherits(ol.source.Raster, ol.source.Image);


/**
 * Set the operation.
 * @param {ol.RasterOperation} operation New operation.
 * @param {Object=} opt_lib Functions that will be available to operations run
 *     in a worker.
 * @api
 */
ol.source.Raster.prototype.setOperation = function(operation, opt_lib) {
  this.worker_ = new ol.ext.pixelworks.Processor({
    operation: operation,
    imageOps: this.operationType_ === ol.RasterOperationType.IMAGE,
    queue: 1,
    lib: opt_lib,
    threads: this.threads_
  });
  this.changed();
};


/**
 * Update the stored frame state.
 * @param {ol.Extent} extent The view extent (in map units).
 * @param {number} resolution The view resolution.
 * @param {ol.proj.Projection} projection The view projection.
 * @return {olx.FrameState} The updated frame state.
 * @private
 */
ol.source.Raster.prototype.updateFrameState_ = function(extent, resolution, projection) {

  var frameState = /** @type {olx.FrameState} */ (
      ol.object.assign({}, this.frameState_));

  frameState.viewState = /** @type {olx.ViewState} */ (
      ol.object.assign({}, frameState.viewState));

  var center = ol.extent.getCenter(extent);
  var width = Math.round(ol.extent.getWidth(extent) / resolution);
  var height = Math.round(ol.extent.getHeight(extent) / resolution);

  frameState.extent = extent;
  frameState.focus = ol.extent.getCenter(extent);
  frameState.size[0] = width;
  frameState.size[1] = height;

  var viewState = frameState.viewState;
  viewState.center = center;
  viewState.projection = projection;
  viewState.resolution = resolution;
  return frameState;
};


/**
 * Determine if the most recently rendered image canvas is dirty.
 * @param {ol.Extent} extent The requested extent.
 * @param {number} resolution The requested resolution.
 * @return {boolean} The image is dirty.
 * @private
 */
ol.source.Raster.prototype.isDirty_ = function(extent, resolution) {
  var state = this.renderedState_;
  return !state ||
      this.getRevision() !== state.revision ||
      resolution !== state.resolution ||
      !ol.extent.equals(extent, state.extent);
};


/**
 * @inheritDoc
 */
ol.source.Raster.prototype.getImage = function(extent, resolution, pixelRatio, projection) {

  if (!this.allSourcesReady_()) {
    return null;
  }

  var currentExtent = extent.slice();
  if (!this.isDirty_(currentExtent, resolution)) {
    return this.renderedImageCanvas_;
  }

  var context = this.canvasContext_;
  var canvas = context.canvas;

  var width = Math.round(ol.extent.getWidth(currentExtent) / resolution);
  var height = Math.round(ol.extent.getHeight(currentExtent) / resolution);

  if (width !== canvas.width ||
      height !== canvas.height) {
    canvas.width = width;
    canvas.height = height;
  }

  var frameState = this.updateFrameState_(currentExtent, resolution, projection);

  var imageCanvas = new ol.ImageCanvas(
      currentExtent, resolution, 1, this.getAttributions(), canvas,
      this.composeFrame_.bind(this, frameState));

  this.renderedImageCanvas_ = imageCanvas;

  this.renderedState_ = {
    extent: currentExtent,
    resolution: resolution,
    revision: this.getRevision()
  };

  return imageCanvas;
};


/**
 * Determine if all sources are ready.
 * @return {boolean} All sources are ready.
 * @private
 */
ol.source.Raster.prototype.allSourcesReady_ = function() {
  var ready = true;
  var source;
  for (var i = 0, ii = this.renderers_.length; i < ii; ++i) {
    source = this.renderers_[i].getLayer().getSource();
    if (source.getState() !== ol.source.State.READY) {
      ready = false;
      break;
    }
  }
  return ready;
};


/**
 * Compose the frame.  This renders data from all sources, runs pixel-wise
 * operations, and renders the result to the stored canvas context.
 * @param {olx.FrameState} frameState The frame state.
 * @param {function(Error)} callback Called when composition is complete.
 * @private
 */
ol.source.Raster.prototype.composeFrame_ = function(frameState, callback) {
  var len = this.renderers_.length;
  var imageDatas = new Array(len);
  for (var i = 0; i < len; ++i) {
    var imageData = ol.source.Raster.getImageData_(
        this.renderers_[i], frameState, frameState.layerStatesArray[i]);
    if (imageData) {
      imageDatas[i] = imageData;
    } else {
      // image not yet ready
      return;
    }
  }

  var data = {};
  this.dispatchEvent(new ol.source.RasterEvent(
      ol.source.RasterEventType.BEFOREOPERATIONS, frameState, data));

  this.worker_.process(imageDatas, data,
      this.onWorkerComplete_.bind(this, frameState, callback));

  frameState.tileQueue.loadMoreTiles(16, 16);
};


/**
 * Called when pixel processing is complete.
 * @param {olx.FrameState} frameState The frame state.
 * @param {function(Error)} callback Called when rendering is complete.
 * @param {Error} err Any error during processing.
 * @param {ImageData} output The output image data.
 * @param {Object} data The user data.
 * @private
 */
ol.source.Raster.prototype.onWorkerComplete_ = function(frameState, callback, err, output, data) {
  if (err) {
    callback(err);
    return;
  }
  if (!output) {
    // job aborted
    return;
  }

  this.dispatchEvent(new ol.source.RasterEvent(
      ol.source.RasterEventType.AFTEROPERATIONS, frameState, data));

  var resolution = frameState.viewState.resolution / frameState.pixelRatio;
  if (!this.isDirty_(frameState.extent, resolution)) {
    this.canvasContext_.putImageData(output, 0, 0);
  }

  callback(null);
};


/**
 * Get image data from a renderer.
 * @param {ol.renderer.canvas.Layer} renderer Layer renderer.
 * @param {olx.FrameState} frameState The frame state.
 * @param {ol.LayerState} layerState The layer state.
 * @return {ImageData} The image data.
 * @private
 */
ol.source.Raster.getImageData_ = function(renderer, frameState, layerState) {
  if (!renderer.prepareFrame(frameState, layerState)) {
    return null;
  }
  var width = frameState.size[0];
  var height = frameState.size[1];
  if (!ol.source.Raster.context_) {
    ol.source.Raster.context_ = ol.dom.createCanvasContext2D(width, height);
  } else {
    var canvas = ol.source.Raster.context_.canvas;
    if (canvas.width !== width || canvas.height !== height) {
      ol.source.Raster.context_ = ol.dom.createCanvasContext2D(width, height);
    } else {
      ol.source.Raster.context_.clearRect(0, 0, width, height);
    }
  }
  renderer.composeFrame(frameState, layerState, ol.source.Raster.context_);
  return ol.source.Raster.context_.getImageData(0, 0, width, height);
};


/**
 * A reusable canvas context.
 * @type {CanvasRenderingContext2D}
 * @private
 */
ol.source.Raster.context_ = null;


/**
 * Get a list of layer states from a list of renderers.
 * @param {Array.<ol.renderer.canvas.Layer>} renderers Layer renderers.
 * @return {Array.<ol.LayerState>} The layer states.
 * @private
 */
ol.source.Raster.getLayerStatesArray_ = function(renderers) {
  return renderers.map(function(renderer) {
    return renderer.getLayer().getLayerState();
  });
};


/**
 * Create renderers for all sources.
 * @param {Array.<ol.source.Source>} sources The sources.
 * @return {Array.<ol.renderer.canvas.Layer>} Array of layer renderers.
 * @private
 */
ol.source.Raster.createRenderers_ = function(sources) {
  var len = sources.length;
  var renderers = new Array(len);
  for (var i = 0; i < len; ++i) {
    renderers[i] = ol.source.Raster.createRenderer_(sources[i]);
  }
  return renderers;
};


/**
 * Create a renderer for the provided source.
 * @param {ol.source.Source} source The source.
 * @return {ol.renderer.canvas.Layer} The renderer.
 * @private
 */
ol.source.Raster.createRenderer_ = function(source) {
  var renderer = null;
  if (source instanceof ol.source.Tile) {
    renderer = ol.source.Raster.createTileRenderer_(source);
  } else if (source instanceof ol.source.Image) {
    renderer = ol.source.Raster.createImageRenderer_(source);
  } else {
    goog.asserts.fail('Unsupported source type: ' + source);
  }
  return renderer;
};


/**
 * Create an image renderer for the provided source.
 * @param {ol.source.Image} source The source.
 * @return {ol.renderer.canvas.Layer} The renderer.
 * @private
 */
ol.source.Raster.createImageRenderer_ = function(source) {
  var layer = new ol.layer.Image({source: source});
  return new ol.renderer.canvas.ImageLayer(layer);
};


/**
 * Create a tile renderer for the provided source.
 * @param {ol.source.Tile} source The source.
 * @return {ol.renderer.canvas.Layer} The renderer.
 * @private
 */
ol.source.Raster.createTileRenderer_ = function(source) {
  var layer = new ol.layer.Tile({source: source});
  return new ol.renderer.canvas.TileLayer(layer);
};


/**
 * @classdesc
 * Events emitted by {@link ol.source.Raster} instances are instances of this
 * type.
 *
 * @constructor
 * @extends {ol.events.Event}
 * @implements {oli.source.RasterEvent}
 * @param {string} type Type.
 * @param {olx.FrameState} frameState The frame state.
 * @param {Object} data An object made available to operations.
 */
ol.source.RasterEvent = function(type, frameState, data) {
  ol.events.Event.call(this, type);

  /**
   * The raster extent.
   * @type {ol.Extent}
   * @api
   */
  this.extent = frameState.extent;

  /**
   * The pixel resolution (map units per pixel).
   * @type {number}
   * @api
   */
  this.resolution = frameState.viewState.resolution / frameState.pixelRatio;

  /**
   * An object made available to all operations.  This can be used by operations
   * as a storage object (e.g. for calculating statistics).
   * @type {Object}
   * @api
   */
  this.data = data;

};
ol.inherits(ol.source.RasterEvent, ol.events.Event);


/**
 * @enum {string}
 */
ol.source.RasterEventType = {
  /**
   * Triggered before operations are run.
   * @event ol.source.RasterEvent#beforeoperations
   * @api
   */
  BEFOREOPERATIONS: 'beforeoperations',

  /**
   * Triggered after operations are run.
   * @event ol.source.RasterEvent#afteroperations
   * @api
   */
  AFTEROPERATIONS: 'afteroperations'
};
