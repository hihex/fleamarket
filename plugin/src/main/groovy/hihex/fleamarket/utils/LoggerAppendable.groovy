package hihex.fleamarket.utils
import groovy.transform.CompileStatic
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

@CompileStatic
class LoggerAppendable implements Appendable {
    private StringBuilder line
    private final Logger logger
    private final LogLevel level

    LoggerAppendable(final Logger logger, final LogLevel level) {
        this.logger = logger
        this.level = level
    }

    @Override
    Appendable append(final CharSequence csq) throws IOException {
        append(csq, 0, csq.length())
    }

    @Override
    Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
        (start ..< end).each { i -> append(csq[i]) }
        this
    }

    @Override
    Appendable append(char c) throws IOException {
        if (c != '\n') {
            line.append(c)
        } else {
            logger.log(level, line.toString())
            line.delete(0, line.length())
        }
        this
    }
}
