package org.codice.ddf.catalog.scripting

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.apache.felix.fileinstall.ArtifactInstaller
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

@SuppressFBWarnings
public class ScriptRunner implements ArtifactInstaller {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptRunner.class)


    public ScriptRunner() {

    }

    @Override
    public void install(File file) throws Exception {
        LOGGER.trace("[Install] Executing script! file: [$file.absolutePath]")
        AtomicBoolean ended = new AtomicBoolean(false)

        Thread thread = new Thread({ ->
            def scriptName = "script:${file.name}"
            def scriptLogger = LoggerFactory.getLogger(scriptName)
            Binding binding = new Binding(LOGGER: scriptLogger,
                    bundleContext: getBundleContext(),
                    ended: ended)
            GroovyShell shell = new GroovyShell(this.class.classLoader, binding)

            def obj = shell.evaluate(file)
            LOGGER.trace("Shell evaluated to: $obj")

        })

        thread.daemon = true
        thread.start()
        // TODO (RCZ) - Should we keep track of threads? What can/should we do with them?

    }

    @Override
    public void update(File file) throws Exception {
        LOGGER.trace("[Update] Executing script! file: [$file.absolutePath]")
        AtomicBoolean ended = new AtomicBoolean(false)

        Thread thread = new Thread({ ->
            def scriptName = "script:${file.name}"
            def scriptLogger = LoggerFactory.getLogger(scriptName)
            Binding binding = new Binding(LOGGER: scriptLogger,
                    bundleContext: getBundleContext(),
                    ended: ended)
            GroovyShell shell = new GroovyShell(this.class.classLoader, binding)

            def obj = shell.evaluate(file)
            LOGGER.trace("Shell evaluated to: $obj")

        })

        thread.daemon = true
        thread.start()
        // TODO (RCZ) - Should we keep track of threads? What can/should we do with them?
    }

    @Override
    public void uninstall(File file) throws Exception {
        LOGGER.trace("uninstall was called! file: [$file.absolutePath]")
    }

    @Override
    public boolean canHandle(File file) {
        LOGGER.trace("CanHandle was called! file: [$file.absolutePath]")
        return file.name
                .endsWith(".groovy")
    }

    private static BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(ScriptRunner.class)?.getBundleContext()
    }

}