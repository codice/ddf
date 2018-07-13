# eslint-plugin-codice-linter

ESLint rules for Codice projects

## Installation

You'll first need to install [ESLint](http://eslint.org):

```
$ npm i eslint --save-dev
```

Next, install `eslint-plugin-codice-linter`:

```
$ npm install eslint-plugin-codice-linter --save-dev
```

**Note:** If you installed ESLint globally (using the `-g` flag) then you must also install `eslint-plugin-codice-linter` globally.

## Usage

Add `codice-linter` to the plugins section of your `.eslintrc` configuration file. You can omit the `eslint-plugin-` prefix:

```json
{
    "plugins": [
        "codice-linter"
    ]
}
```


Then configure the rules you want to use under the rules section.

```json
{
    "rules": {
        "codice-linter/rule-name": 2
    }
}
```

## Supported Rules

* Fill in provided rules here





