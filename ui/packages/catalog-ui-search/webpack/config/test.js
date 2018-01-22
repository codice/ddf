var webpack = require('webpack');
var merge = require('webpack-merge');

var ci = require('./ci');

module.exports = merge.smart(ci, {
    entry: [
        'stack-source-map/register'
    ],
    devServer: {
        progress: true,
        historyApiFallback: true,
        inline: true,
        hot: true,
        port: 8181
    },
    plugins: [
        new webpack.HotModuleReplacementPlugin()
    ]
});
