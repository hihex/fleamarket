package hihex.fleamarket

import hihex.fleamarket.dsl.Channels
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.w3c.dom.Document

final class FleaMarketPlugin implements Plugin<Project> {
    @Override
    void apply(final Project project) {
        // FIXME Figure out the extension methods do not work.
        Document.metaClass.setAnalyticsChannel = { c -> ExtensionModule.setAnalyticsChannel(delegate, c) }
        Document.metaClass.deleteTagsWithName = { t, n -> ExtensionModule.deleteTagsWithName(delegate, t, n) }
        Document.metaClass.addUsesPermission = { n -> ExtensionModule.addUsesPermission(delegate, n) }

        project.with {
            extensions.create('channels', Channels)
            task('listChannels', type: ListChannelsTask)
            afterEvaluate this.&createAssembleTasks
        }
    }

    private static void createAssembleTasks(final Project project) {
        project.channels.channels.values().each { channel ->
            project.task("assemble${channel.name.capitalize()}", type: AssembleChannelTask) {
                it.initialize(channel)
            }
        }
    }
}
