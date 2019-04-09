const loaderUtils = require('loader-utils')
const fs = require('fs')
const path = require('path')

module.exports = function(source, map) {
  const rootContext = path.join(this.rootContext, 'src/main/webapp/')
  const options = {
    prefix: 'catalog-ui-search/src/main/webapp/',
    resolve(filePath) {
      const possibleAbsolutePathToJSFile = path.join(
        rootContext,
        `${filePath}.js`
      )
      const possibleAbsolutePathToTSXFile = path.join(
        rootContext,
        `${filePath}.tsx`
      )
      if (fs.existsSync(possibleAbsolutePathToJSFile)) {
        return filePath
      } else if (fs.existsSync(possibleAbsolutePathToTSXFile)) {
        return filePath
      } else {
        return `${this.prefix}${filePath}`
      }
    },
    ...loaderUtils.getOptions(this),
  }
  this.cacheable()
  const routeDefinitions = eval(`${source}`)
  const routesObjectString = Object.keys(routeDefinitions)
    .map(routeName => {
      const { patterns, component, menu } = routeDefinitions[routeName]
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
                    ${loaderUtils.stringifyRequest(
                      this,
                      options.resolve(component)
                    )}
                ], onComponentResolution.bind(this, deferred, {}));
                return deferred;
            },
            menu: {
                getComponent: function() {
                    var deferred = new $.Deferred();
                    require([
                        ${loaderUtils.stringifyRequest(
                          this,
                          options.resolve(
                            menu.component
                              ? menu.component
                              : 'component/navigation-middle/navigation-middle.view'
                          )
                        )}
                    ], onComponentResolution.bind(this, deferred, ${JSON.stringify(
                      menu.component ? {} : menu
                    )}));
                    return deferred;
                }
            }
        }
      `
    })
    .join(',')
  this.callback(
    null,
    [
      `
      var $ = require('jquery')
      var onComponentResolution = function(deferred, options, component) {
        this.component = this.component || (component.default ? component.default : new component(options))
        deferred.resolve(this.component);
      }
      module.exports = { ${routesObjectString} }`,
    ].join(''),
    map
  )
}
