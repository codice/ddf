const Marionette = require('marionette');
const template = require('./guide.hbs');
const CustomElements = require('js/CustomElements');
const PropertyView = require('component/property/property.view');
const Property = require('component/property/property');
const CardGuideView = require('dev/component/card-guide/card-guide.view');
const ButtonGuideView = require('dev/component/button-guide/button-guide.view');
const StaticDropdownGuideView = require('dev/component/static-dropdown-guide/static-dropdown-guide.view');
const DropdownGuideView = require('dev/component/dropdown-guide/dropdown-guide.view');
const InputGuideView = require('dev/component/input-guide/input-guide.view');
const JSXGuideView = require('dev/component/jsx-guide/jsx-guide.view');
const RegionGuideView = require('dev/component/region-guide/region-guide.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('dev-guide'),
    className: 'pad-bottom',
    regions: {
        componentGuide: '> .container > .section > .component',
        componentDetails: '> .container > .component-details'
    },
    onBeforeShow() {
        this.componentGuide.show(new PropertyView({
            model: new Property({
                enumFiltering: true,
                showLabel: false,
                value: ['Card'],
                enum: [
                    {
                        label: 'Card',
                        value: 'Card'
                    }, 
                    {
                        label: 'Button',
                        value: 'Button'
                    },
                    {
                        label: 'Static Dropdowns (deprecated)',
                        value: 'Static Dropdowns'
                    },
                    {
                        label: 'Dropdowns',
                        value: 'Dropdowns',
                    },
                    {
                        label: 'Inputs',
                        value: 'Inputs'
                    },
                    {
                        label: 'JSX',
                        value: 'JSX'
                    },
                    {
                        label: 'Regions (Layout Views)',
                        value: 'Regions'
                    }
                ],
                id: 'component'
            })
        }));
        this.componentGuide.currentView.turnOnEditing();
        this.listenTo(this.componentGuide.currentView.model, 'change:value', this.updateComponentDetails);
        this.updateComponentDetails();
    },
    updateComponentDetails() {
        let componentToShow;
        switch(this.componentGuide.currentView.model.get('value')[0]) {
            case 'Card':
            componentToShow = CardGuideView;
            break;
            case 'Button':
            componentToShow = ButtonGuideView;
            break;
            case 'Dropdowns':
            componentToShow = DropdownGuideView;
            break;
            case 'Inputs':
            componentToShow = InputGuideView;
            break;
            case 'Static Dropdowns':
            componentToShow = StaticDropdownGuideView;
            break;
            case 'JSX':
            componentToShow = JSXGuideView;
            break;
            case 'Regions':
            componentToShow = RegionGuideView;
            break;
            default: 
            componentToShow = CardGuideView;
            break;
        }
        this.componentDetails.show(new componentToShow());
    }
});