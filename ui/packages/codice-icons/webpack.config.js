const path = require('path');

module.exports = {
    entry: [
        './codice.font.config.js',
        './codice.font.css'
    ],
    output: {
        path: path.resolve(__dirname, 'icons'),
        publicPath: '/',
        filename: 'codice.font.js'
    },
    module: {
        rules: [
            {
                test: /\.font\.config\.js/,
                use: [
                    'style-loader',
                    'css-loader',
                    'webfonts-loader'
                ]
            },
            {
                test: /\.css/,
                    use: [
                    'style-loader',
                    'css-loader'
                ]
            }
        ]
    }
};
