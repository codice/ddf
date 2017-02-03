goog.provide('ol.interaction.Draw');
goog.provide('ol.interaction.DrawEvent');
goog.provide('ol.interaction.DrawEventType');
goog.provide('ol.interaction.DrawMode');

goog.require('goog.asserts');
goog.require('ol.events');
goog.require('ol.events.Event');
goog.require('ol.Collection');
goog.require('ol.Feature');
goog.require('ol.MapBrowserEvent');
goog.require('ol.MapBrowserEvent.EventType');
goog.require('ol.Object');
goog.require('ol.coordinate');
goog.require('ol.functions');
goog.require('ol.events.condition');
goog.require('ol.geom.Circle');
goog.require('ol.geom.GeometryType');
goog.require('ol.geom.LineString');
goog.require('ol.geom.MultiLineString');
goog.require('ol.geom.MultiPoint');
goog.require('ol.geom.MultiPolygon');
goog.require('ol.geom.Point');
goog.require('ol.geom.Polygon');
goog.require('ol.geom.SimpleGeometry');
goog.require('ol.interaction.InteractionProperty');
goog.require('ol.interaction.Pointer');
goog.require('ol.layer.Vector');
goog.require('ol.source.Vector');


/**
 * @enum {string}
 */
ol.interaction.DrawEventType = {
  /**
   * Triggered upon feature draw start
   * @event ol.interaction.DrawEvent#drawstart
   * @api stable
   */
  DRAWSTART: 'drawstart',
  /**
   * Triggered upon feature draw end
   * @event ol.interaction.DrawEvent#drawend
   * @api stable
   */
  DRAWEND: 'drawend'
};


/**
 * @classdesc
 * Events emitted by {@link ol.interaction.Draw} instances are instances of
 * this type.
 *
 * @constructor
 * @extends {ol.events.Event}
 * @implements {oli.DrawEvent}
 * @param {ol.interaction.DrawEventType} type Type.
 * @param {ol.Feature} feature The feature drawn.
 */
ol.interaction.DrawEvent = function(type, feature) {

  ol.events.Event.call(this, type);

  /**
   * The feature being drawn.
   * @type {ol.Feature}
   * @api stable
   */
  this.feature = feature;

};
ol.inherits(ol.interaction.DrawEvent, ol.events.Event);


/**
 * @classdesc
 * Interaction for drawing feature geometries.
 *
 * @constructor
 * @extends {ol.interaction.Pointer}
 * @fires ol.interaction.DrawEvent
 * @param {olx.interaction.DrawOptions} options Options.
 * @api stable
 */
ol.interaction.Draw = function(options) {

  ol.interaction.Pointer.call(this, {
    handleDownEvent: ol.interaction.Draw.handleDownEvent_,
    handleEvent: ol.interaction.Draw.handleEvent,
    handleUpEvent: ol.interaction.Draw.handleUpEvent_
  });

  /**
   * @type {ol.Pixel}
   * @private
   */
  this.downPx_ = null;

  /**
   * @type {boolean}
   * @private
   */
  this.freehand_ = false;

  /**
   * Target source for drawn features.
   * @type {ol.source.Vector}
   * @private
   */
  this.source_ = options.source ? options.source : null;

  /**
   * Target collection for drawn features.
   * @type {ol.Collection.<ol.Feature>}
   * @private
   */
  this.features_ = options.features ? options.features : null;

  /**
   * Pixel distance for snapping.
   * @type {number}
   * @private
   */
  this.snapTolerance_ = options.snapTolerance ? options.snapTolerance : 12;

  /**
   * Geometry type.
   * @type {ol.geom.GeometryType}
   * @private
   */
  this.type_ = options.type;

  /**
   * Drawing mode (derived from geometry type.
   * @type {ol.interaction.DrawMode}
   * @private
   */
  this.mode_ = ol.interaction.Draw.getMode_(this.type_);

  /**
   * The number of points that must be drawn before a polygon ring or line
   * string can be finished.  The default is 3 for polygon rings and 2 for
   * line strings.
   * @type {number}
   * @private
   */
  this.minPoints_ = options.minPoints ?
      options.minPoints :
      (this.mode_ === ol.interaction.DrawMode.POLYGON ? 3 : 2);

  /**
   * The number of points that can be drawn before a polygon ring or line string
   * is finished. The default is no restriction.
   * @type {number}
   * @private
   */
  this.maxPoints_ = options.maxPoints ? options.maxPoints : Infinity;

  /**
   * A function to decide if a potential finish coordinate is permissable
   * @private
   * @type {ol.events.ConditionType}
   */
  this.finishCondition_ = options.finishCondition ? options.finishCondition : ol.functions.TRUE;

  var geometryFunction = options.geometryFunction;
  if (!geometryFunction) {
    if (this.type_ === ol.geom.GeometryType.CIRCLE) {
      /**
       * @param {ol.Coordinate|Array.<ol.Coordinate>|Array.<Array.<ol.Coordinate>>} coordinates
       *     The coordinates.
       * @param {ol.geom.SimpleGeometry=} opt_geometry Optional geometry.
       * @return {ol.geom.SimpleGeometry} A geometry.
       */
      geometryFunction = function(coordinates, opt_geometry) {
        var circle = opt_geometry ? opt_geometry :
            new ol.geom.Circle([NaN, NaN]);
        goog.asserts.assertInstanceof(circle, ol.geom.Circle,
            'geometry must be an ol.geom.Circle');
        var squaredLength = ol.coordinate.squaredDistance(
            coordinates[0], coordinates[1]);
        circle.setCenterAndRadius(coordinates[0], Math.sqrt(squaredLength));
        return circle;
      };
    } else {
      var Constructor;
      var mode = this.mode_;
      if (mode === ol.interaction.DrawMode.POINT) {
        Constructor = ol.geom.Point;
      } else if (mode === ol.interaction.DrawMode.LINE_STRING) {
        Constructor = ol.geom.LineString;
      } else if (mode === ol.interaction.DrawMode.POLYGON) {
        Constructor = ol.geom.Polygon;
      }
      /**
       * @param {ol.Coordinate|Array.<ol.Coordinate>|Array.<Array.<ol.Coordinate>>} coordinates
       *     The coordinates.
       * @param {ol.geom.SimpleGeometry=} opt_geometry Optional geometry.
       * @return {ol.geom.SimpleGeometry} A geometry.
       */
      geometryFunction = function(coordinates, opt_geometry) {
        var geometry = opt_geometry;
        if (geometry) {
          geometry.setCoordinates(coordinates);
        } else {
          geometry = new Constructor(coordinates);
        }
        return geometry;
      };
    }
  }

  /**
   * @type {ol.interaction.DrawGeometryFunctionType}
   * @private
   */
  this.geometryFunction_ = geometryFunction;

  /**
   * Finish coordinate for the feature (first point for polygons, last point for
   * linestrings).
   * @type {ol.Coordinate}
   * @private
   */
  this.finishCoordinate_ = null;

  /**
   * Sketch feature.
   * @type {ol.Feature}
   * @private
   */
  this.sketchFeature_ = null;

  /**
   * Sketch point.
   * @type {ol.Feature}
   * @private
   */
  this.sketchPoint_ = null;

  /**
   * Sketch coordinates. Used when drawing a line or polygon.
   * @type {ol.Coordinate|Array.<ol.Coordinate>|Array.<Array.<ol.Coordinate>>}
   * @private
   */
  this.sketchCoords_ = null;

  /**
   * Sketch line. Used when drawing polygon.
   * @type {ol.Feature}
   * @private
   */
  this.sketchLine_ = null;

  /**
   * Sketch line coordinates. Used when drawing a polygon or circle.
   * @type {Array.<ol.Coordinate>}
   * @private
   */
  this.sketchLineCoords_ = null;

  /**
   * Squared tolerance for handling up events.  If the squared distance
   * between a down and up event is greater than this tolerance, up events
   * will not be handled.
   * @type {number}
   * @private
   */
  this.squaredClickTolerance_ = options.clickTolerance ?
      options.clickTolerance * options.clickTolerance : 36;

  /**
   * Draw overlay where our sketch features are drawn.
   * @type {ol.layer.Vector}
   * @private
   */
  this.overlay_ = new ol.layer.Vector({
    source: new ol.source.Vector({
      useSpatialIndex: false,
      wrapX: options.wrapX ? options.wrapX : false
    }),
    style: options.style ? options.style :
        ol.interaction.Draw.getDefaultStyleFunction()
  });

  /**
   * Name of the geometry attribute for newly created features.
   * @type {string|undefined}
   * @private
   */
  this.geometryName_ = options.geometryName;

  /**
   * @private
   * @type {ol.events.ConditionType}
   */
  this.condition_ = options.condition ?
      options.condition : ol.events.condition.noModifierKeys;

  /**
   * @private
   * @type {ol.events.ConditionType}
   */
  this.freehandCondition_ = options.freehandCondition ?
      options.freehandCondition : ol.events.condition.shiftKeyOnly;

  ol.events.listen(this,
      ol.Object.getChangeEventType(ol.interaction.InteractionProperty.ACTIVE),
      this.updateState_, this);

};
ol.inherits(ol.interaction.Draw, ol.interaction.Pointer);


/**
 * @return {ol.style.StyleFunction} Styles.
 */
ol.interaction.Draw.getDefaultStyleFunction = function() {
  var styles = ol.style.createDefaultEditingStyles();
  return function(feature, resolution) {
    return styles[feature.getGeometry().getType()];
  };
};


/**
 * @inheritDoc
 */
ol.interaction.Draw.prototype.setMap = function(map) {
  ol.interaction.Pointer.prototype.setMap.call(this, map);
  this.updateState_();
};


/**
 * Handles the {@link ol.MapBrowserEvent map browser event} and may actually
 * draw or finish the drawing.
 * @param {ol.MapBrowserEvent} mapBrowserEvent Map browser event.
 * @return {boolean} `false` to stop event propagation.
 * @this {ol.interaction.Draw}
 * @api
 */
ol.interaction.Draw.handleEvent = function(mapBrowserEvent) {
  if ((this.mode_ === ol.interaction.DrawMode.LINE_STRING ||
    this.mode_ === ol.interaction.DrawMode.POLYGON) &&
    this.freehandCondition_(mapBrowserEvent)) {
    this.freehand_ = true;
  }
  var pass = !this.freehand_;
  if (this.freehand_ &&
      mapBrowserEvent.type === ol.MapBrowserEvent.EventType.POINTERDRAG) {
    this.addToDrawing_(mapBrowserEvent);
    pass = false;
  } else if (mapBrowserEvent.type ===
      ol.MapBrowserEvent.EventType.POINTERMOVE) {
    pass = this.handlePointerMove_(mapBrowserEvent);
  } else if (mapBrowserEvent.type === ol.MapBrowserEvent.EventType.DBLCLICK) {
    pass = false;
  }
  return ol.interaction.Pointer.handleEvent.call(this, mapBrowserEvent) && pass;
};


/**
 * @param {ol.MapBrowserPointerEvent} event Event.
 * @return {boolean} Start drag sequence?
 * @this {ol.interaction.Draw}
 * @private
 */
ol.interaction.Draw.handleDownEvent_ = function(event) {
  if (this.condition_(event)) {
    this.downPx_ = event.pixel;
    return true;
  } else if (this.freehand_) {
    this.downPx_ = event.pixel;
    if (!this.finishCoordinate_) {
      this.startDrawing_(event);
    }
    return true;
  } else {
    return false;
  }
};


/**
 * @param {ol.MapBrowserPointerEvent} event Event.
 * @return {boolean} Stop drag sequence?
 * @this {ol.interaction.Draw}
 * @private
 */
ol.interaction.Draw.handleUpEvent_ = function(event) {
  this.freehand_ = false;
  var downPx = this.downPx_;
  var clickPx = event.pixel;
  var dx = downPx[0] - clickPx[0];
  var dy = downPx[1] - clickPx[1];
  var squaredDistance = dx * dx + dy * dy;
  var pass = true;
  if (squaredDistance <= this.squaredClickTolerance_) {
    this.handlePointerMove_(event);
    if (!this.finishCoordinate_) {
      this.startDrawing_(event);
      if (this.mode_ === ol.interaction.DrawMode.POINT) {
        this.finishDrawing();
      }
    } else if (this.mode_ === ol.interaction.DrawMode.CIRCLE) {
      this.finishDrawing();
    } else if (this.atFinish_(event)) {
      if (this.finishCondition_(event)) {
        this.finishDrawing();
      }
    } else {
      this.addToDrawing_(event);
    }
    pass = false;
  }
  return pass;
};


/**
 * Handle move events.
 * @param {ol.MapBrowserEvent} event A move event.
 * @return {boolean} Pass the event to other interactions.
 * @private
 */
ol.interaction.Draw.prototype.handlePointerMove_ = function(event) {
  if (this.finishCoordinate_) {
    this.modifyDrawing_(event);
  } else {
    this.createOrUpdateSketchPoint_(event);
  }
  return true;
};


/**
 * Determine if an event is within the snapping tolerance of the start coord.
 * @param {ol.MapBrowserEvent} event Event.
 * @return {boolean} The event is within the snapping tolerance of the start.
 * @private
 */
ol.interaction.Draw.prototype.atFinish_ = function(event) {
  var at = false;
  if (this.sketchFeature_) {
    var potentiallyDone = false;
    var potentiallyFinishCoordinates = [this.finishCoordinate_];
    if (this.mode_ === ol.interaction.DrawMode.LINE_STRING) {
      potentiallyDone = this.sketchCoords_.length > this.minPoints_;
    } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
      potentiallyDone = this.sketchCoords_[0].length >
          this.minPoints_;
      potentiallyFinishCoordinates = [this.sketchCoords_[0][0],
        this.sketchCoords_[0][this.sketchCoords_[0].length - 2]];
    }
    if (potentiallyDone) {
      var map = event.map;
      for (var i = 0, ii = potentiallyFinishCoordinates.length; i < ii; i++) {
        var finishCoordinate = potentiallyFinishCoordinates[i];
        var finishPixel = map.getPixelFromCoordinate(finishCoordinate);
        var pixel = event.pixel;
        var dx = pixel[0] - finishPixel[0];
        var dy = pixel[1] - finishPixel[1];
        var freehand = this.freehand_ && this.freehandCondition_(event);
        var snapTolerance = freehand ? 1 : this.snapTolerance_;
        at = Math.sqrt(dx * dx + dy * dy) <= snapTolerance;
        if (at) {
          this.finishCoordinate_ = finishCoordinate;
          break;
        }
      }
    }
  }
  return at;
};


/**
 * @param {ol.MapBrowserEvent} event Event.
 * @private
 */
ol.interaction.Draw.prototype.createOrUpdateSketchPoint_ = function(event) {
  var coordinates = event.coordinate.slice();
  if (!this.sketchPoint_) {
    this.sketchPoint_ = new ol.Feature(new ol.geom.Point(coordinates));
    this.updateSketchFeatures_();
  } else {
    var sketchPointGeom = this.sketchPoint_.getGeometry();
    goog.asserts.assertInstanceof(sketchPointGeom, ol.geom.Point,
        'sketchPointGeom should be an ol.geom.Point');
    sketchPointGeom.setCoordinates(coordinates);
  }
};


/**
 * Start the drawing.
 * @param {ol.MapBrowserEvent} event Event.
 * @private
 */
ol.interaction.Draw.prototype.startDrawing_ = function(event) {
  var start = event.coordinate;
  this.finishCoordinate_ = start;
  if (this.mode_ === ol.interaction.DrawMode.POINT) {
    this.sketchCoords_ = start.slice();
  } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
    this.sketchCoords_ = [[start.slice(), start.slice()]];
    this.sketchLineCoords_ = this.sketchCoords_[0];
  } else {
    this.sketchCoords_ = [start.slice(), start.slice()];
    if (this.mode_ === ol.interaction.DrawMode.CIRCLE) {
      this.sketchLineCoords_ = this.sketchCoords_;
    }
  }
  if (this.sketchLineCoords_) {
    this.sketchLine_ = new ol.Feature(
        new ol.geom.LineString(this.sketchLineCoords_));
  }
  var geometry = this.geometryFunction_(this.sketchCoords_);
  goog.asserts.assert(geometry !== undefined, 'geometry should be defined');
  this.sketchFeature_ = new ol.Feature();
  if (this.geometryName_) {
    this.sketchFeature_.setGeometryName(this.geometryName_);
  }
  this.sketchFeature_.setGeometry(geometry);
  this.updateSketchFeatures_();
  this.dispatchEvent(new ol.interaction.DrawEvent(
      ol.interaction.DrawEventType.DRAWSTART, this.sketchFeature_));
};


/**
 * Modify the drawing.
 * @param {ol.MapBrowserEvent} event Event.
 * @private
 */
ol.interaction.Draw.prototype.modifyDrawing_ = function(event) {
  var coordinate = event.coordinate;
  var geometry = this.sketchFeature_.getGeometry();
  goog.asserts.assertInstanceof(geometry, ol.geom.SimpleGeometry,
      'geometry should be ol.geom.SimpleGeometry or subclass');
  var coordinates, last;
  if (this.mode_ === ol.interaction.DrawMode.POINT) {
    last = this.sketchCoords_;
  } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
    coordinates = this.sketchCoords_[0];
    last = coordinates[coordinates.length - 1];
    if (this.atFinish_(event)) {
      // snap to finish
      coordinate = this.finishCoordinate_.slice();
    }
  } else {
    coordinates = this.sketchCoords_;
    last = coordinates[coordinates.length - 1];
  }
  last[0] = coordinate[0];
  last[1] = coordinate[1];
  goog.asserts.assert(this.sketchCoords_, 'sketchCoords_ expected');
  this.geometryFunction_(this.sketchCoords_, geometry);
  if (this.sketchPoint_) {
    var sketchPointGeom = this.sketchPoint_.getGeometry();
    goog.asserts.assertInstanceof(sketchPointGeom, ol.geom.Point,
        'sketchPointGeom should be an ol.geom.Point');
    sketchPointGeom.setCoordinates(coordinate);
  }
  var sketchLineGeom;
  if (geometry instanceof ol.geom.Polygon &&
      this.mode_ !== ol.interaction.DrawMode.POLYGON) {
    if (!this.sketchLine_) {
      this.sketchLine_ = new ol.Feature(new ol.geom.LineString(null));
    }
    var ring = geometry.getLinearRing(0);
    sketchLineGeom = this.sketchLine_.getGeometry();
    goog.asserts.assertInstanceof(sketchLineGeom, ol.geom.LineString,
        'sketchLineGeom must be an ol.geom.LineString');
    sketchLineGeom.setFlatCoordinates(
        ring.getLayout(), ring.getFlatCoordinates());
  } else if (this.sketchLineCoords_) {
    sketchLineGeom = this.sketchLine_.getGeometry();
    goog.asserts.assertInstanceof(sketchLineGeom, ol.geom.LineString,
        'sketchLineGeom must be an ol.geom.LineString');
    sketchLineGeom.setCoordinates(this.sketchLineCoords_);
  }
  this.updateSketchFeatures_();
};


/**
 * Add a new coordinate to the drawing.
 * @param {ol.MapBrowserEvent} event Event.
 * @private
 */
ol.interaction.Draw.prototype.addToDrawing_ = function(event) {
  var coordinate = event.coordinate;
  var geometry = this.sketchFeature_.getGeometry();
  goog.asserts.assertInstanceof(geometry, ol.geom.SimpleGeometry,
      'geometry must be an ol.geom.SimpleGeometry');
  var done;
  var coordinates;
  if (this.mode_ === ol.interaction.DrawMode.LINE_STRING) {
    this.finishCoordinate_ = coordinate.slice();
    coordinates = this.sketchCoords_;
    coordinates.push(coordinate.slice());
    done = coordinates.length > this.maxPoints_;
    this.geometryFunction_(coordinates, geometry);
  } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
    coordinates = this.sketchCoords_[0];
    coordinates.push(coordinate.slice());
    done = coordinates.length > this.maxPoints_;
    if (done) {
      this.finishCoordinate_ = coordinates[0];
    }
    this.geometryFunction_(this.sketchCoords_, geometry);
  }
  this.updateSketchFeatures_();
  if (done) {
    this.finishDrawing();
  }
};


/**
 * Remove last point of the feature currently being drawn.
 * @api
 */
ol.interaction.Draw.prototype.removeLastPoint = function() {
  var geometry = this.sketchFeature_.getGeometry();
  goog.asserts.assertInstanceof(geometry, ol.geom.SimpleGeometry,
      'geometry must be an ol.geom.SimpleGeometry');
  var coordinates, sketchLineGeom;
  if (this.mode_ === ol.interaction.DrawMode.LINE_STRING) {
    coordinates = this.sketchCoords_;
    coordinates.splice(-2, 1);
    this.geometryFunction_(coordinates, geometry);
  } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
    coordinates = this.sketchCoords_[0];
    coordinates.splice(-2, 1);
    sketchLineGeom = this.sketchLine_.getGeometry();
    goog.asserts.assertInstanceof(sketchLineGeom, ol.geom.LineString,
        'sketchLineGeom must be an ol.geom.LineString');
    sketchLineGeom.setCoordinates(coordinates);
    this.geometryFunction_(this.sketchCoords_, geometry);
  }

  if (coordinates.length === 0) {
    this.finishCoordinate_ = null;
  }

  this.updateSketchFeatures_();
};


/**
 * Stop drawing and add the sketch feature to the target layer.
 * The {@link ol.interaction.DrawEventType.DRAWEND} event is dispatched before
 * inserting the feature.
 * @api
 */
ol.interaction.Draw.prototype.finishDrawing = function() {
  var sketchFeature = this.abortDrawing_();
  goog.asserts.assert(sketchFeature, 'sketchFeature expected to be truthy');
  var coordinates = this.sketchCoords_;
  var geometry = sketchFeature.getGeometry();
  goog.asserts.assertInstanceof(geometry, ol.geom.SimpleGeometry,
      'geometry must be an ol.geom.SimpleGeometry');
  if (this.mode_ === ol.interaction.DrawMode.LINE_STRING) {
    // remove the redundant last point
    coordinates.pop();
    this.geometryFunction_(coordinates, geometry);
  } else if (this.mode_ === ol.interaction.DrawMode.POLYGON) {
    // When we finish drawing a polygon on the last point,
    // the last coordinate is duplicated as for LineString
    // we force the replacement by the first point
    coordinates[0].pop();
    coordinates[0].push(coordinates[0][0]);
    this.geometryFunction_(coordinates, geometry);
  }

  // cast multi-part geometries
  if (this.type_ === ol.geom.GeometryType.MULTI_POINT) {
    sketchFeature.setGeometry(new ol.geom.MultiPoint([coordinates]));
  } else if (this.type_ === ol.geom.GeometryType.MULTI_LINE_STRING) {
    sketchFeature.setGeometry(new ol.geom.MultiLineString([coordinates]));
  } else if (this.type_ === ol.geom.GeometryType.MULTI_POLYGON) {
    sketchFeature.setGeometry(new ol.geom.MultiPolygon([coordinates]));
  }

  // First dispatch event to allow full set up of feature
  this.dispatchEvent(new ol.interaction.DrawEvent(
      ol.interaction.DrawEventType.DRAWEND, sketchFeature));

  // Then insert feature
  if (this.features_) {
    this.features_.push(sketchFeature);
  }
  if (this.source_) {
    this.source_.addFeature(sketchFeature);
  }
};


/**
 * Stop drawing without adding the sketch feature to the target layer.
 * @return {ol.Feature} The sketch feature (or null if none).
 * @private
 */
ol.interaction.Draw.prototype.abortDrawing_ = function() {
  this.finishCoordinate_ = null;
  var sketchFeature = this.sketchFeature_;
  if (sketchFeature) {
    this.sketchFeature_ = null;
    this.sketchPoint_ = null;
    this.sketchLine_ = null;
    this.overlay_.getSource().clear(true);
  }
  return sketchFeature;
};


/**
 * Extend an existing geometry by adding additional points. This only works
 * on features with `LineString` geometries, where the interaction will
 * extend lines by adding points to the end of the coordinates array.
 * @param {!ol.Feature} feature Feature to be extended.
 * @api
 */
ol.interaction.Draw.prototype.extend = function(feature) {
  var geometry = feature.getGeometry();
  goog.asserts.assert(this.mode_ == ol.interaction.DrawMode.LINE_STRING,
      'interaction mode must be "line"');
  goog.asserts.assert(geometry, 'feature must have a geometry');
  goog.asserts.assert(geometry.getType() == ol.geom.GeometryType.LINE_STRING,
      'feature geometry must be a line string');
  var lineString = /** @type {ol.geom.LineString} */ (geometry);
  this.sketchFeature_ = feature;
  this.sketchCoords_ = lineString.getCoordinates();
  var last = this.sketchCoords_[this.sketchCoords_.length - 1];
  this.finishCoordinate_ = last.slice();
  this.sketchCoords_.push(last.slice());
  this.updateSketchFeatures_();
  this.dispatchEvent(new ol.interaction.DrawEvent(
      ol.interaction.DrawEventType.DRAWSTART, this.sketchFeature_));
};


/**
 * @inheritDoc
 */
ol.interaction.Draw.prototype.shouldStopEvent = ol.functions.FALSE;


/**
 * Redraw the sketch features.
 * @private
 */
ol.interaction.Draw.prototype.updateSketchFeatures_ = function() {
  var sketchFeatures = [];
  if (this.sketchFeature_) {
    sketchFeatures.push(this.sketchFeature_);
  }
  if (this.sketchLine_) {
    sketchFeatures.push(this.sketchLine_);
  }
  if (this.sketchPoint_) {
    sketchFeatures.push(this.sketchPoint_);
  }
  var overlaySource = this.overlay_.getSource();
  overlaySource.clear(true);
  overlaySource.addFeatures(sketchFeatures);
};


/**
 * @private
 */
ol.interaction.Draw.prototype.updateState_ = function() {
  var map = this.getMap();
  var active = this.getActive();
  if (!map || !active) {
    this.abortDrawing_();
  }
  this.overlay_.setMap(active ? map : null);
};


/**
 * Create a `geometryFunction` for `mode: 'Circle'` that will create a regular
 * polygon with a user specified number of sides and start angle instead of an
 * `ol.geom.Circle` geometry.
 * @param {number=} opt_sides Number of sides of the regular polygon. Default is
 *     32.
 * @param {number=} opt_angle Angle of the first point in radians. 0 means East.
 *     Default is the angle defined by the heading from the center of the
 *     regular polygon to the current pointer position.
 * @return {ol.interaction.DrawGeometryFunctionType} Function that draws a
 *     polygon.
 * @api
 */
ol.interaction.Draw.createRegularPolygon = function(opt_sides, opt_angle) {
  return (
      /**
       * @param {ol.Coordinate|Array.<ol.Coordinate>|Array.<Array.<ol.Coordinate>>} coordinates
       * @param {ol.geom.SimpleGeometry=} opt_geometry
       * @return {ol.geom.SimpleGeometry}
       */
      function(coordinates, opt_geometry) {
        var center = coordinates[0];
        var end = coordinates[1];
        var radius = Math.sqrt(
            ol.coordinate.squaredDistance(center, end));
        var geometry = opt_geometry ? opt_geometry :
            ol.geom.Polygon.fromCircle(new ol.geom.Circle(center), opt_sides);
        goog.asserts.assertInstanceof(geometry, ol.geom.Polygon,
            'geometry must be a polygon');
        var angle = opt_angle ? opt_angle :
            Math.atan((end[1] - center[1]) / (end[0] - center[0]));
        ol.geom.Polygon.makeRegular(geometry, center, radius, angle);
        return geometry;
      }
  );
};


/**
 * Get the drawing mode.  The mode for mult-part geometries is the same as for
 * their single-part cousins.
 * @param {ol.geom.GeometryType} type Geometry type.
 * @return {ol.interaction.DrawMode} Drawing mode.
 * @private
 */
ol.interaction.Draw.getMode_ = function(type) {
  var mode;
  if (type === ol.geom.GeometryType.POINT ||
      type === ol.geom.GeometryType.MULTI_POINT) {
    mode = ol.interaction.DrawMode.POINT;
  } else if (type === ol.geom.GeometryType.LINE_STRING ||
      type === ol.geom.GeometryType.MULTI_LINE_STRING) {
    mode = ol.interaction.DrawMode.LINE_STRING;
  } else if (type === ol.geom.GeometryType.POLYGON ||
      type === ol.geom.GeometryType.MULTI_POLYGON) {
    mode = ol.interaction.DrawMode.POLYGON;
  } else if (type === ol.geom.GeometryType.CIRCLE) {
    mode = ol.interaction.DrawMode.CIRCLE;
  }
  goog.asserts.assert(mode !== undefined, 'mode should be defined');
  return mode;
};


/**
 * Draw mode.  This collapses multi-part geometry types with their single-part
 * cousins.
 * @enum {string}
 */
ol.interaction.DrawMode = {
  POINT: 'Point',
  LINE_STRING: 'LineString',
  POLYGON: 'Polygon',
  CIRCLE: 'Circle'
};
