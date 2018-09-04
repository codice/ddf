const template = require('./button-guide.hbs');
const CustomElements = require('js/CustomElements');
const BaseGuideView = require('dev/component/base-guide/base-guide.view');

module.exports = BaseGuideView.extend({
    template: template,
    tagName: CustomElements.register('dev-button-guide'),
    styles: {
        button: require('!raw-loader!./button.less')   
    }
});