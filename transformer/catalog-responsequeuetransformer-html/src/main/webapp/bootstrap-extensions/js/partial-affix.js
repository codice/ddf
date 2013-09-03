
!function ($) {

  "use strict"; // jshint ;_;

 /* PARTIAL-AFFIX CLASS DEFINITION
  * ====================== */

  var PartialAffix = function (element, options) {
    this.options = $.extend({}, $.fn.partialaffix.defaults, options)
    this.$window = $(window).on('scroll.partialaffix.data-api', $.proxy(this.checkPosition, this))
    this.$element = $(element)
    this.checkPosition()
  }

  PartialAffix.prototype.checkPosition = function () {
    if (!this.$element.is(':visible')) return;

    var scrollTop = this.$window.scrollTop()
      , windowHeight = this.$window.height()
	  , elementHeight = this.$element.height()
      , position = this.$element.offset()
      , offset = this.options.offset
      , offsetBottom = offset.bottom ? offset.bottom : 0
      , offsetTop = offset.top ? offset.top : 0;

    if (typeof offset != 'object') offsetBottom = offsetTop = offset;
    if (typeof offsetTop == 'function') offsetTop = offset.top();
    if (typeof offsetBottom == 'function') offsetBottom = offset.bottom();

	var newTop = offsetTop + scrollTop;
	var totalSize = offsetTop + elementHeight + offsetBottom;
	
	var elementId = this.$element.attr('id');
	if(! elementId) {
		elementId = "";
	}
	if(0 == $("#" + elementId + "_hidden").length) {
		this.$element.parent().append("<div id='"+ elementId + "_hidden'></div>");
	}
	if($("#" + elementId + "_hidden").height() !== totalSize) {
		$("#" + elementId + "_hidden").height(totalSize);
	}
	
	if(windowHeight < totalSize) {
		var maxScroll = totalSize - windowHeight;
		newTop = scrollTop < maxScroll ? offsetTop : scrollTop + offsetTop - maxScroll;
	}

    this.partialaffixed = false;

	position.top = newTop;
	this.$element.offset(position);		
}


 /* PARTIAL-AFFIX PLUGIN DEFINITION
  * ======================= */

  $.fn.partialaffix = function (option) {
    return this.each(function () {
      var $this = $(this)
        , data = $this.data('partialaffix')
        , options = typeof option == 'object' && option
      if (!data) $this.data('partialaffix', (data = new PartialAffix(this, options)))
      if (typeof option == 'string') data[option]()
    })
  }

  $.fn.partialaffix.Constructor = PartialAffix

  $.fn.partialaffix.defaults = {
    offset: 0
  }

}(window.jQuery);