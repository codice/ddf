goog.provide('ol.control.ZoomToExtent');

goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('ol.events');
goog.require('ol.events.EventType');
goog.require('ol.control.Control');
goog.require('ol.css');


/**
 * @classdesc
 * A button control which, when pressed, changes the map view to a specific
 * extent. To style this control use the css selector `.ol-zoom-extent`.
 *
 * @constructor
 * @extends {ol.control.Control}
 * @param {olx.control.ZoomToExtentOptions=} opt_options Options.
 * @api stable
 */
ol.control.ZoomToExtent = function(opt_options) {
  var options = opt_options ? opt_options : {};

  /**
   * @type {ol.Extent}
   * @private
   */
  this.extent_ = options.extent ? options.extent : null;

  var className = options.className !== undefined ? options.className :
      'ol-zoom-extent';

  var label = options.label !== undefined ? options.label : 'E';
  var tipLabel = options.tipLabel !== undefined ?
      options.tipLabel : 'Fit to extent';
  var button = goog.dom.createDom('BUTTON', {
    'type': 'button',
    'title': tipLabel
  }, label);

  ol.events.listen(button, ol.events.EventType.CLICK,
      this.handleClick_, this);

  var cssClasses = className + ' ' + ol.css.CLASS_UNSELECTABLE + ' ' +
      ol.css.CLASS_CONTROL;
  var element = goog.dom.createDom('DIV', cssClasses, button);

  ol.control.Control.call(this, {
    element: element,
    target: options.target
  });
};
ol.inherits(ol.control.ZoomToExtent, ol.control.Control);


/**
 * @param {Event} event The event to handle
 * @private
 */
ol.control.ZoomToExtent.prototype.handleClick_ = function(event) {
  event.preventDefault();
  this.handleZoomToExtent_();
};


/**
 * @private
 */
ol.control.ZoomToExtent.prototype.handleZoomToExtent_ = function() {
  var map = this.getMap();
  var view = map.getView();
  var extent = !this.extent_ ? view.getProjection().getExtent() : this.extent_;
  var size = map.getSize();
  goog.asserts.assert(size, 'size should be defined');
  view.fit(extent, size);
};
