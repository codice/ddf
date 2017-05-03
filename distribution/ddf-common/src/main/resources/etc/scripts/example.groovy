// Available Variables:

// LOGGER (an org.slf4j.Logger)
//  - log levels for this SCRIPT can be set on karaf console via `log:set LEVEL script:SCRIPTNAME`

// BundleContext [TEMPORARY] (an  org.osgi.framework.BundleContext)

// ended [EXPERIMENTAL] (An AtomicBoolean. Represents if script should exit)
// - Thinking of this being used as a way for scripts to check if they should exit prematurely in long running/repeated operations.

LOGGER.trace("Scripting support started.")