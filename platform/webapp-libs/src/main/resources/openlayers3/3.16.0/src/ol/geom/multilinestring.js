goog.provide('ol.geom.MultiLineString');

goog.require('goog.asserts');
goog.require('ol');
goog.require('ol.array');
goog.require('ol.extent');
goog.require('ol.geom.GeometryLayout');
goog.require('ol.geom.GeometryType');
goog.require('ol.geom.LineString');
goog.require('ol.geom.SimpleGeometry');
goog.require('ol.geom.flat.closest');
goog.require('ol.geom.flat.deflate');
goog.require('ol.geom.flat.inflate');
goog.require('ol.geom.flat.interpolate');
goog.require('ol.geom.flat.intersectsextent');
goog.require('ol.geom.flat.simplify');


/**
 * @classdesc
 * Multi-linestring geometry.
 *
 * @constructor
 * @extends {ol.geom.SimpleGeometry}
 * @param {Array.<Array.<ol.Coordinate>>} coordinates Coordinates.
 * @param {ol.geom.GeometryLayout=} opt_layout Layout.
 * @api stable
 */
ol.geom.MultiLineString = function(coordinates, opt_layout) {

  ol.geom.SimpleGeometry.call(this);

  /**
   * @type {Array.<number>}
   * @private
   */
  this.ends_ = [];

  /**
   * @private
   * @type {number}
   */
  this.maxDelta_ = -1;

  /**
   * @private
   * @type {number}
   */
  this.maxDeltaRevision_ = -1;

  this.setCoordinates(coordinates, opt_layout);

};
ol.inherits(ol.geom.MultiLineString, ol.geom.SimpleGeometry);


/**
 * Append the passed linestring to the multilinestring.
 * @param {ol.geom.LineString} lineString LineString.
 * @api stable
 */
ol.geom.MultiLineString.prototype.appendLineString = function(lineString) {
  goog.asserts.assert(lineString.getLayout() == this.layout,
      'layout of lineString should match the layout');
  if (!this.flatCoordinates) {
    this.flatCoordinates = lineString.getFlatCoordinates().slice();
  } else {
    ol.array.extend(
        this.flatCoordinates, lineString.getFlatCoordinates().slice());
  }
  this.ends_.push(this.flatCoordinates.length);
  this.changed();
};


/**
 * Make a complete copy of the geometry.
 * @return {!ol.geom.MultiLineString} Clone.
 * @api stable
 */
ol.geom.MultiLineString.prototype.clone = function() {
  var multiLineString = new ol.geom.MultiLineString(null);
  multiLineString.setFlatCoordinates(
      this.layout, this.flatCoordinates.slice(), this.ends_.slice());
  return multiLineString;
};


/**
 * @inheritDoc
 */
ol.geom.MultiLineString.prototype.closestPointXY = function(x, y, closestPoint, minSquaredDistance) {
  if (minSquaredDistance <
      ol.extent.closestSquaredDistanceXY(this.getExtent(), x, y)) {
    return minSquaredDistance;
  }
  if (this.maxDeltaRevision_ != this.getRevision()) {
    this.maxDelta_ = Math.sqrt(ol.geom.flat.closest.getsMaxSquaredDelta(
        this.flatCoordinates, 0, this.ends_, this.stride, 0));
    this.maxDeltaRevision_ = this.getRevision();
  }
  return ol.geom.flat.closest.getsClosestPoint(
      this.flatCoordinates, 0, this.ends_, this.stride,
      this.maxDelta_, false, x, y, closestPoint, minSquaredDistance);
};


/**
 * Returns the coordinate at `m` using linear interpolation, or `null` if no
 * such coordinate exists.
 *
 * `opt_extrapolate` controls extrapolation beyond the range of Ms in the
 * MultiLineString. If `opt_extrapolate` is `true` then Ms less than the first
 * M will return the first coordinate and Ms greater than the last M will
 * return the last coordinate.
 *
 * `opt_interpolate` controls interpolation between consecutive LineStrings
 * within the MultiLineString. If `opt_interpolate` is `true` the coordinates
 * will be linearly interpolated between the last coordinate of one LineString
 * and the first coordinate of the next LineString.  If `opt_interpolate` is
 * `false` then the function will return `null` for Ms falling between
 * LineStrings.
 *
 * @param {number} m M.
 * @param {boolean=} opt_extrapolate Extrapolate. Default is `false`.
 * @param {boolean=} opt_interpolate Interpolate. Default is `false`.
 * @return {ol.Coordinate} Coordinate.
 * @api stable
 */
ol.geom.MultiLineString.prototype.getCoordinateAtM = function(m, opt_extrapolate, opt_interpolate) {
  if ((this.layout != ol.geom.GeometryLayout.XYM &&
       this.layout != ol.geom.GeometryLayout.XYZM) ||
      this.flatCoordinates.length === 0) {
    return null;
  }
  var extrapolate = opt_extrapolate !== undefined ? opt_extrapolate : false;
  var interpolate = opt_interpolate !== undefined ? opt_interpolate : false;
  return ol.geom.flat.lineStringsCoordinateAtM(this.flatCoordinates, 0,
      this.ends_, this.stride, m, extrapolate, interpolate);
};


/**
 * Return the coordinates of the multilinestring.
 * @return {Array.<Array.<ol.Coordinate>>} Coordinates.
 * @api stable
 */
ol.geom.MultiLineString.prototype.getCoordinates = function() {
  return ol.geom.flat.inflate.coordinatess(
      this.flatCoordinates, 0, this.ends_, this.stride);
};


/**
 * @return {Array.<number>} Ends.
 */
ol.geom.MultiLineString.prototype.getEnds = function() {
  return this.ends_;
};


/**
 * Return the linestring at the specified index.
 * @param {number} index Index.
 * @return {ol.geom.LineString} LineString.
 * @api stable
 */
ol.geom.MultiLineString.prototype.getLineString = function(index) {
  goog.asserts.assert(0 <= index && index < this.ends_.length,
      'index should be in between 0 and length of the this.ends_ array');
  if (index < 0 || this.ends_.length <= index) {
    return null;
  }
  var lineString = new ol.geom.LineString(null);
  lineString.setFlatCoordinates(this.layout, this.flatCoordinates.slice(
      index === 0 ? 0 : this.ends_[index - 1], this.ends_[index]));
  return lineString;
};


/**
 * Return the linestrings of this multilinestring.
 * @return {Array.<ol.geom.LineString>} LineStrings.
 * @api stable
 */
ol.geom.MultiLineString.prototype.getLineStrings = function() {
  var flatCoordinates = this.flatCoordinates;
  var ends = this.ends_;
  var layout = this.layout;
  /** @type {Array.<ol.geom.LineString>} */
  var lineStrings = [];
  var offset = 0;
  var i, ii;
  for (i = 0, ii = ends.length; i < ii; ++i) {
    var end = ends[i];
    var lineString = new ol.geom.LineString(null);
    lineString.setFlatCoordinates(layout, flatCoordinates.slice(offset, end));
    lineStrings.push(lineString);
    offset = end;
  }
  return lineStrings;
};


/**
 * @return {Array.<number>} Flat midpoints.
 */
ol.geom.MultiLineString.prototype.getFlatMidpoints = function() {
  var midpoints = [];
  var flatCoordinates = this.flatCoordinates;
  var offset = 0;
  var ends = this.ends_;
  var stride = this.stride;
  var i, ii;
  for (i = 0, ii = ends.length; i < ii; ++i) {
    var end = ends[i];
    var midpoint = ol.geom.flat.interpolate.lineString(
        flatCoordinates, offset, end, stride, 0.5);
    ol.array.extend(midpoints, midpoint);
    offset = end;
  }
  return midpoints;
};


/**
 * @inheritDoc
 */
ol.geom.MultiLineString.prototype.getSimplifiedGeometryInternal = function(squaredTolerance) {
  var simplifiedFlatCoordinates = [];
  var simplifiedEnds = [];
  simplifiedFlatCoordinates.length = ol.geom.flat.simplify.douglasPeuckers(
      this.flatCoordinates, 0, this.ends_, this.stride, squaredTolerance,
      simplifiedFlatCoordinates, 0, simplifiedEnds);
  var simplifiedMultiLineString = new ol.geom.MultiLineString(null);
  simplifiedMultiLineString.setFlatCoordinates(
      ol.geom.GeometryLayout.XY, simplifiedFlatCoordinates, simplifiedEnds);
  return simplifiedMultiLineString;
};


/**
 * @inheritDoc
 * @api stable
 */
ol.geom.MultiLineString.prototype.getType = function() {
  return ol.geom.GeometryType.MULTI_LINE_STRING;
};


/**
 * @inheritDoc
 * @api stable
 */
ol.geom.MultiLineString.prototype.intersectsExtent = function(extent) {
  return ol.geom.flat.intersectsextent.lineStrings(
      this.flatCoordinates, 0, this.ends_, this.stride, extent);
};


/**
 * Set the coordinates of the multilinestring.
 * @param {Array.<Array.<ol.Coordinate>>} coordinates Coordinates.
 * @param {ol.geom.GeometryLayout=} opt_layout Layout.
 * @api stable
 */
ol.geom.MultiLineString.prototype.setCoordinates = function(coordinates, opt_layout) {
  if (!coordinates) {
    this.setFlatCoordinates(ol.geom.GeometryLayout.XY, null, this.ends_);
  } else {
    this.setLayout(opt_layout, coordinates, 2);
    if (!this.flatCoordinates) {
      this.flatCoordinates = [];
    }
    var ends = ol.geom.flat.deflate.coordinatess(
        this.flatCoordinates, 0, coordinates, this.stride, this.ends_);
    this.flatCoordinates.length = ends.length === 0 ? 0 : ends[ends.length - 1];
    this.changed();
  }
};


/**
 * @param {ol.geom.GeometryLayout} layout Layout.
 * @param {Array.<number>} flatCoordinates Flat coordinates.
 * @param {Array.<number>} ends Ends.
 */
ol.geom.MultiLineString.prototype.setFlatCoordinates = function(layout, flatCoordinates, ends) {
  if (!flatCoordinates) {
    goog.asserts.assert(ends && ends.length === 0,
        'ends must be truthy and ends.length should be 0');
  } else if (ends.length === 0) {
    goog.asserts.assert(flatCoordinates.length === 0,
        'flatCoordinates should be an empty array');
  } else {
    goog.asserts.assert(flatCoordinates.length == ends[ends.length - 1],
        'length of flatCoordinates array should match the last value of ends');
  }
  this.setFlatCoordinatesInternal(layout, flatCoordinates);
  this.ends_ = ends;
  this.changed();
};


/**
 * @param {Array.<ol.geom.LineString>} lineStrings LineStrings.
 */
ol.geom.MultiLineString.prototype.setLineStrings = function(lineStrings) {
  var layout = this.getLayout();
  var flatCoordinates = [];
  var ends = [];
  var i, ii;
  for (i = 0, ii = lineStrings.length; i < ii; ++i) {
    var lineString = lineStrings[i];
    if (i === 0) {
      layout = lineString.getLayout();
    } else {
      // FIXME better handle the case of non-matching layouts
      goog.asserts.assert(lineString.getLayout() == layout,
          'layout of lineString should match layout');
    }
    ol.array.extend(flatCoordinates, lineString.getFlatCoordinates());
    ends.push(flatCoordinates.length);
  }
  this.setFlatCoordinates(layout, flatCoordinates, ends);
};
