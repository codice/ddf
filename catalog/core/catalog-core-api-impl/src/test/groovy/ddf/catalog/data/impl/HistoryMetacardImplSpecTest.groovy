package ddf.catalog.data.impl

import ddf.catalog.data.Metacard
import ddf.security.SubjectUtils
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import spock.lang.Specification

import java.time.Instant

import static ddf.catalog.data.impl.HistoryMetacardImpl.Action

class HistoryMetacardImplSpecTest extends Specification {

    void setup() {
        ThreadContext.bind(Mock(Subject))

    }

    void cleanup() {
        ThreadContext.unbindSubject()
    }

    def "History Metacard Creation"() {
        setup:
        def meta = defaultMetacard()
        Action action = Action.CREATED
        Instant start = Instant.now()

        when:
        HistoryMetacardImpl history = new HistoryMetacardImpl(meta.metacard, action, SecurityUtils.subject)

        then: $/All the original attributes should be there except for:
                    - Old tags should be stored in `tagsHistory
                    - new tag should be `history`
                    - Old id should be in `idHistory` /$
        !history.id.equals(meta.id)
        history.idHistory.equals(meta.id)
        history.tags.containsAll([HistoryMetacardImpl.HISTORY_TAG])
        !history.tags.any { meta.tags.contains(it) }
        history.metadata.equals(meta.metadata)
        history.title.equals(meta.title)
        history.action.equals(action)
        history.tagsHistory.containsAll(meta.tags)

        Instant finish = Instant.now()
        Instant versionMoment = history.versioned.toInstant()
        versionMoment.isAfter(start)
        versionMoment.isBefore(finish)
    }

    def "History Metacard back to Basic Metacard"() {
        setup:
        def meta = defaultMetacard()
        Action action = Action.CREATED
        HistoryMetacardImpl history = new HistoryMetacardImpl(meta.metacard, action, SecurityUtils.subject)

        when:
        Metacard metacard = history.toBasicMetacard()

        then:
        metacard != null
        history.idHistory.equals(metacard.id)
        metacard.tags.containsAll(meta.tags)
        !metacard.tags.contains(HistoryMetacardImpl.HISTORY_TAG)
    }

    def defaultMetacard() {
        def res = [:]
        res.id = "OriginalMetacardId"
        res.metadata = "<title>blablabla my metadata</title>"
        res.title = "Title"
        res.tags = [Metacard.DEFAULT_TAG]

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        metacard.id = res.id
        metacard.tags = res.tags
        metacard.metadata = res.metadata
        metacard.title = res.title
        res.metacard = metacard
        return res
    }
}