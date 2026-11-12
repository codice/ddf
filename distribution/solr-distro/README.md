#Custom Solr Distribution for DDF

###Introduction
The purpose of this module to create a custom distribution of Solr. The custom distribution is modified 
to support the needs of DDF. The custom distribution adds the JTS spatial library and configuration
to the base distribution. The custom distribution should include every file needed to run Solr in
its own JVM. The custom distribution is packaged as a zip file that can be unzipped and run.

### Downloading the Solr distribution
The base Solr distribution is downloaded by the `download-maven-plugin`. See the
module's `pom.xml` for configuration. The URL combines the root POM properties
`solr.download.url` (the versioned Apache archive) and `solr.version`.
The `download-maven-plugin` is also configured with the SHA-512 for the Solr tarball.
The download operation fails if the file's SHA-512 does not match the configured value. This is done
for protection against false or corrupted Solr distributions.

Changing the version of Solr that is downloaded can be as simple as updating the
`${solr.version}` property and changing the SHA-512 in this module's `pom.xml`.
Verify both against the Apache release before building; changing only the version is insufficient.

The `download-maven-plugin` will cache the Solr distribution tarball in the local Maven
repository. 

### Creating the DDF custom Solr distribution
The `maven-assembly-plugin` uses the maven coordinates of JAR files that need to be 
included with the Solr distribution. The plugin fetches those JARs like any other artifacts 
and copies them into the configured location.

The download plugin first unpacks the base Solr distribution into the module's target directory.
The assembly descriptor copies the upstream files after DDF's overrides because
`maven-assembly-plugin` does NOT overwrite existing files.

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
maven-assembly-plugin descriptor file. The descriptor file is `assembly.xml` in this module.
