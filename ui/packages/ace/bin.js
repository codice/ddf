#!/usr/bin/env node

const fs = require('fs')
const program = require('commander')

const find = require('find-up')
const cheerio = require('cheerio')

const wrap = (path) => (args = {}) => {
  const cmd = require(path)

  let pkg, pom

  try {
    pkg = require(find.sync('package.json'))
    pom = cheerio.load(
      fs.readFileSync(find.sync('pom.xml'), {encoding: 'utf8'}),
      { xmlMode: true, decodeEntities: false })
  } catch (e) {
    console.error(e)
  }

  cmd({ args, pkg, pom })
}

const pkg = require('./package.json')

program.version(pkg.version)
program.description(pkg.description)

program
  .command('package')
  .description('build a jar')
  .action(wrap('./lib/package'))

program
  .command('set-env [args...]')
  .description('run args with `ACE_BUILD` environment variable set')
  .action(wrap('./lib/set-env'))

program
  .command('install [jar]')
  .description('install a jar into ~/.m2')
  .action(wrap('./lib/install'))

program
  .command('clean')
  .description('remove target directory')
  .option('-w, --workspaces', 'only clean workspaces')
  .action(wrap('./lib/clean'))

program
  .command('test [html]')
  .description('run mocha tests in a headless browser')
  .action(wrap('./lib/test'))

program
  .command('pom')
  .description('verify/fix the root pom')
  .option('-f, --fix', 'sync pom with packages')
  .action(wrap('./lib/pom'))

program
  .command('gen-feature')
  .description('generate a feature file')
  .option('-e, --extend <feature-file>', 'extend an existing feature file')
  .option('-x, --exclude [projects]', 'exclude existing wabs', (val) => val.split(','))
  .action(wrap('./lib/gen-feature'))

program
  .command('bundle')
  .description('bundle webapp')
  .option('-e, --env <env>', 'build environment <development|test|production>')
  .action(wrap('./lib/bundle'))

program
  .command('start')
  .description('start the dev server')
  .option('-a, --auth <auth>', 'auth <username:password> (default: admin:admin)')
  .option('-e, --env <env>', 'build environment <development|test|production>')
  .option('-o, --open', 'open default browser')
  .option('--port <port>', 'dev server port (default: 8080)')
  .option('--host <host>', 'dev server host (default: localhost)')
  .action(wrap('./lib/start'))

program.parse(process.argv)

