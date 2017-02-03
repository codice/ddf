// FIXME should possibly show tooltip when dragging?

goog.provide('ol.control.ZoomSlider');

goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('goog.style');
goog.require('ol.events');
goog.require('ol.events.Event');
goog.require('ol.events.EventType');
goog.require('ol.pointer.PointerEventHandler');
goog.require('ol.ViewHint');
goog.require('ol.animation');
goog.require('ol.control.Control');
goog.require('ol.css');
goog.require('ol.easing');
goog.require('ol.math');


/**
 * @classdesc
 * A slider type of control for zooming.
 *
 * Example:
 *
 *     map.addControl(new ol.control.ZoomSlider());
 *
 * @constructor
 * @extends {ol.control.Control}
 * @param {olx.control.ZoomSliderOptions=} opt_options Zoom slider options.
 * @api stable
 */
ol.control.ZoomSlider = function(opt_options) {

  var options = opt_options ? opt_options : {};

  /**
   * Will hold the current resolution of the view.
   *
   * @type {number|undefined}
   * @private
   */
  this.currentResolution_ = undefined;

  /**
   * The direction of the slider. Will be determined from actual display of the
   * container and defaults to ol.control.ZoomSlider.direction.VERTICAL.
   *
   * @type {ol.control.ZoomSlider.direction}
   * @private
   */
  this.direction_ = ol.control.ZoomSlider.direction.VERTICAL;

  /**
   * @type {boolean}
   * @private
   */
  this.dragging_;

  /**
   * @type {!Array.<ol.events.Key>}
   * @private
   */
  this.dragListenerKeys_ = [];

  /**
   * @type {number}
   * @private
   */
  this.heightLimit_ = 0;

  /**
   * @type {number}
   * @private
   */
  this.widthLimit_ = 0;

  /**
   * @type {number|undefined}
   * @private
   */
  this.previousX_;

  /**
   * @type {number|undefined}
   * @private
   */
  this.previousY_;

  /**
   * The calculated thumb size (border box plus margins).  Set when initSlider_
   * is called.
   * @type {ol.Size}
   * @private
   */
  this.thumbSize_ = null;

  /**
   * Whether the slider is initialized.
   * @type {boolean}
   * @private
   */
  this.sliderInitialized_ = false;

  /**
   * @type {number}
   * @private
   */
  this.duration_ = options.duration !== undefined ? options.duration : 200;

  var className = options.className !== undefined ? options.className : 'ol-zoomslider';
  var thumbElement = goog.dom.createDom('BUTTON', {
    'type': 'button',
    'class': className + '-thumb ' + ol.css.CLASS_UNSELECTABLE
  });
  var containerElement = goog.dom.createDom('DIV',
      [className, ol.css.CLASS_UNSELECTABLE, ol.css.CLASS_CONTROL],
      thumbElement);

  /**
   * @type {ol.pointer.PointerEventHandler}
   * @private
   */
  this.dragger_ = new ol.pointer.PointerEventHandler(containerElement);

  ol.events.listen(this.dragger_, ol.pointer.EventType.POINTERDOWN,
      this.handleDraggerStart_, this);
  ol.events.listen(this.dragger_, ol.pointer.EventType.POINTERMOVE,
      this.handleDraggerDrag_, this);
  ol.events.listen(this.dragger_, ol.pointer.EventType.POINTERUP,
      this.handleDraggerEnd_, this);

  ol.events.listen(containerElement, ol.events.EventType.CLICK,
      this.handleContainerClick_, this);
  ol.events.listen(thumbElement, ol.events.EventType.CLICK,
      ol.events.Event.stopPropagation);

  var render = options.render ? options.render : ol.control.ZoomSlider.render;

  ol.control.Control.call(this, {
    element: containerElement,
    render: render
  });
};
ol.inherits(ol.control.ZoomSlider, ol.control.Control);


/**
 * @inheritDoc
 */
ol.control.ZoomSlider.prototype.disposeInternal = function() {
  this.dragger_.dispose();
  ol.control.Control.prototype.disposeInternal.call(this);
};


/**
 * The enum for available directions.
 *
 * @enum {number}
 */
ol.control.ZoomSlider.direction = {
  VERTICAL: 0,
  HORIZONTAL: 1
};


/**
 * @inheritDoc
 */
ol.control.ZoomSlider.prototype.setMap = function(map) {
  ol.control.Control.prototype.setMap.call(this, map);
  if (map) {
    map.render();
  }
};


/**
 * Initializes the slider element. This will determine and set this controls
 * direction_ and also constrain the dragging of the thumb to always be within
 * the bounds of the container.
 *
 * @private
 */
ol.control.ZoomSlider.prototype.initSlider_ = function() {
  var container = this.element;
  var containerSize = goog.style.getSize(container);

  var thumb = container.firstElementChild;
  var thumbMargins = goog.style.getMarginBox(thumb);
  var thumbBorderBoxSize = goog.style.getBorderBoxSize(thumb);
  var thumbWidth = thumbBorderBoxSize.width +
      thumbMargins.right + thumbMargins.left;
  var thumbHeight = thumbBorderBoxSize.height +
      thumbMargins.top + thumbMargins.bottom;
  this.thumbSize_ = [thumbWidth, thumbHeight];

  if (containerSize.width > containerSize.height) {
    this.direction_ = ol.control.ZoomSlider.direction.HORIZONTAL;
    this.widthLimit_ = containerSize.width - thumbWidth;
  } else {
    this.direction_ = ol.control.ZoomSlider.direction.VERTICAL;
    this.heightLimit_ = containerSize.height - thumbHeight;
  }
  this.sliderInitialized_ = true;
};


/**
 * Update the zoomslider element.
 * @param {ol.MapEvent} mapEvent Map event.
 * @this {ol.control.ZoomSlider}
 * @api
 */
ol.control.ZoomSlider.render = function(mapEvent) {
  if (!mapEvent.frameState) {
    return;
  }
  goog.asserts.assert(mapEvent.frameState.viewState,
      'viewState should be defined');
  if (!this.sliderInitialized_) {
    this.initSlider_();
  }
  var res = mapEvent.frameState.viewState.resolution;
  if (res !== this.currentResolution_) {
    this.currentResolution_ = res;
    this.setThumbPosition_(res);
  }
};


/**
 * @param {Event} event The browser event to handle.
 * @private
 */
ol.control.ZoomSlider.prototype.handleContainerClick_ = function(event) {
  var map = this.getMap();
  var view = map.getView();
  var currentResolution = view.getResolution();
  goog.asserts.assert(currentResolution,
      'currentResolution should be defined');
  map.beforeRender(ol.animation.zoom({
    resolution: currentResolution,
    duration: this.duration_,
    easing: ol.easing.easeOut
  }));
  var relativePosition = this.getRelativePosition_(
      event.offsetX - this.thumbSize_[0] / 2,
      event.offsetY - this.thumbSize_[1] / 2);
  var resolution = this.getResolutionForPosition_(relativePosition);
  view.setResolution(view.constrainResolution(resolution));
};


/**
 * Handle dragger start events.
 * @param {ol.pointer.PointerEvent} event The drag event.
 * @private
 */
ol.control.ZoomSlider.prototype.handleDraggerStart_ = function(event) {
  if (!this.dragging_ &&
      event.originalEvent.target === this.element.firstElementChild) {
    this.getMap().getView().setHint(ol.ViewHint.INTERACTING, 1);
    this.previousX_ = event.clientX;
    this.previousY_ = event.clientY;
    this.dragging_ = true;

    if (this.dragListenerKeys_.length === 0) {
      var drag = this.handleDraggerDrag_;
      var end = this.handleDraggerEnd_;
      this.dragListenerKeys_.push(
        ol.events.listen(document, ol.events.EventType.MOUSEMOVE, drag, this),
        ol.events.listen(document, ol.events.EventType.TOUCHMOVE, drag, this),
        ol.events.listen(document, ol.pointer.EventType.POINTERMOVE, drag, this),
        ol.events.listen(document, ol.events.EventType.MOUSEUP, end, this),
        ol.events.listen(document, ol.events.EventType.TOUCHEND, end, this),
        ol.events.listen(document, ol.pointer.EventType.POINTERUP, end, this)
      );
    }
  }
};


/**
 * Handle dragger drag events.
 *
 * @param {ol.pointer.PointerEvent|Event} event The drag event.
 * @private
 */
ol.control.ZoomSlider.prototype.handleDraggerDrag_ = function(event) {
  if (this.dragging_) {
    var element = this.element.firstElementChild;
    var deltaX = event.clientX - this.previousX_ + parseInt(element.style.left, 10);
    var deltaY = event.clientY - this.previousY_ + parseInt(element.style.top, 10);
    var relativePosition = this.getRelativePosition_(deltaX, deltaY);
    this.currentResolution_ = this.getResolutionForPosition_(relativePosition);
    this.getMap().getView().setResolution(this.currentResolution_);
    this.setThumbPosition_(this.currentResolution_);
    this.previousX_ = event.clientX;
    this.previousY_ = event.clientY;
  }
};


/**
 * Handle dragger end events.
 * @param {ol.pointer.PointerEvent|Event} event The drag event.
 * @private
 */
ol.control.ZoomSlider.prototype.handleDraggerEnd_ = function(event) {
  if (this.dragging_) {
    var map = this.getMap();
    var view = map.getView();
    view.setHint(ol.ViewHint.INTERACTING, -1);
    goog.asserts.assert(this.currentResolution_,
        'this.currentResolution_ should be defined');
    map.beforeRender(ol.animation.zoom({
      resolution: this.currentResolution_,
      duration: this.duration_,
      easing: ol.easing.easeOut
    }));
    var resolution = view.constrainResolution(this.currentResolution_);
    view.setResolution(resolution);
    this.dragging_ = false;
    this.previousX_ = undefined;
    this.previousY_ = undefined;
    this.dragListenerKeys_.forEach(ol.events.unlistenByKey);
    this.dragListenerKeys_.length = 0;
  }
};


/**
 * Positions the thumb inside its container according to the given resolution.
 *
 * @param {number} res The res.
 * @private
 */
ol.control.ZoomSlider.prototype.setThumbPosition_ = function(res) {
  var position = this.getPositionForResolution_(res);
  var thumb = this.element.firstElementChild;

  if (this.direction_ == ol.control.ZoomSlider.direction.HORIZONTAL) {
    thumb.style.left = this.widthLimit_ * position + 'px';
  } else {
    thumb.style.top = this.heightLimit_ * position + 'px';
  }
};


/**
 * Calculates the relative position of the thumb given x and y offsets.  The
 * relative position scales from 0 to 1.  The x and y offsets are assumed to be
 * in pixel units within the dragger limits.
 *
 * @param {number} x Pixel position relative to the left of the slider.
 * @param {number} y Pixel position relative to the top of the slider.
 * @return {number} The relative position of the thumb.
 * @private
 */
ol.control.ZoomSlider.prototype.getRelativePosition_ = function(x, y) {
  var amount;
  if (this.direction_ === ol.control.ZoomSlider.direction.HORIZONTAL) {
    amount = x / this.widthLimit_;
  } else {
    amount = y / this.heightLimit_;
  }
  return ol.math.clamp(amount, 0, 1);
};


/**
 * Calculates the corresponding resolution of the thumb given its relative
 * position (where 0 is the minimum and 1 is the maximum).
 *
 * @param {number} position The relative position of the thumb.
 * @return {number} The corresponding resolution.
 * @private
 */
ol.control.ZoomSlider.prototype.getResolutionForPosition_ = function(position) {
  var fn = this.getMap().getView().getResolutionForValueFunction();
  return fn(1 - position);
};


/**
 * Determines the relative position of the slider for the given resolution.  A
 * relative position of 0 corresponds to the minimum view resolution.  A
 * relative position of 1 corresponds to the maximum view resolution.
 *
 * @param {number} res The resolution.
 * @return {number} The relative position value (between 0 and 1).
 * @private
 */
ol.control.ZoomSlider.prototype.getPositionForResolution_ = function(res) {
  var fn = this.getMap().getView().getValueForResolutionFunction();
  return 1 - fn(res);
};
