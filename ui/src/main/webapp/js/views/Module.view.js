/*global define,setTimeout*/
define(function(require) {

    var Marionette = require('marionette'),
        Application = require('js/application');

    var ModuleView = Marionette.Layout.extend({
        template: 'tabs',
        className: 'relative',
        regions: {
            tabs: '#tabs',
            tabContent: '#tabContent'
        },
        onRender: function() {
            this.tabs.show(new Marionette.CollectionView({
                tagName: 'ul',
                className: 'nav nav-pills nav-stacked',
                collection: this.model.get('value'),
                itemView: Marionette.ItemView.extend({
                    tagName: 'li',
                    template: 'moduleTab',
                    onRender: function() {
                        if(this.model.get('active')) {
                            this.$el.addClass('active');
                        }
                    }
                })
            }));
            this.tabContent.show(new Marionette.CollectionView({
                tagName: 'div',
                className: 'tab-content',
                collection: this.model.get('value'),
                itemView: Marionette.ItemView.extend({
                    tagName: 'div',
                    className: 'tab-pane',
                    onRender: function() {
                        var view = this;
                        this.$el.attr('id', this.model.get('id'));
                        if(this.model.get('active')) {
                            this.$el.addClass('active');
                        }
                        //this dynamically requires in our modules based on wherever the model says they could be found
                        //check if we already have the module
                        if(this.model.get('iframeLocation') && this.model.get('iframeLocation') !== "") {
                            this.$el.html('<iframe src="' + this.model.get('iframeLocation') + '"></iframe>');
                        } else {
                            if(Application.App[this.model.get('name')]) {
                                //the require([]) function uses setTimeout internally to make this call asynchronously
                                //we need to do the same thing here so that everything is in place when the module starts
                                setTimeout(function() {
                                    Application.App[view.model.get('name')].start();
                                }, 0);
                            } else if(this.module && this.module.start) {
                                setTimeout(function() {
                                    view.module.start();
                                }, 0);
                            } else {
                                //if it isn't here, we haven't required it in yet, this should automatically start the module
                                require([this.model.get('jsLocation')], function(module) {
                                    //if a marionette module is being called, it will start up automatically
                                    //however if someone has built something else, we are just checking for start and stop
                                    //functions so we can control the module
                                    //it isn't required that a module have a start and stop function, we just wouldn't be able
                                    //to dynamically add and remove that module without refreshing the ui
                                    if(module && module.start) {
                                        module.start();
                                        view.module = module;
                                    }
                                });
                            }
                            if(this.model.get('cssLocation') && this.model.get('cssLocation') !== "") {
                                require(['css!' + this.model.get('cssLocation')]);
                            }
                        }
                    },
                    onClose: function() {
                        //stop the module
                        if(Application.App[this.model.get('name')]) {
                            Application.App[this.model.get('name')].stop();
                        } else if(this.module && this.module.stop) {
                            this.module.stop();
                        }
                        //remove the region where the module is rendered
                        Application.App.removeRegion(this.model.get('id'));
                    }
                })
            }));
        }
    });

    return ModuleView;
});