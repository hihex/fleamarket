package hihex.fleamarket.model

import groovy.transform.CompileStatic
import org.gradle.api.InvalidUserCodeException

@CompileStatic
final class ChannelActions {
    private ChannelActions() {}

    static <T> ChannelAction<T> fromClosure(final Closure closure) {
        switch (closure.maximumNumberOfParameters) {
            case 0:
                { T x, c ->
                    closure.delegate = x
                    closure()
                } as ChannelAction<T>
                break
            case 1:
                { T x, c ->
                    closure.delegate = x
                    closure(c)
                } as ChannelAction<T>
                break
            case 2:
                closure as ChannelAction<T>
                break
            default:
                throw new InvalidUserCodeException('Expected less than 2 arguments for a ChannelAction')
        }
    }
}
