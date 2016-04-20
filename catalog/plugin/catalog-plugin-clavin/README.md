
## Clavin Plugin

Clavin needs extra memory. Before ddf startup, edit ../bin/setenv and change memory to 6 gigs:

```
export JAVA_MAX_MEM=6G
```

Howto enable clavin plugin:

```
Admin UI, DDF Catalog App, Features Tab, catalog-plugin-clavin, install
```

After intalling the clavin plugin, the create clavin index command is available:

```
ddf@local>help | grep -i clavin
geocoding:createClavinIndex       createClavinIndex
```

Must create index from local countries file:

```
ddf@local>geocoding:createClavinIndex $HOME/downloads/allCountries.txt
```

Howto see clavin plugin debug statements in ddf.log:

```
ddf@local>log:set DEBUG org.codice.ddf.catalog.content.plugin.clavin
```

