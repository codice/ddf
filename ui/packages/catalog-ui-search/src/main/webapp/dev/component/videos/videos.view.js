const Marionette = require('marionette');
const template = require('./videos.hbs');
const CustomElements = require('js/CustomElements');

module.exports = Marionette.LayoutView.extend({
    template: template,
    className: 'pad-bottom',
    tagName: CustomElements.register('dev-videos')
});