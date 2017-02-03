goog.provide('ol.source.TileUTFGrid');

goog.require('goog.asserts');
goog.require('goog.async.nextTick');
goog.require('ol.Attribution');
goog.require('ol.Tile');
goog.require('ol.TileState');
goog.require('ol.TileUrlFunction');
goog.require('ol.events');
goog.require('ol.events.EventType');
goog.require('ol.extent');
goog.require('ol.net');
goog.require('ol.proj');
goog.require('ol.source.State');
goog.require('ol.source.Tile');


/**
 * @classdesc
 * Layer source for UTFGrid interaction data loaded from TileJSON format.
 *
 * @constructor
 * @extends {ol.source.Tile}
 * @param {olx.source.TileUTFGridOptions} options Source options.
 * @api
 */
ol.source.TileUTFGrid = function(options) {
  ol.source.Tile.call(this, {
    projection: ol.proj.get('EPSG:3857'),
    state: ol.source.State.LOADING
  });

  /**
   * @private
   * @type {boolean}
   */
  this.preemptive_ = options.preemptive !== undefined ?
      options.preemptive : true;

  /**
   * @private
   * @type {!ol.TileUrlFunctionType}
   */
  this.tileUrlFunction_ = ol.TileUrlFunction.nullTileUrlFunction;

  /**
   * @private
   * @type {string|undefined}
   */
  this.template_ = undefined;

  /**
   * @private
   * @type {boolean}
   */
  this.jsonp_ = options.jsonp || false;

  if (options.url) {
    if (this.jsonp_) {
      ol.net.jsonp(options.url, this.handleTileJSONResponse.bind(this),
          this.handleTileJSONError.bind(this));
    } else {
      var client = new XMLHttpRequest();
      client.addEventListener('load', this.onXHRLoad_.bind(this));
      client.addEventListener('error', this.onXHRError_.bind(this));
      client.open('GET', options.url);
      client.send();
    }
  } else if (options.tileJSON) {
    this.handleTileJSONResponse(options.tileJSON);
  } else {
    goog.asserts.fail('Either url or tileJSON options must be provided');
  }
};
ol.inherits(ol.source.TileUTFGrid, ol.source.Tile);


/**
 * @private
 * @param {Event} event The load event.
 */
ol.source.TileUTFGrid.prototype.onXHRLoad_ = function(event) {
  var client = /** @type {XMLHttpRequest} */ (event.target);
  if (client.status >= 200 && client.status < 300) {
    var response;
    try {
      response = /** @type {TileJSON} */(JSON.parse(client.responseText));
    } catch (err) {
      this.handleTileJSONError();
      return;
    }
    this.handleTileJSONResponse(response);
  } else {
    this.handleTileJSONError();
  }
};


/**
 * @private
 * @param {Event} event The error event.
 */
ol.source.TileUTFGrid.prototype.onXHRError_ = function(event) {
  this.handleTileJSONError();
};


/**
 * Return the template from TileJSON.
 * @return {string|undefined} The template from TileJSON.
 * @api
 */
ol.source.TileUTFGrid.prototype.getTemplate = function() {
  return this.template_;
};


/**
 * Calls the callback (synchronously by default) with the available data
 * for given coordinate and resolution (or `null` if not yet loaded or
 * in case of an error).
 * @param {ol.Coordinate} coordinate Coordinate.
 * @param {number} resolution Resolution.
 * @param {function(this: T, *)} callback Callback.
 * @param {T=} opt_this The object to use as `this` in the callback.
 * @param {boolean=} opt_request If `true` the callback is always async.
 *                               The tile data is requested if not yet loaded.
 * @template T
 * @api
 */
ol.source.TileUTFGrid.prototype.forDataAtCoordinateAndResolution = function(
    coordinate, resolution, callback, opt_this, opt_request) {
  if (this.tileGrid) {
    var tileCoord = this.tileGrid.getTileCoordForCoordAndResolution(
        coordinate, resolution);
    var tile = /** @type {!ol.source.TileUTFGridTile_} */(this.getTile(
        tileCoord[0], tileCoord[1], tileCoord[2], 1, this.getProjection()));
    tile.forDataAtCoordinate(coordinate, callback, opt_this, opt_request);
  } else {
    if (opt_request === true) {
      goog.async.nextTick(function() {
        callback.call(opt_this, null);
      });
    } else {
      callback.call(opt_this, null);
    }
  }
};


/**
 * @protected
 */
ol.source.TileUTFGrid.prototype.handleTileJSONError = function() {
  this.setState(ol.source.State.ERROR);
};


/**
 * TODO: very similar to ol.source.TileJSON#handleTileJSONResponse
 * @protected
 * @param {TileJSON} tileJSON Tile JSON.
 */
ol.source.TileUTFGrid.prototype.handleTileJSONResponse = function(tileJSON) {

  var epsg4326Projection = ol.proj.get('EPSG:4326');

  var sourceProjection = this.getProjection();
  var extent;
  if (tileJSON.bounds !== undefined) {
    var transform = ol.proj.getTransformFromProjections(
        epsg4326Projection, sourceProjection);
    extent = ol.extent.applyTransform(tileJSON.bounds, transform);
  }

  if (tileJSON.scheme !== undefined) {
    goog.asserts.assert(tileJSON.scheme == 'xyz', 'tileJSON-scheme is "xyz"');
  }
  var minZoom = tileJSON.minzoom || 0;
  var maxZoom = tileJSON.maxzoom || 22;
  var tileGrid = ol.tilegrid.createXYZ({
    extent: ol.tilegrid.extentFromProjection(sourceProjection),
    maxZoom: maxZoom,
    minZoom: minZoom
  });
  this.tileGrid = tileGrid;

  this.template_ = tileJSON.template;

  var grids = tileJSON.grids;
  if (!grids) {
    this.setState(ol.source.State.ERROR);
    return;
  }

  this.tileUrlFunction_ =
      ol.TileUrlFunction.createFromTemplates(grids, tileGrid);

  if (tileJSON.attribution !== undefined) {
    var attributionExtent = extent !== undefined ?
        extent : epsg4326Projection.getExtent();
    /** @type {Object.<string, Array.<ol.TileRange>>} */
    var tileRanges = {};
    var z, zKey;
    for (z = minZoom; z <= maxZoom; ++z) {
      zKey = z.toString();
      tileRanges[zKey] =
          [tileGrid.getTileRangeForExtentAndZ(attributionExtent, z)];
    }
    this.setAttributions([
      new ol.Attribution({
        html: tileJSON.attribution,
        tileRanges: tileRanges
      })
    ]);
  }

  this.setState(ol.source.State.READY);

};


/**
 * @inheritDoc
 */
ol.source.TileUTFGrid.prototype.getTile = function(z, x, y, pixelRatio, projection) {
  var tileCoordKey = this.getKeyZXY(z, x, y);
  if (this.tileCache.containsKey(tileCoordKey)) {
    return /** @type {!ol.Tile} */ (this.tileCache.get(tileCoordKey));
  } else {
    goog.asserts.assert(projection, 'argument projection is truthy');
    var tileCoord = [z, x, y];
    var urlTileCoord =
        this.getTileCoordForTileUrlFunction(tileCoord, projection);
    var tileUrl = this.tileUrlFunction_(urlTileCoord, pixelRatio, projection);
    var tile = new ol.source.TileUTFGridTile_(
        tileCoord,
        tileUrl !== undefined ? ol.TileState.IDLE : ol.TileState.EMPTY,
        tileUrl !== undefined ? tileUrl : '',
        this.tileGrid.getTileCoordExtent(tileCoord),
        this.preemptive_,
        this.jsonp_);
    this.tileCache.set(tileCoordKey, tile);
    return tile;
  }
};


/**
 * @inheritDoc
 */
ol.source.TileUTFGrid.prototype.useTile = function(z, x, y) {
  var tileCoordKey = this.getKeyZXY(z, x, y);
  if (this.tileCache.containsKey(tileCoordKey)) {
    this.tileCache.get(tileCoordKey);
  }
};


/**
 * @constructor
 * @extends {ol.Tile}
 * @param {ol.TileCoord} tileCoord Tile coordinate.
 * @param {ol.TileState} state State.
 * @param {string} src Image source URI.
 * @param {ol.Extent} extent Extent of the tile.
 * @param {boolean} preemptive Load the tile when visible (before it's needed).
 * @param {boolean} jsonp Load the tile as a script.
 * @private
 */
ol.source.TileUTFGridTile_ = function(tileCoord, state, src, extent, preemptive, jsonp) {

  ol.Tile.call(this, tileCoord, state);

  /**
   * @private
   * @type {string}
   */
  this.src_ = src;

  /**
   * @private
   * @type {ol.Extent}
   */
  this.extent_ = extent;

  /**
   * @private
   * @type {boolean}
   */
  this.preemptive_ = preemptive;

  /**
   * @private
   * @type {Array.<string>}
   */
  this.grid_ = null;

  /**
   * @private
   * @type {Array.<string>}
   */
  this.keys_ = null;

  /**
   * @private
   * @type {Object.<string, Object>|undefined}
   */
  this.data_ = null;


  /**
   * @private
   * @type {boolean}
   */
  this.jsonp_ = jsonp;

};
ol.inherits(ol.source.TileUTFGridTile_, ol.Tile);


/**
 * Get the image element for this tile.
 * @param {Object=} opt_context Optional context. Only used for the DOM
 *     renderer.
 * @return {Image} Image.
 */
ol.source.TileUTFGridTile_.prototype.getImage = function(opt_context) {
  return null;
};


/**
 * Synchronously returns data at given coordinate (if available).
 * @param {ol.Coordinate} coordinate Coordinate.
 * @return {*} The data.
 */
ol.source.TileUTFGridTile_.prototype.getData = function(coordinate) {
  if (!this.grid_ || !this.keys_) {
    return null;
  }
  var xRelative = (coordinate[0] - this.extent_[0]) /
      (this.extent_[2] - this.extent_[0]);
  var yRelative = (coordinate[1] - this.extent_[1]) /
      (this.extent_[3] - this.extent_[1]);

  var row = this.grid_[Math.floor((1 - yRelative) * this.grid_.length)];

  if (typeof row !== 'string') {
    return null;
  }

  var code = row.charCodeAt(Math.floor(xRelative * row.length));
  if (code >= 93) {
    code--;
  }
  if (code >= 35) {
    code--;
  }
  code -= 32;

  var data = null;
  if (code in this.keys_) {
    var id = this.keys_[code];
    if (this.data_ && id in this.data_) {
      data = this.data_[id];
    } else {
      data = id;
    }
  }
  return data;
};


/**
 * Calls the callback (synchronously by default) with the available data
 * for given coordinate (or `null` if not yet loaded).
 * @param {ol.Coordinate} coordinate Coordinate.
 * @param {function(this: T, *)} callback Callback.
 * @param {T=} opt_this The object to use as `this` in the callback.
 * @param {boolean=} opt_request If `true` the callback is always async.
 *                               The tile data is requested if not yet loaded.
 * @template T
 */
ol.source.TileUTFGridTile_.prototype.forDataAtCoordinate = function(coordinate, callback, opt_this, opt_request) {
  if (this.state == ol.TileState.IDLE && opt_request === true) {
    ol.events.listenOnce(this, ol.events.EventType.CHANGE, function(e) {
      callback.call(opt_this, this.getData(coordinate));
    }, this);
    this.loadInternal_();
  } else {
    if (opt_request === true) {
      goog.async.nextTick(function() {
        callback.call(opt_this, this.getData(coordinate));
      }, this);
    } else {
      callback.call(opt_this, this.getData(coordinate));
    }
  }
};


/**
 * @inheritDoc
 */
ol.source.TileUTFGridTile_.prototype.getKey = function() {
  return this.src_;
};


/**
 * @private
 */
ol.source.TileUTFGridTile_.prototype.handleError_ = function() {
  this.state = ol.TileState.ERROR;
  this.changed();
};


/**
 * @param {!UTFGridJSON} json UTFGrid data.
 * @private
 */
ol.source.TileUTFGridTile_.prototype.handleLoad_ = function(json) {
  this.grid_ = json.grid;
  this.keys_ = json.keys;
  this.data_ = json.data;

  this.state = ol.TileState.EMPTY;
  this.changed();
};


/**
 * @private
 */
ol.source.TileUTFGridTile_.prototype.loadInternal_ = function() {
  if (this.state == ol.TileState.IDLE) {
    this.state = ol.TileState.LOADING;
    if (this.jsonp_) {
      ol.net.jsonp(this.src_, this.handleLoad_.bind(this),
          this.handleError_.bind(this));
    } else {
      var client = new XMLHttpRequest();
      client.addEventListener('load', this.onXHRLoad_.bind(this));
      client.addEventListener('error', this.onXHRError_.bind(this));
      client.open('GET', this.src_);
      client.send();
    }
  }
};


/**
 * @private
 * @param {Event} event The load event.
 */
ol.source.TileUTFGridTile_.prototype.onXHRLoad_ = function(event) {
  var client = /** @type {XMLHttpRequest} */ (event.target);
  if (client.status >= 200 && client.status < 300) {
    var response;
    try {
      response = /** @type {!UTFGridJSON} */(JSON.parse(client.responseText));
    } catch (err) {
      this.handleError_();
      return;
    }
    this.handleLoad_(response);
  } else {
    this.handleError_();
  }
};


/**
 * @private
 * @param {Event} event The error event.
 */
ol.source.TileUTFGridTile_.prototype.onXHRError_ = function(event) {
  this.handleError_();
};


/**
 * Load not yet loaded URI.
 */
ol.source.TileUTFGridTile_.prototype.load = function() {
  if (this.preemptive_) {
    this.loadInternal_();
  }
};
