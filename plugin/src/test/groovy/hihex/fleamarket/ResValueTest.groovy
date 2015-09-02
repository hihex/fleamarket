package hihex.fleamarket

import groovy.transform.CompileStatic
import groovy.xml.DOMBuilder
import hihex.fleamarket.model.ResValue
import hihex.fleamarket.utils.Xml
import org.gradle.api.InvalidUserCodeException
import spock.lang.Specification

import java.util.regex.Pattern

class ResValueTest extends Specification {
    def 'support recursive string replacement'() {
        given:

        final source = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <plurals name="my_name">
                <item quantity="one"><b>bold</b> or <i>italic <u>character</u></i></item>
                <item quantity="other"><strong>strong</strong> and <em>emphasized <ins>letters</ins></em></item>
            </plurals>'''.stripIndent()

        final expected = '''\
            <?xml version="1.0" encoding="UTF-8"?>
            <plurals name="my_name">
                <item quantity="one"><b>BOLD</b> OR <i>ITaLIC <u>CHaRaCTER</u></i></item>
                <item quantity="other"><strong>STRONG</strong> aND <em>EMPHaSIZED <ins>LETTERS</ins></em></item>
            </plurals>'''.stripIndent()

        expect:
        performResValueReplacement(source, ~/[b-z]+/, { it.toUpperCase() }) == expected
    }

    def 'ignores non-strings'() {
        given:
        final source = '<?xml version="1.0" encoding="UTF-8"?>\n<color name="my_color">#abcdef</color>'

        expect:
        performResValueReplacement(source, ~/./, 'x') == source
    }

    def 'replace with capture group'() {
        given:
        final source = '<?xml version="1.0" encoding="UTF-8"?>\n<string>SuperFoo and SuperBar are Super Useful</string>'
        final expected = '<?xml version="1.0" encoding="UTF-8"?>\n<string>ÜberFoo and ÜberBar are Super Useful</string>'

        expect:
        performResValueReplacement(source, ~/Super(\w+)/, 'Über$1') == expected
    }

    def 'fail with invalid replacement type'() {
        given:
        final source = '<?xml version="1.0" encoding="UTF-8"?>\n<string-array><item>1</item></string-array>'

        when:
        performResValueReplacement(source, ~/./, [ab:345])

        then:
        thrown(InvalidUserCodeException)
    }

    @CompileStatic
    private static String performResValueReplacement(final String source,
                                                     final Pattern pattern,
                                                     final Object replacement) {
        def xml = DOMBuilder.newInstance().parseText(source)
        def resValue = new ResValue(null, xml.documentElement)
        resValue.replaceStrings(pattern, replacement)
        def output = new StringWriter()
        Xml.write(xml, output)
        return output.toString()
    }
}
