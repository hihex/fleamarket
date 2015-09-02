package hihex.fleamarket.model
import groovy.transform.CompileStatic

@CompileStatic
interface ChannelAction<T> {
    void execute(final T object, final Channel channel)
}
