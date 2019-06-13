module.exports = {
    "extends": ["eslint:recommended", "plugin:react/recommended"],
    rules: {
        // TODO rules we don't already follow have been turned off. 
        //  this is a reminder to some day fix all the violations and turn the rules on

        // eslint:recommended
        "no-extra-semi" : "off",
        "no-undef" : "off",
        "no-unused-vars" : "off",
        "no-redeclare" : "off",
        "no-inner-declarations" : "off",
        "no-case-declarations" : "off",
        "no-empty" : "off",
        "no-dupe-keys" : "off",
        "no-useless-escape" : "off",
        "no-unreachable" : "off",
        "no-fallthrough" : "off",

        // plugin:react/recommended
        "react/prop-types" : "off",
        "react/display-name" : "off",
        "react/no-unescaped-entities" : "off",
        "react/jsx-key" : "off",
        "react/no-render-return-value" : "off",
        "react/no-deprecated" : "off",
        "react/no-find-dom-node" : "off",
    }
};
