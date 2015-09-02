package hihex.fleamarket.dsl
import groovy.transform.CompileStatic
import hihex.fleamarket.model.Channel
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException

@CompileStatic
class Channels {
    Channel defaultChannelTemplate = null

    final HashMap<String, Channel> channels = [:]

    /**
     * Sets the default configuration of all channels.
     */
    void defaultConfig(final Action<Config> configAction) {
        if (defaultChannelTemplate) {
            throw new InvalidUserDataException('Do not call defaultConfig{} twice.')
        }

        final dsl = new Config(channel: new Channel())
        configAction.execute(dsl)
        defaultChannelTemplate = dsl.channel
    }

    /**
     * Creates a new channel using the given name.
     */
    void create(final String name, final Action<Config> configAction) {
        if (!defaultChannelTemplate) {
            throw new InvalidUserDataException('defaultConfig{} must be called before create()')
        }
        if (channels.containsKey(name)) {
            throw new InvalidUserDataException("Cannot create() two channels with the same name: ${name}")
        }

        final dsl = new Config(channel: new Channel(name, defaultChannelTemplate))
        configAction.execute(dsl)
        channels[name] = dsl.channel
    }

    /**
     * Creates many channels, all reusing the default configuration.
     */
    void create(final String... names) {
        if (!defaultChannelTemplate) {
            throw new InvalidUserDataException('defaultConfig{} must be called before create()')
        }
        names.each { String name ->
            if (channels.containsKey(name)) {
                throw new InvalidUserDataException("Cannot create() two channels with the same name: ${name}")
            }
            channels[name] = defaultChannelTemplate.createShallowCopy(name)
        }
    }
}
