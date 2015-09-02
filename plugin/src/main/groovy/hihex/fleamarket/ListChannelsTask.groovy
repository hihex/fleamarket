package hihex.fleamarket

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ListChannelsTask extends DefaultTask {
    @TaskAction
    void listChannels() {
        project.channels.channels.keySet().each { logger.quiet(it) }
    }
}
