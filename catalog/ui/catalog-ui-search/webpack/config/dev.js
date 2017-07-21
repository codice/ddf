var path = require('path');
var webpack = require('webpack');
var merge = require('webpack-merge');

var base = require('./base');

var resolve = function (place) {
    return path.resolve(__dirname, '../../', place)
};

var handleProxyRes = function (proxyRes, req, res) {
    // remove so we can still login in through http
    delete proxyRes.headers['x-xss-protection'];
    var cookie = proxyRes.headers['set-cookie'];
    if (cookie !== undefined) {
        // force the cookie to be insecure since the proxy is over http
        proxyRes.headers['set-cookie'] = cookie[0].replace(new RegExp(/;\w?Secure/), '')
    }
};

var proxyConfig = function (target) {
    return {
        target: target,
        secure: false,
        changeOrigin: true,
        onProxyRes: handleProxyRes
    }
};

module.exports = merge.smart(base, {
    devServer: {
        progress: true,
        historyApiFallback: true,
        inline: true,
        hot: true,
        contentBase: [resolve('./node_modules/cesium/Build/'), resolve('/src/main/webapp/')],
        proxy: {
            '/search/catalog/**': proxyConfig('https://localhost:8993'),
            '/services/**': proxyConfig('https://localhost:8993'),
            '/styles/**': proxyConfig('https://localhost:8993/search/catalog')
        }
    },
    entry: [
        'stack-source-map/register',
        'console-polyfill'
    ],
    module: {
        loaders: [{
            test: /\.jsx$/,
            loaders: ['react-hot']
        }]
    },
    plugins: [
        new webpack.NoErrorsPlugin(),
        new webpack.NamedModulesPlugin(),
        new webpack.HotModuleReplacementPlugin()
    ]
});