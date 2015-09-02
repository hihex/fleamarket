package hihex.fleamarket.model

import groovy.transform.CompileStatic
import groovy.xml.dom.DOMCategory
import org.gradle.api.InvalidUserCodeException
import org.w3c.dom.Element
import org.w3c.dom.Text

import javax.xml.xpath.XPathConstants
import java.util.regex.Pattern

@CompileStatic
class ResValue {
    ResValue(final String qualifier, final Element element) {
        this.qualifier = qualifier
        this.element = element
    }

    /**
     * The qualifier, if this value is part of an alternative resource. This field is null for the unqualified version.
     * Example values are "hdpi", "en-rUS", "fr-sw720dp-port-xxhdpi-v7" etc.
     *
     * @see <a href="http://developer.android.com/guide/topics/resources/providing-resources.html#table2">Android's Configuration qualifier names</a>
     */
    final String qualifier

    /**
     * The resource element itself.
     *
     * <p>Warning: Do not delete this element or change the "name" attribute, otherwise your code may break.
     */
    final Element element

    /**
     * The type of resource (i.e. the tag name of the resource element). Can be one of "string", "bool", "color",
     * "dimen", "integer", "integer-array", "array", "string-array", "plurals" or "style"
     *
     * @return The type of resource
     */
    String getType() {
        element.tagName
    }

    /**
     * The name of the resource.
     */
    String getName() {
        element.getAttributeNS(null, 'name')
    }

    /**
     * Replace all string resources (string, string-array, plurals) that matches a pattern to another. This can be used
     * to change all references to a brand name in a channel, e.g.
     *
     * <pre>
     * values { channel, resValues ->
     *     resValues.with {
     *         if (qualifier =~ /\bes\b/) {
     *             xml.replaceStrings ~/PlataformaCompetidor/, 'PlataformaSocio'
     *         } else {
     *             xml.replaceStrings ~/CompetitorPlatform/, 'PartnerPlatform'
     *         }
     *     }
     * }
     * </pre>
     *
     * @param pattern The pattern to search for
     * @param replacement The replacement string (may use variables like "$1") or a closure (see
     *                    {@link org.codehaus.groovy.runtime.StringGroovyMethods#replaceAll(CharSequence, Pattern, Closure)
     *                    replaceAll(Pattern, Closure)}).
     */
    void replaceStrings(final Pattern pattern, final Object replacement) {
        if (type in ['string', 'string-array', 'plurals']) {
            DOMCategory.xpath(element, '//text()', XPathConstants.NODESET).each { Text node ->
                def src = node.data
                def res
                if (replacement instanceof CharSequence) {
                    res = src.replaceAll(pattern, replacement)
                } else if (replacement instanceof Closure) {
                    res = src.replaceAll(pattern, replacement)
                } else {
                    throw new InvalidUserCodeException("replacement in ResValue.replaceStrings() must either be a String or a Closure")
                }
                node.data = res
            }
        }
    }
}
