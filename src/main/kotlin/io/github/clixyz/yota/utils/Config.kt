package io.github.clixyz.yota.utils

import java.io.IOException
import java.util.*

const val sootAPIVersion = 28
val os_name = System.getProperty("os.name").lowercase(Locale.getDefault())

lateinit var workingDir: String

const val CoverageLabelFile = "labels.txt"
const val CoverageOutPutFile = "Coverage.xml"
const val SOOT_COV_TAG = "Metroid.COV"

var sdk: Int = 28
lateinit var qualifier: String
lateinit var apkStorePath: String
lateinit var apkName: String
lateinit var outputDir: String
lateinit var pkgName: String
lateinit var runningMinutes: String
lateinit var instanceName: String
lateinit var keyStoreFileName: String
lateinit var keyAlias : String
lateinit var keyPwd : String
lateinit var androidSDKAddress: String
lateinit var androidBuildToolVersion : String
lateinit var originApk : String
lateinit var instrumentedApk : String
lateinit var scriptPath : String

fun getWorkingSpace(): String {
    if(!::workingDir.isInitialized) {
        workingDir = "${System.getProperty("user.dir")}/monkeyinstance/${System.getProperty("MonkeyInstanceName")}"
    }
    return workingDir
}

fun parseConfig(configFile: String) {
    val properties = Properties()
    val classLoader = Thread.currentThread().contextClassLoader
    try {
        val configIn = classLoader.getResourceAsStream(configFile)
        val pathIn = if (os_name.contains("mac")) {
            classLoader.getResourceAsStream("mac_runtime.properties")
        } else {
            classLoader.getResourceAsStream("linux_runtime.properties")
        }

        properties.load(configIn)
        properties.load(pathIn)
    } catch (e: IOException) {
        e.printStackTrace()
    }

    apkStorePath = properties["basePath"] as String
    apkName = properties["apkPath"] as String
    qualifier = properties["qualifiers"] as String
    sdk = (properties["sdk"] as String).toInt()
    pkgName = properties["pkgName"] as String
    System.setProperty("running_app_pkg", pkgName)
    runningMinutes = properties["runningMinutes"] as String
    instanceName = properties.getOrDefault("instance", "default") as String
    System.setProperty("MonkeyInstanceName", instanceName)
    androidSDKAddress = properties["androidSDK-dir"] as String
    outputDir = properties["instrument-output-dir"] as String
    keyStoreFileName = properties["keystore-path"] as String
    keyAlias = properties["key-alias"] as String
    keyPwd = properties["key-password"] as String
    androidBuildToolVersion = properties.getOrDefault("android-buildtool-version", "28.0.3") as String

    var dumpTimInterval = Integer.parseInt(properties["dumpTimeInterval"] as String)
    dumpTimInterval *= 1000
    System.setProperty("coverage.dumpTimInterval", dumpTimInterval.toString())

    scriptPath = "${System.getProperty("user.dir")}/scripts/$pkgName.json"
}