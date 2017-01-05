var webpack = require('webpack');
var merge = require('webpack-merge');

var ci = require('./ci');

module.exports = merge.smart(ci, {
    entry: [
        'webpack-hot-middleware/client?path=/__webpack_hmr_test',
        'webpack/hot/only-dev-server',
        'stack-source-map/register'
    ],
    plugins: [
        new webpack.HotModuleReplacementPlugin()
    ]
});
