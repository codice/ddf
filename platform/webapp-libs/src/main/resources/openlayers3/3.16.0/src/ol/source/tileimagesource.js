goog.provide('ol.source.TileImage');

goog.require('goog.asserts');
goog.require('ol.ImageTile');
goog.require('ol.TileCache');
goog.require('ol.TileState');
goog.require('ol.events');
goog.require('ol.events.EventType');
goog.require('ol.proj');
goog.require('ol.reproj.Tile');
goog.require('ol.source.UrlTile');


/**
 * @classdesc
 * Base class for sources providing images divided into a tile grid.
 *
 * @constructor
 * @fires ol.source.TileEvent
 * @extends {ol.source.UrlTile}
 * @param {olx.source.TileImageOptions} options Image tile options.
 * @api
 */
ol.source.TileImage = function(options) {

  ol.source.UrlTile.call(this, {
    attributions: options.attributions,
    cacheSize: options.cacheSize,
    extent: options.extent,
    logo: options.logo,
    opaque: options.opaque,
    projection: options.projection,
    state: options.state,
    tileGrid: options.tileGrid,
    tileLoadFunction: options.tileLoadFunction ?
        options.tileLoadFunction : ol.source.TileImage.defaultTileLoadFunction,
    tilePixelRatio: options.tilePixelRatio,
    tileUrlFunction: options.tileUrlFunction,
    url: options.url,
    urls: options.urls,
    wrapX: options.wrapX
  });

  /**
   * @protected
   * @type {?string}
   */
  this.crossOrigin =
      options.crossOrigin !== undefined ? options.crossOrigin : null;

  /**
   * @protected
   * @type {function(new: ol.ImageTile, ol.TileCoord, ol.TileState, string,
   *        ?string, ol.TileLoadFunctionType)}
   */
  this.tileClass = options.tileClass !== undefined ?
      options.tileClass : ol.ImageTile;

  /**
   * @protected
   * @type {Object.<string, ol.TileCache>}
   */
  this.tileCacheForProjection = {};

  /**
   * @protected
   * @type {Object.<string, ol.tilegrid.TileGrid>}
   */
  this.tileGridForProjection = {};

  /**
   * @private
   * @type {number|undefined}
   */
  this.reprojectionErrorThreshold_ = options.reprojectionErrorThreshold;

  /**
   * @private
   * @type {boolean}
   */
  this.renderReprojectionEdges_ = false;
};
ol.inherits(ol.source.TileImage, ol.source.UrlTile);


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.canExpireCache = function() {
  if (!ol.ENABLE_RASTER_REPROJECTION) {
    return ol.source.UrlTile.prototype.canExpireCache.call(this);
  }
  if (this.tileCache.canExpireCache()) {
    return true;
  } else {
    for (var key in this.tileCacheForProjection) {
      if (this.tileCacheForProjection[key].canExpireCache()) {
        return true;
      }
    }
  }
  return false;
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.expireCache = function(projection, usedTiles) {
  if (!ol.ENABLE_RASTER_REPROJECTION) {
    ol.source.UrlTile.prototype.expireCache.call(this, projection, usedTiles);
    return;
  }
  var usedTileCache = this.getTileCacheForProjection(projection);

  this.tileCache.expireCache(this.tileCache == usedTileCache ? usedTiles : {});
  for (var id in this.tileCacheForProjection) {
    var tileCache = this.tileCacheForProjection[id];
    tileCache.expireCache(tileCache == usedTileCache ? usedTiles : {});
  }
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.getGutter = function(projection) {
  if (ol.ENABLE_RASTER_REPROJECTION &&
      this.getProjection() && projection &&
      !ol.proj.equivalent(this.getProjection(), projection)) {
    return 0;
  } else {
    return this.getGutterInternal();
  }
};


/**
 * @protected
 * @return {number} Gutter.
 */
ol.source.TileImage.prototype.getGutterInternal = function() {
  return 0;
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.getOpaque = function(projection) {
  if (ol.ENABLE_RASTER_REPROJECTION &&
      this.getProjection() && projection &&
      !ol.proj.equivalent(this.getProjection(), projection)) {
    return false;
  } else {
    return ol.source.UrlTile.prototype.getOpaque.call(this, projection);
  }
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.getTileGridForProjection = function(projection) {
  if (!ol.ENABLE_RASTER_REPROJECTION) {
    return ol.source.UrlTile.prototype.getTileGridForProjection.call(this, projection);
  }
  var thisProj = this.getProjection();
  if (this.tileGrid &&
      (!thisProj || ol.proj.equivalent(thisProj, projection))) {
    return this.tileGrid;
  } else {
    var projKey = goog.getUid(projection).toString();
    if (!(projKey in this.tileGridForProjection)) {
      this.tileGridForProjection[projKey] =
          ol.tilegrid.getForProjection(projection);
    }
    return this.tileGridForProjection[projKey];
  }
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.getTileCacheForProjection = function(projection) {
  if (!ol.ENABLE_RASTER_REPROJECTION) {
    return ol.source.UrlTile.prototype.getTileCacheForProjection.call(this, projection);
  }
  var thisProj = this.getProjection();
  if (!thisProj || ol.proj.equivalent(thisProj, projection)) {
    return this.tileCache;
  } else {
    var projKey = goog.getUid(projection).toString();
    if (!(projKey in this.tileCacheForProjection)) {
      this.tileCacheForProjection[projKey] = new ol.TileCache();
    }
    return this.tileCacheForProjection[projKey];
  }
};


/**
 * @param {number} z Tile coordinate z.
 * @param {number} x Tile coordinate x.
 * @param {number} y Tile coordinate y.
 * @param {number} pixelRatio Pixel ratio.
 * @param {ol.proj.Projection} projection Projection.
 * @param {string} key The key set on the tile.
 * @return {ol.Tile} Tile.
 * @private
 */
ol.source.TileImage.prototype.createTile_ = function(z, x, y, pixelRatio, projection, key) {
  var tileCoord = [z, x, y];
  var urlTileCoord = this.getTileCoordForTileUrlFunction(
      tileCoord, projection);
  var tileUrl = urlTileCoord ?
      this.tileUrlFunction(urlTileCoord, pixelRatio, projection) : undefined;
  var tile = new this.tileClass(
      tileCoord,
      tileUrl !== undefined ? ol.TileState.IDLE : ol.TileState.EMPTY,
      tileUrl !== undefined ? tileUrl : '',
      this.crossOrigin,
      this.tileLoadFunction);
  tile.key = key;
  ol.events.listen(tile, ol.events.EventType.CHANGE,
      this.handleTileChange, this);
  return tile;
};


/**
 * @inheritDoc
 */
ol.source.TileImage.prototype.getTile = function(z, x, y, pixelRatio, projection) {
  if (!ol.ENABLE_RASTER_REPROJECTION ||
      !this.getProjection() ||
      !projection ||
      ol.proj.equivalent(this.getProjection(), projection)) {
    return this.getTileInternal(z, x, y, pixelRatio, projection);
  } else {
    var cache = this.getTileCacheForProjection(projection);
    var tileCoord = [z, x, y];
    var tileCoordKey = this.getKeyZXY.apply(this, tileCoord);
    if (cache.containsKey(tileCoordKey)) {
      return /** @type {!ol.Tile} */ (cache.get(tileCoordKey));
    } else {
      var sourceProjection = this.getProjection();
      var sourceTileGrid = this.getTileGridForProjection(sourceProjection);
      var targetTileGrid = this.getTileGridForProjection(projection);
      var wrappedTileCoord =
          this.getTileCoordForTileUrlFunction(tileCoord, projection);
      var tile = new ol.reproj.Tile(
          sourceProjection, sourceTileGrid,
          projection, targetTileGrid,
          tileCoord, wrappedTileCoord, this.getTilePixelRatio(pixelRatio),
          this.getGutterInternal(),
          function(z, x, y, pixelRatio) {
            return this.getTileInternal(z, x, y, pixelRatio, sourceProjection);
          }.bind(this), this.reprojectionErrorThreshold_,
          this.renderReprojectionEdges_);

      cache.set(tileCoordKey, tile);
      return tile;
    }
  }
};


/**
 * @param {number} z Tile coordinate z.
 * @param {number} x Tile coordinate x.
 * @param {number} y Tile coordinate y.
 * @param {number} pixelRatio Pixel ratio.
 * @param {ol.proj.Projection} projection Projection.
 * @return {!ol.Tile} Tile.
 * @protected
 */
ol.source.TileImage.prototype.getTileInternal = function(z, x, y, pixelRatio, projection) {
  var /** @type {ol.Tile} */ tile = null;
  var tileCoordKey = this.getKeyZXY(z, x, y);
  var key = this.getKey();
  if (!this.tileCache.containsKey(tileCoordKey)) {
    goog.asserts.assert(projection, 'argument projection is truthy');
    tile = this.createTile_(z, x, y, pixelRatio, projection, key);
    this.tileCache.set(tileCoordKey, tile);
  } else {
    tile = /** @type {!ol.Tile} */ (this.tileCache.get(tileCoordKey));
    if (tile.key != key) {
      // The source's params changed. If the tile has an interim tile and if we
      // can use it then we use it. Otherwise we create a new tile.  In both
      // cases we attempt to assign an interim tile to the new tile.
      var /** @type {ol.Tile} */ interimTile = tile;
      if (tile.interimTile && tile.interimTile.key == key) {
        goog.asserts.assert(tile.interimTile.getState() == ol.TileState.LOADED);
        goog.asserts.assert(tile.interimTile.interimTile === null);
        tile = tile.interimTile;
        if (interimTile.getState() == ol.TileState.LOADED) {
          tile.interimTile = interimTile;
        }
      } else {
        tile = this.createTile_(z, x, y, pixelRatio, projection, key);
        if (interimTile.getState() == ol.TileState.LOADED) {
          tile.interimTile = interimTile;
        } else if (interimTile.interimTile &&
            interimTile.interimTile.getState() == ol.TileState.LOADED) {
          tile.interimTile = interimTile.interimTile;
          interimTile.interimTile = null;
        }
      }
      if (tile.interimTile) {
        tile.interimTile.interimTile = null;
      }
      this.tileCache.replace(tileCoordKey, tile);
    }
  }
  goog.asserts.assert(tile);
  return tile;
};


/**
 * Sets whether to render reprojection edges or not (usually for debugging).
 * @param {boolean} render Render the edges.
 * @api
 */
ol.source.TileImage.prototype.setRenderReprojectionEdges = function(render) {
  if (!ol.ENABLE_RASTER_REPROJECTION ||
      this.renderReprojectionEdges_ == render) {
    return;
  }
  this.renderReprojectionEdges_ = render;
  for (var id in this.tileCacheForProjection) {
    this.tileCacheForProjection[id].clear();
  }
  this.changed();
};


/**
 * Sets the tile grid to use when reprojecting the tiles to the given
 * projection instead of the default tile grid for the projection.
 *
 * This can be useful when the default tile grid cannot be created
 * (e.g. projection has no extent defined) or
 * for optimization reasons (custom tile size, resolutions, ...).
 *
 * @param {ol.proj.ProjectionLike} projection Projection.
 * @param {ol.tilegrid.TileGrid} tilegrid Tile grid to use for the projection.
 * @api
 */
ol.source.TileImage.prototype.setTileGridForProjection = function(projection, tilegrid) {
  if (ol.ENABLE_RASTER_REPROJECTION) {
    var proj = ol.proj.get(projection);
    if (proj) {
      var projKey = goog.getUid(proj).toString();
      if (!(projKey in this.tileGridForProjection)) {
        this.tileGridForProjection[projKey] = tilegrid;
      }
    }
  }
};


/**
 * @param {ol.ImageTile} imageTile Image tile.
 * @param {string} src Source.
 */
ol.source.TileImage.defaultTileLoadFunction = function(imageTile, src) {
  imageTile.getImage().src = src;
};
