package io.github.clixyz.yota

import edu.nju.ics.marukohe.metroid.App
import edu.nju.ics.marukohe.metroid.Device
import edu.nju.ics.marukohe.metroid.Metroid
import edu.nju.ics.marukohe.metroid.MetroidStarter
import edu.nju.ics.marukohe.metroid.utils.Options
import io.github.clixyz.yota.cmds.mnky.YotaMnky
import io.github.clixyz.yota.utils.*
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo
import org.robolectric.shadows.util.DataSource
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    System.setProperty("robolectric.logging.enabled", "false")
    System.setProperty("android.util.log.added", "false")

    Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
        println("Error: $e")
        e.printStackTrace()
        exitProcess(1)
    }

    if (args.isEmpty()) {
        System.err.println("Must give a config path.")
        exitProcess(1)
    }

    parseConfig(args[0])
//    instrument()

    val builder = Options.Builder()
    builder
        .setQualifiers(qualifier)
        .setSdk(sdk)
//        .setApkPath(instrumentedApk)
        .setApkPath("/Users/hewei/Workspace/yota/monkeyinstance/default/instrumentedApks/ac.robinson.mediaphone_51.apk")
        .setInstance(instanceName)
        .setOverwriteJarfile(false)
//        .setScriptPath(scriptPath)

    Metroid.init(builder.build(), Main::class.java)
}

class Main : MetroidStarter() {

    override fun registerCallbacks(device: Device?) {
        device!!.setupCallbacks(object : App.Callbacks {
            override fun beforeAppCreate() {
                ShadowLog.registerListener(CoverageListener())
            }

            override fun onApplicationIsReady() {
                // rocks.poopjournal.morse
                val dataSource = DataSource.toDataSource("android.resource://rocks.poopjournal.morse/2131689473")
                val mediaInfo = MediaInfo()
                ShadowMediaPlayer.addMediaInfo(dataSource, mediaInfo)
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://rocks.poopjournal.morse/2131689474"),
                    MediaInfo()
                )

                // com.chess.clock
                // android.resource://com.chess.clock/2131820546
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://com.chess.clock/2131820546"),
                    MediaInfo()
                )
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://com.chess.clock/2131820547"),
                    MediaInfo()
                )
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://com.chess.clock/2131820548"),
                    MediaInfo()
                )
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://com.chess.clock/2131820545"),
                    MediaInfo()
                )
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://com.chess.clock/2131820544"),
                    MediaInfo()
                )

                // me.tsukanov.counter
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://me.tsukanov.counter/2131623937"),
                    MediaInfo()
                )
                ShadowMediaPlayer.addMediaInfo(
                    DataSource.toDataSource("android.resource://me.tsukanov.counter/2131623936"),
                    MediaInfo()
                )
            }

            override fun onApplicationIsDestroyed() {}

            override fun onFirstActivityReady(appInstalledTime: Long) {
                val monkeyArgs = mutableListOf(
                    "-p", System.getProperty("running_app_pkg"),
                    "10"
                )
//                monkeyArgs.add("--start-up-time")
//                monkeyArgs.add(appInstalledTime.toString())
//                monkeyArgs.add("100000000")
//                monkeyArgs.add("-p $pkgName")
//                monkeyArgs.add("10")
                exitProcess(YotaMnky("/data/local/tmp").exec(monkeyArgs.toTypedArray()).code)
            }

            override fun onActivityChanged() {}
        })
    }
}