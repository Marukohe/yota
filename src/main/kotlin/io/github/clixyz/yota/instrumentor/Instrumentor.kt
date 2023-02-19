package io.github.clixyz.yota.instrumentor

import io.github.clixyz.yota.utils.*
import java.io.File
import org.robocli.soot.*
import org.robocli.soot.options.Options

fun instrument() {
    val apkFile = "$apkStorePath/$apkName".also {
        val tem = File(it)
        require(tem.exists()) {"apk File ${tem.absoluteFile} dose not exist!"}
    }

    val outApkDir = run {
        val path = "${getWorkingSpace()}/$outputDir"
        path.also {
            File(it).deleteRecursively()
            File(it).mkdirs()
        }
    }


    G.reset()
    Options.v().set_src_prec(Options.src_prec_apk)
    Options.v().set_output_format(Options.output_format_dex)
    Options.v().set_include_all(true)
    //!!!!!!!!!!!!!!!!!!!!!!!soot will corrupt the apk file if the api version is higher!!!!!!!!!!!!!!!!!!!!!!
    Options.v().set_android_api_version(sootAPIVersion)
    Options.v().set_whole_program(true)
    Options.v().set_prepend_classpath(true)
    Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES)
    Scene.v().addBasicClass("android.util.Log", SootClass.SIGNATURES);
    Options.v().set_process_multiple_dex(true)
    Options.v().set_allow_phantom_refs(true)

    PackManager.v().getPack("jtp").add(Transform("jtp.myCoverageInstrumenter", DetailTraceTransformer()))

    Main.main(arrayOf(
        "-force-android-jar", "$androidSDKAddress/platforms/android-${sootAPIVersion}/android.jar",
        "-d", outApkDir,
        "-process-dir", apkFile
    ))

    originApk = apkFile
    instrumentedApk = "$outApkDir/$apkName"
    resign("$outApkDir/$apkName")

    val resultsPath = "${System.getProperty("user.dir")}/results/coverage/${System.getProperty("running_app_pkg")}"
    resultsPath.also { path ->
        if (!File(path).exists()) {
            File(path).mkdirs()
        }
        File("${path}/$CoverageLabelFile").bufferedWriter().use { writer ->
            output.forEach {
                writer.write(it)
                writer.newLine()
            }
        }
    }
}

/**
 * Resign the instrumented app，so it can be installed on the phone
 * */
private fun resign(apkPath : String) {
    resign("$androidSDKAddress/build-tools/$androidBuildToolVersion",
        keyStoreFileName, keyAlias, apkPath, keyPwd)
}