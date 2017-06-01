var webpack = require('webpack');
var merge = require('webpack-merge');
var ExtractTextPlugin = require("extract-text-webpack-plugin");

module.exports = merge.smart(require('./base'), {
    module: {
        loaders: [ 
            {
                test: /\.(css|less)$/,
                loader: ExtractTextPlugin.extract("style", "css?url=false&sourceMap!less?sourceMap")
            }
        ]
    },
    plugins: [
        new webpack.DefinePlugin({
            'process.env':{
                'NODE_ENV': JSON.stringify('production')
            }
        }),
        new webpack.optimize.UglifyJsPlugin({
            compress: {
                warnings: false,
                drop_console: true
            },
            output: {
                comments: false
            }
        }),
        new ExtractTextPlugin("css/styles.[contenthash].css")
    ]
});
