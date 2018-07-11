#Custom Solr Distribution for DDF

###Introduction
The purpose of this module to create a custom distribution of Solr. The custom distribution is modified 
to support the needs of DDF. The custom distribution adds two JAR libraries and configuration
to the base distribution. The custom distribution should include every file needed to run Solr in
its own JVM. The custom distribution is packaged as a zip file that can be unzipped and run.

### Downloading the Solr distribution
The base Solr distribution is downloaded by the `download-maven-plugin`. See the
modules's POM.xml file for configuration. The download URL is a literal string,
except for the maven property `${solr.version)`. The `download-maven-plugin`
is also configured with the sha1 hash for the Solr distribution file. The download operation
should fail if the the file's sha1 does not match the configured value. This is done 
for protection against false or corrupted Solr distributions.

Changing the version of Solr that is downloaded can be as simple as updating the
`${solr.version}` property and changing the sha1 hash in the `POM.xml` file. However, 
the literal URL might need to be changed as well. 
For example, if `archive.apache.org` no longer hosts the zip file.

The `download-maven-plugin` will cache the Solr distribution zip file in the local maven 
repository. 

### Creating the DDF custom Solr distribution
The `maven-assembly-plugin` uses the maven coordinates of JAR files that need to be 
included with the Solr distribution. The plugin fetches those JARs like any other artifacts 
and copies them into the configured location.

Finally the `maven-assembly-plugin` unzips the base Solr distribution into the 
module's target directory. This step should be last because 
`maven-assembly-plugin` does NOT overwrite files.

Finally, the maven-assembly-plugin creates a type _zip_ artifact and assigns it the 
classifier `assembly`. This artifact is installed into the local repository where the 
integration tests can access it. The classifier is arbitrary, 
but `maven-assembly-plugin` fails if a classifier is not provided.

The maven-assembly-plugin will install the custom Solr distribution into the local repository as 
soon as the distribution is built.

**The custom Solr distribution is used by the integration tests. Therefore, 
the custom Solr distribution
must be built before the integration tests attempt to unpack and run
the custom Solr distribution.** Otherwise, the integration tests may fail. Or the integration
tests may use an older version of the custom Solr distribution from the local (or a remote)
maven repository.

### The maven-assembly-plugin descriptor file
The maven-assembly-plugin's instructions are not in the `POM.xml` file. They are in a 
maven-assembly-plugin descriptor file. The descriptor file is `solr-distro/assemblyl.xml`.
