module.exports = function (source, map) {
    this.cacheable()
    const routeDefinitions = eval(`${source}`);
    routeDefinitions.notFound = {
        patterns: ['*path'],
        component: 'component/notfound/notfound.view',
        menu: {
          text: 'Page Not Found',
          classes: 'is-bold'
        }
    };
    const routesObjectString = Object.keys(routeDefinitions).map((routeName) => {
      const { patterns, component, menu } = routeDefinitions[routeName];
      return `
          ${routeName}: {
            patterns: ${JSON.stringify(patterns)},
            preload: function() {
                this.getComponent();
                this.menu.getComponent();
            },
            getComponent: function() {
                var deferred = new $.Deferred();
                require([
                    ${JSON.stringify(component)}
                ], onComponentResolution.bind(this, deferred, {}));
                return deferred;
            },
            menu: {
                getComponent: function() {
                    var deferred = new $.Deferred();
                    require([
                        ${JSON.stringify(menu.component ? menu.component : 'component/navigation-middle/navigation-middle.view')}
                    ], onComponentResolution.bind(this, deferred, ${JSON.stringify(menu.component ? {} : menu)}));
                    return deferred;
                }
            }
        }
      `;
    }).join(',');
    this.callback(null, [
      `
      const $ = require('jquery')
      const onComponentResolution = function(deferred, options, component) {
        this.component = this.component || new component(options);
        deferred.resolve(this.component);
      }
      module.exports = { ${routesObjectString} }`
    ].join(''), map)
  }