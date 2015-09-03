package hihex.fleamarket.model
import com.android.builder.model.SigningConfig
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.w3c.dom.Document

@CompileStatic
class Channel {
    ChannelFilename filename
    String name
    SigningConfig signingConfig
    String[] flavors
    String buildType
    ArrayList<ChannelAction<Document>> manifestModifications
    ArrayList<ChannelAction<ResValue>> valueReplacements
    HashMap<String, File> assets
    ArrayList<File> resources

    Channel() {
        filename = { Channel c -> "${c.name}.apk" }
        flavors = []
        buildType = 'release'
        manifestModifications = []
        valueReplacements = []
        assets = [:]
        resources = []
    }

    Channel(final String name, final Channel base) {
        filename = base.filename
        this.name = name
        signingConfig = base.signingConfig
        flavors = base.flavors
        buildType = base.buildType
        manifestModifications = new ArrayList<>(base.manifestModifications)
        valueReplacements = new ArrayList<>(base.valueReplacements)
        assets = new HashMap<>(base.assets)
        resources = new ArrayList<>(base.resources)
    }

    Channel createShallowCopy(final String newName) {
        new Channel(
                name: newName,
                filename: filename,
                signingConfig: signingConfig,
                flavors: flavors,
                buildType: buildType,
                manifestModifications: manifestModifications,
                valueReplacements: valueReplacements,
                assets: assets,
                resources: resources
        )
    }

    String getPackageTaskName() {
        'package' + flavors.collect { String s -> s.capitalize() }.join('') + buildType.capitalize()
    }

    String getIntermediateDirName() {
        if (flavors) {
            "${flavors.join('')}/${buildType}"
        } else {
            buildType
        }
    }

    File getInputApk(final Project project) {
        final apkName = "outputs/apk/${project.name}-${flavors ? flavors.join('-')+'-' : ''}${buildType}-unsigned.apk"
        new File(project.buildDir, apkName)
        //project.tasks[packageTaskName].outputs.files.singleFile
    }
}

