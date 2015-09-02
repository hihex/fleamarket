package hihex.fleamarket.model

import groovy.transform.CompileStatic

@CompileStatic
interface ChannelFilename {
    CharSequence call(final Channel channel)
}
