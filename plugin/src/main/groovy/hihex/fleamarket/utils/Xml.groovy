package hihex.fleamarket.utils

import groovy.transform.CompileStatic
import groovy.xml.DOMBuilder
import org.w3c.dom.Document
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS

@CompileStatic
class Xml {
    private Xml() {}

    static Document read(File input) {
        input.withReader { DOMBuilder.parse(it) }
    }

    static void write(Document document, Writer output) {
        // Use DOM3 Save/Load instead of XmlUtil.serialize (based on XLST) to keep the formatting.

        final reg = DOMImplementationRegistry.newInstance()
        final impl = (DOMImplementationLS) reg.getDOMImplementation('LS')

        final lsOutput = impl.createLSOutput()
        lsOutput.characterStream = output
        final serializer = impl.createLSSerializer()
        serializer.write document, lsOutput
    }
}
