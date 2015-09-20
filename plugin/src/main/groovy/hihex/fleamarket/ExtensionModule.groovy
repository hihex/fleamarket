package hihex.fleamarket
import groovy.transform.CompileStatic
import org.w3c.dom.Document
import org.w3c.dom.Element

import java.util.regex.Pattern
/**
 * Extension method to some XML objects. This provides some common AndroidManifest and resource value modifications when
 * using Channels.
 */
@CompileStatic
class ExtensionModule {
    private static final String ANDROID_NAMESPACE = 'http://schemas.android.com/apk/res/android'
    private static final Pattern ANALYTICS_NAME_REGEX = ~/_CHANNEL(?:_ID)?$|^(?:InstallChannel|leancloud)$/

    /**
     * Sets the channel ID for analytics services. This method will replace the value of all {@code &lt;meta-data&gt;}
     * which name ends with the word "_CHANNEL". Additionally we support the following services:
     *
     * <table border=1>
     *     <tr><th>Service<th>meta-data name
     *     <tr><td>Umeng 友盟<td>UMENG_CHANNEL
     *     <tr><td>TalkingData<td>TD_CHANNEL_ID
     *     <tr><td>百度移动统计<td>BaiduMobAd_CHANNEL
     *     <tr><td>腾讯云分析<td>InstallChannel
     *     <tr><td>诸葛io<td>ZHUGE_CHANNEL
     *     <tr><td>讯飞开放统计<td>IFLYTEK_CHANNEL
     *     <tr><td>LeanCloud (v2.6.8+)<td>leancloud
     * </table>
     *
     * Not all analytic services encode the channel into AndroidManifest. Some services like UTMini (无线数读) requires
     * modifying the source code, while most others like Flurry and Google Analytics does not support it natively. If
     * you are using these services, you may consider manually define a meta-data tag and reading the channel value from
     * the AndroidManifest meta-data in the Android Java source code.
     *
     * @param self An XML document.
     * @param channel The new channel value.
     */
    static void setAnalyticsChannel(final Document self, final String channel) {
        self.getElementsByTagName('meta-data').each { Element elem ->
            if (elem.getAttributeNS(ANDROID_NAMESPACE, 'name') =~ ANALYTICS_NAME_REGEX) {
                elem.setAttributeNS ANDROID_NAMESPACE, 'android:value', channel
            }
        }
    }

    /**
     * Deletes all tags with the given {@code android:name}. For instance, to delete a permission:
     *
     * <pre>
     *     doc.deleteTagsWithName 'uses-permission', 'android.permission.INJECT_EVENTS'
     * </pre>
     *
     * to delete an activity:
     *
     * <pre>
     *     doc.deleteTagsWithName 'activity', 'com.example.myapp.ThisMarketsCompetitorActivity'
     * </pre>
     *
     * @param self An XML document.
     * @param tagName The tag name.
     * @param name The name.
     */
    static void deleteTagsWithName(final Document self, final String tagName, final String name) {
        self.getElementsByTagName(tagName).each { Element elem ->
            if (elem.getAttributeNS(ANDROID_NAMESPACE, 'name') == name) {
                elem.parentNode.removeChild elem
            }
        }
    }

    /**
     * Adds a {@code <uses-permission>} tag to the end of the root element (i.e. the {@code <manifest>} tag)
     *
     * @param self An XML document.
     * @param name The permission name, e.g. {@code 'android.permission.INTERNET'}.
     */
    static void addUsesPermission(final Document self, final String name) {
        final permission = self.createElement('uses-permission')
        permission.setAttributeNS(ANDROID_NAMESPACE, 'android:name', name)
        self.documentElement.appendChild(permission)
    }
}
