package ddf.catalog.cache.impl

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification

import java.nio.file.Path

class FileSystemPersistenceProviderSpec extends Specification {

    private Path cachePath

    private FileSystemPersistenceProvider provider

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    def setup() {
        temporaryFolder.create()
        cachePath = temporaryFolder.newFolder("cache").toPath()
        provider = new FileSystemPersistenceProvider("cache", cachePath.toString())
    }

    def "test storing an item"() {
        setup:
        File expectedPath = getCachedFilePath("foo.ser")

        when:
        provider.store("foo", ["foo", "bar"])

        then:
        expectedPath.exists()
        expectedPath.text.contains("foo")
        expectedPath.text.contains("bar")
    }

    @Ignore("DDF-4119")
    def "test storing an item when storage is not writable"() {
        setup:
        File expectedPath = getCachedFilePath("foo.ser")
        cachePath.toFile().setWritable(false)

        when:
        provider.store("foo", ["foo", "bar"])

        then:
        notThrown IOException
        !expectedPath.exists()
    }

    def "test storing multiple items"() {
        setup:
        File item1 = getCachedFilePath("foo.ser")
        File item2 = getCachedFilePath("bar.ser")

        when:
        provider.storeAll([foo: "foo", bar: "bar"])

        then:
        item1.exists()
        item1.text.contains("foo")
        item2.exists()
        item2.text.contains("bar")
    }

    def "test deleting an item"() {
        setup:
        File expectedPath = getCachedFilePath("foo.bar")
        provider.store("foo", ["foo"])
        expectedPath.exists()

        when:
        provider.delete("foo")

        then:
        !expectedPath.exists()
    }

    def "test deleting all files"() {
        setup:
        File item1 = getCachedFilePath("foo.ser")
        File item2 = getCachedFilePath("bar.ser")
        item1.createNewFile()
        item2.createNewFile()

        when:
        provider.deleteAll(["foo", "bar"])

        then:
        !item1.exists()
        !item2.exists()
    }

    def "test loading all files"() {
        setup:
        provider.storeAll([foo: "foo", bar: "bar"])
        Map loaded
        String loadedItem1
        String loadedItem2

        when:
        loaded = provider.loadAll(["foo", "bar"])
        loadedItem1 = loaded.get("foo")
        loadedItem2 = loaded.get("bar")

        then:
        loadedItem1.contains("foo")
        loadedItem2.contains("bar")
    }

    def "test loading all files when one does not exist"() {
        setup:
        provider.storeAll([foo: "foo"])
        Map loaded
        String loadedItem1

        when:
        loaded = provider.loadAll(["foo", "bar"])
        loadedItem1 = loaded.get("foo")

        then:
        loadedItem1.contains("foo")
        loaded.get("bar") == null
    }

    @Ignore("DDF-4119")
    def "test loading file when file is not readable"() {
        setup:
        File file = getCachedFilePath("foo.ser")
        provider.store("foo", ["bar"])
        file.setReadable(false)
        Map loaded

        when:
        loaded = provider.loadAll(["foo"])

        then:
        loaded.get("foo") == null
    }

    def "test loading all keys"() {
        setup:
        File item1 = getCachedFilePath("foo.ser")
        File item2 = getCachedFilePath("bar.ser")
        item1.createNewFile()
        item2.createNewFile()
        Set keys

        when:
        keys = provider.loadAllKeys()

        then:
        keys.contains("foo")
        keys.contains("bar")
    }

    def "test loading all keys when files not belonging to the cache are present"() {
        setup:
        getCachedFilePath("foo.ser").createNewFile()
        getCachedFilePath("bar.ser").createNewFile()
        getCachedFilePath("fake.notcached").createNewFile()
        Set keys

        when:
        keys = provider.loadAllKeys()

        then:
        keys.contains("foo")
        keys.contains("bar")
        !keys.contains("fake")
    }

    def "test clearing files"() {
        setup:
        File item1 = getCachedFilePath("foo.ser")
        File item2 = getCachedFilePath("bar.ser")
        item1.createNewFile()
        item2.createNewFile()

        when:
        provider.clear()

        then:
        !item1.exists()
        !item2.exists()
    }

    def "test clearing files when other non-cache related files are present"() {
        setup:
        File item1 = getCachedFilePath("foo.ser")
        File fake = getCachedFilePath("fake.notcached")
        item1.createNewFile()
        fake.createNewFile()

        when:
        provider.clear()

        then:
        !item1.exists()
        fake.exists()
    }

    private File getCachedFilePath(String name) {
        return cachePath.resolve(name).toFile()
    }
}
