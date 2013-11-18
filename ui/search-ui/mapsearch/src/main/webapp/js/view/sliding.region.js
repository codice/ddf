/*global define*/

define(function (require) {
    "use strict";
    var Marionette = require('marionette');

    require('jqueryui');

    var Region = Marionette.Region.extend({
        open: function (view) {
            // src  example
//           this.$el.empty().append(view.el);
            this.$el.perfectScrollbar('destroy');
            this.$el.hide();
            // TODO:  find appropriate animation
//            this.$el.hide('slide',{direction:'left'},1000);
            this.$el.html(view.el);
            console.log('sliding view down');

            this.$el.slideDown("fast");
            this.$el.perfectScrollbar();
        }
    });

    return Region;
});
