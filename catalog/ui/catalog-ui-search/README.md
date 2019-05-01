## Internal Documentation for the Intrigue Backend

This is internal documentation. Some of the data structures documented here are either subject to 
change or are not publicly exposed.

### Maintaining the blueprint files

Catalog UI Search's blueprint definitions have been broken up into four files, each of which is further
subdivided by exposed functionality. Any component that has an ID can be referenced by that ID across
the different XML files.

The files exist to organize components with respect to the coupling between Catalog UI Search and DDF:
1. `endpoints.xml` is where exposed networking services live that the UI itself will consume.
1. `provides.xml` is for publishing OSGi services **to** DDF to support the container itself and other distributions.
1. `consumes.xml` is for retrieving OSGi services **from** DDF to support the UI.
1. `blueprint.xml` is for beans, objects, and implementation details supporting the above.

Before adding services to the `provides.xml` file, consider alternative approaches to supply DDF with the
services it needs.

Before adding more code to `EndpointUtil`, consider a separate class or splitting up existing logic into
a separate class that does one thing, and one thing really well.

Within the files, sections exist to organize components with respect to the functionality they enable:
1. Simple apps that just proxy existing interfaces, such as catalog services or auth services.
1. Major apps, such as querying, workspaces, search forms, and other logical groups of REST services.
1. Plugins that define policies that Intrigue relies on.

New, refined, or refactored functionality should update the sections accordingly.

### Spark Applications Overview

The Intrigue backend is comprised of multiple Spark applications. 
They represent logical groups of functions, exposed over REST, and are as follows: 
* Configuration Application - provides access to Intrigue's configuration as set by the system administrator
* Forms Application - provides access to custom search forms so users only work with fields they care about
* Metacard Application - provides various CRUD operations on metacards and their respective data such as
types, validation details, history, associations, subscriptions, and annotations; primarily designed to work 
with non-resource metacards such as workspaces
* Query Application - provides support for querying the catalog with CQL
* Feedback Application - allows users to submit feedback about queries

### CXF Endpoints

Some functionality is defined using JAX-RS instead of Spark. 
* Metacard Edit Endpoint - allows the updating of metacards

### Intrigue Data Expectations

#### Filters

Intrigue currently represents a filter using the tokens of CQL in a JSON object hierarchy. The
key fields on each terminal node in the hierarchy are: 
* `type` - name of the operation to perform as a upper-case string
* `property` - name of the property to operate on
* `value` - value of the property to operate on; it can be:
    * Primitive type (int, double, boolean, or string - float not supported)
    * Temporal data (a DateTime object) specified as a string in ISO 8601 format (GML time range not supported)
    * Spatial data as a WKT string

For nodes that are logical operations and contain other nodes, they forgo the `property` and `value`
fields in favor of a `filters` field, which is a list of nodes.

Examples of this data structure are included for reference. 

##### Bounding-Box with Temporal Range
```
{
  "type": "AND",
  "filters": [
    {
      "type": "OR",
      "filters": [
        {
          "type": "BEFORE",
          "property": "\"created\"",
          "value": "2018-02-17T23:24:15.701Z"
        },
        {
          "type": "BEFORE",
          "property": "\"modified\"",
          "value": "2018-02-17T23:24:15.701Z"
        },
        {
          "type": "BEFORE",
          "property": "\"effective\"",
          "value": "2018-02-17T23:24:15.701Z"
        },
        {
          "type": "BEFORE",
          "property": "\"metacard.created\"",
          "value": "2018-02-17T23:24:15.701Z"
        },
        {
          "type": "BEFORE",
          "property": "\"metacard.modified\"",
          "value": "2018-02-17T23:24:15.701Z"
        }
      ]
    },
    {
      "type": "OR",
      "filters": [
        {
          "type": "AFTER",
          "property": "\"created\"",
          "value": "2018-02-08T23:24:12.709Z"
        },
        {
          "type": "AFTER",
          "property": "\"modified\"",
          "value": "2018-02-08T23:24:12.709Z"
        },
        {
          "type": "AFTER",
          "property": "\"effective\"",
          "value": "2018-02-08T23:24:12.709Z"
        },
        {
          "type": "AFTER",
          "property": "\"metacard.created\"",
          "value": "2018-02-08T23:24:12.709Z"
        },
        {
          "type": "AFTER",
          "property": "\"metacard.modified\"",
          "value": "2018-02-08T23:24:12.709Z"
        }
      ]
    },
    {
      "type": "INTERSECTS",
      "property": "anyGeo",
      "value": "POLYGON((-105.91230761661996 31.9334620191689,-105.91230761661996 38.0176324006135,-95.51632701146173 38.0176324006135,-95.51632701146173 31.9334620191689,-105.91230761661996 31.9334620191689))"
    }
  ]
}
```

##### Lat-Lon
```
{
  "type": "AND",
  "filters": [
    {
      "type": "DWITHIN",
      "property": "anyGeo",
      "value": "POINT(-95.65792005395343 36.13061496061506)",
      "distance": 0.000001
    }
  ]
}
```

##### Line
```
{
  "type": "AND",
  "filters": [
    {
      "type": "DWITHIN",
      "property": "anyGeo",
      "value": "LINESTRING(-117.13230471468687 44.14686910368135,-115.76807564454025 39.98312582991066,-113.48157584359824 37.912516776342436,-111.32735611138365 40.816915235679254,-114.55723858194943 42.52119725321046,-117.29871236281292 44.078654446842506)",
      "distance": 1
    }
  ]
}
```

##### Point-Radius
```
{
  "type": "AND",
  "filters": [
    {
      "type": "DWITHIN",
      "property": "anyGeo",
      "value": "POINT(-93.25696492774843 43.14514690567744)",
      "distance": 0.000001
    }
  ]
}
```

##### USNG with Type Constraints
```
{
  "type": "AND",
  "filters": [
    {
      "type": "INTERSECTS",
      "property": "anyGeo",
      "value": "POLYGON((-104.41136226817798 38.19888480954465,-104.41136226817798 41.33692635632009,-103.42550211052009 41.33692635632009,-103.42550211052009 38.19888480954465,-104.41136226817798 38.19888480954465))"
    },
    {
      "type": "OR",
      "filters": [
        {
          "type": "ILIKE",
          "property": "\"metadata-content-type\"",
          "value": "Moving Image"
        },
        {
          "type": "ILIKE",
          "property": "\"datatype\"",
          "value": "Moving Image"
        }
      ]
    }
  ]
}
```

##### UTM
```
{
  "type": "AND",
  "filters": [
    {
      "type": "INTERSECTS",
      "property": "anyGeo",
      "value": "POLYGON((-171.11428227015105 41.03150897992597,-171.11428227015105 42.69309383179558,-112.89261713634725 42.69309383179558,-112.89261713634725 41.03150897992597,-171.11428227015105 41.03150897992597))"
    }
  ]
}
```

### Application Details

#### Forms Application

Search forms are custom query interfaces created by the system or user that only show query controls 
that the user is interested in. They effectively hide the complexity of very large query filters while 
allowing a small set of values to be plugged into predetermined locations within the filter tree. Search
forms also support the notion of "detail level" which limits what attributes are rendered when viewing 
results. Search forms are powered by filter templates and detail levels are powered by attribute groups. 

##### Filter XML 2.0

Filter templates and attribute groups are persisted in the catalog as metacards. Attribute groups are
simple: they only specify a set of attribute descriptor names. Filter templates store the templated 
filter data as [Filter XML 2.0](http://schemas.opengis.net/filter/2.0/) and leverage functions to
act as a placeholder when user input is needed. 

Comprehensive examples can be found in `catalog-ui-search/src/test/resources/forms/filter2` for reference. 
They are arranged by classification according to the bindings provided by `mvn:org.jvnet.ogc/filter-v_2_0/2.6.1`
which scrutinize by the number of parameters needed to construct a given JAXB type and the possible values
that can exist on a JAXB type (i.e. serializable literals vs more XML). 

Some operation types have incomplete examples. The instances of these types are listed here for reference. 

##### Spatial Binary Ops

Spatial Ops are supported by templates **only** when defined using functions for passthrough to user
input. GML cannot yet be used to specify constant location data on a template that's hidden away from
the user. 

Currently only `Intersects` and `Disjoint` are supported by Intrigue (from Filter 1.1):

`Intersects` - Tests whether two geometries intersect (example provided)
`Disjoint` - Tests whether two geometries are disjoint (example provided)

Unsupported: 

`Contains` - Tests whether a geometry contains another one
`Within` - Tests whether a geometry is within another one
`Touches` - Tests whether two geometries touch
`Crosses` - Tests whether two geometries cross
`Overlaps` - Tests whether two geometries overlap
`Equals` - Tests whether two geometries are topologically equal

##### Temporal Ops

Temporal Ops are **not yet supported** by templates for any use case. 

Currently only `After` and `Before` are supported by Intrigue (from Filter 1.1):

 `After` - tests whether a time instant occurs _after_ another
 `Before` - tests whether a time instant occurs _before_ another
 
 Unsupported: 
 
 ```
 TEquals (example provided)
 Begins
 BegunBy
 TContains
 During
 EndedBy
 Ends
 Meets
 MetBy
 TOverlaps
 OverlapedBy
 AnyInteracts
 ```
 
 Also note that Filter 2.0 adds complex temporal value support via GML, as shown in `temporal-ops/gml`
 vs the simple style shown in `temporal-ops/literal`. 