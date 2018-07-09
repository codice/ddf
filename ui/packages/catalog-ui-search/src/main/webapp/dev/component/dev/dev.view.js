const Marionette = require('marionette');
const template = require('./dev.hbs');
const CustomElements = require('js/CustomElements');
const router = require('component/router/router');
const TabsModel = require('component/tabs/tabs');
const TabsView = require('component/tabs/tabs.view');
const GuideView = require('dev/component/guide/guide.view');
const AboutView = require('dev/component/about/about.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('dev-dev'),
    regions: {
        tabs: '> .content'
    },
    onBeforeShow() {
        this.tabs.show(new TabsView({
            model: new TabsModel({
                tabs: {
                    'About': AboutView,
                    'Guide': GuideView
                }
            })
        }), {
            replaceElement: true
        });
    }
});