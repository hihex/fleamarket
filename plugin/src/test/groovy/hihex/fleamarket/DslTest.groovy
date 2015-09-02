package hihex.fleamarket
import hihex.fleamarket.dsl.Channels
import org.gradle.api.InvalidUserDataException
import spock.lang.Specification

class DslTest extends Specification {
    def channels = new Channels()

    def 'cannot call defaultConfig{} twice'() {
        when:
        channels.with {
            defaultConfig {}
            defaultConfig {}
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Do not call defaultConfig{} twice.'
    }

    def 'cannot call defaultConfig{} twice (with create() in between)'() {
        when:
        channels.with {
            defaultConfig {}
            create('A') {}
            defaultConfig {}
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Do not call defaultConfig{} twice.'
    }

    def 'cannot call create() before defaultConfig{} (parametric version)'() {
        when:
        channels.with {
            create('A') {}
            defaultConfig {}
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'defaultConfig{} must be called before create()'
    }

    def 'cannot call create() before defaultConfig{} (variadic version)'() {
        when:
        channels.with {
            create 'A', 'B', 'C'
            defaultConfig {}
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'defaultConfig{} must be called before create()'
    }

    def 'cannot call create() twice with the same name (both parametric)'() {
        when:
        channels.with {
            defaultConfig {}
            create('A') {}
            create('B') {}
            create('A') {}
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Cannot create() two channels with the same name: A'
    }

    def 'cannot call create() twice with the same name (parametric and variadic)'() {
        when:
        channels.with {
            defaultConfig {}
            create('A') {}
            create 'B', 'C', 'A', '0'
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Cannot create() two channels with the same name: A'
    }

    def 'cannot call create() twice with the same name (both variadic)'() {
        when:
        channels.with {
            defaultConfig {}
            create 'X', 'Y', 'Z'
            create 'Y', 'M', 'C', 'A'
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Cannot create() two channels with the same name: Y'
    }

    def 'create(...) should contain unique list of names'() {
        when:
        channels.with {
            defaultConfig {}
            create 'A', 'B', 'C', 'D', 'A'
        }

        then:
        InvalidUserDataException e = thrown()
        e.message == 'Cannot create() two channels with the same name: A'
    }

    def 'normal configuration is successful'() {
        when:
        channels.with {
            defaultConfig {}
            create('A') {}
            create('B') {}
            create 'C', 'D', 'E'
        }

        then:
        notThrown(InvalidUserDataException)
    }

}
