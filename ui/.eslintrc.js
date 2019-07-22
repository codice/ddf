module.exports = {
    "extends": ["eslint:recommended", "plugin:react/recommended"],
    rules: {
        /* eslint:recommended */

        "no-case-declarations" : "off",
        "no-fallthrough" : "off",

        // don't allow unused variables but do allow unused function arguments
        // libraries like mocha and express change behaviour based on the
        // number of arguments (unused or not) in the function signature
        "no-unused-vars" : ["error", { "args": "none" }],

        // TODO investigate whether the below rules should be enabled
        /* plugin:react/recommended */
        "react/prop-types" : "off",
        "react/display-name" : "off",
        "react/no-unescaped-entities" : "off",
        "react/jsx-key" : "off",
        "react/no-render-return-value" : "off",
        "react/no-deprecated" : "off",
        "react/no-find-dom-node" : "off",
    },
    env: {es6: true}
};
