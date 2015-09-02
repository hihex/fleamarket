package hihex.fleamarket.dsl
import com.android.builder.model.SigningConfig
import groovy.transform.CompileStatic
import hihex.fleamarket.model.ChannelAction
import hihex.fleamarket.model.ChannelFilename
import hihex.fleamarket.model.Channel
import hihex.fleamarket.model.ResValue
import org.w3c.dom.Document
/**
 * Configuration for a particular channel.
 */
@CompileStatic
class Config {
    Channel channel

    /**
     * Sets the output filename for this channel.
     *
     * @param filename The output filename (should contain the ".apk" suffix).
     */
    void filename(final String filename) {
        channel.filename = { filename }
    }

    /**
     * Sets the output filename for this channel. The {@link Channel} object will be provided to the closure to
     * construct the filename.
     *
     * @param filename The output filename function (the output should contain the ".apk" suffix).
     */
    void filename(final ChannelFilename filename) {
        channel.filename = filename
    }

    /**
     * Sets the signing configuration used to sign the APK.
     *
     * @param signingConfig The signing configuration. Set to null to skip the signing step.
     */
    void signingConfig(final SigningConfig signingConfig) {
        channel.signingConfig = signingConfig
    }

    /**
     * The (dimensioned) product flavors to base on.
     *
     * <p>If the project defines productFlavors, then this field must be supplied. It must include all flavors using the
     * same dimension order just like inside the android block. Example:
     *
     * <pre>
     * flavors 'arm', 'freeapp'
     * </pre>
     *
     * @param flavors The list of dimensioned flavors
     */
    void flavors(final String... flavors) {
        channel.flavors = flavors
    }

    /**
     * The build type of the APK. The default is "release".
     *
     * @param buildType The build type, either "release" or "debug".
     */
    void buildType(final String buildType) {
        channel.buildType = buildType
    }

    /**
     * Defines a list of modifications that should be performed on the AndroidManifest.xml for this APK.
     *
     * @param modifications The modifications that may be applied on the XML document.
     * @see hihex.fleamarket.ExtensionModule
     */
    void manifest(final ChannelAction<Document> modifications) {
        channel.manifestModifications << modifications
    }

    /**
     * Replaces the resource values (e.g. strings, colors, dimensions).
     *
     * @param replacement The replacement acted on every resource values.
     */
    void values(final ChannelAction<ResValue> replacements) {
        channel.valueReplacements << replacements
    }

    /**
     * Adds or replaces a new file in the assets/ folder.
     *
     * @param assetPath The relative path to inside the assets/ folder, same as the path you would use in
     *                  {@code AssetManager.open()}
     * @param input The file to copy from
     */
    void asset(final String assetPath, final File input) {
        channel.assets[assetPath] = input
    }

    /**
     * Replaces some existing resources (drawables, layouts, XMLs, menus, animations, etc.)
     *
     * <p>The folder should have the same structure of the merged res/ folder. All files should already exist in the
     * APK. Be aware that the manifest merging may rename some folders (usually adding -v4 or -v13 suffixes).
     *
     * @param folder
     */
    void resources(final File folder) {
        channel.resources << folder
    }
}
