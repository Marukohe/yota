package io.github.clixyz.yota.cmds.mnky

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.RemoteException
import android.os.UserHandle
import android.view.KeyEvent
import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.droid.delegates.UiAutoDelegate
import io.github.clixyz.yota.events.*
import io.github.clixyz.yota.utils.Command
import io.github.clixyz.yota.utils.Logger
import io.github.clixyz.yota.utils.OptParser
import java.io.PrintStream

class YotaMnky(val stateDir: String) : Command {
    override val name: String = "mnky"
    override val usage: String by lazy {
        "mnky: a monkey-like tool that removes redundant events\n" +
        "\n" +
        "usage: \n" +
        "  mnky [-P POLICY]\n" +
        "       [-s SEED]\n" +
        "       [-a MAIN_ACTIVITY]\n" +
        "       [--throttle THROTTLE]\n" +
        "       [--attr-path LENGTH]\n" +
        "       [--pct-last-page PCT]\n" +
        "       [--pct-enter-after-text PCT]\n" +
        "       [--only-alnum]\n" +
        "       [--save-state]\n" +
        "       [--save-screenshot]\n" +
        "       [--show-timestamp]\n" +
        "       [--show-activity]\n" +
        "       [--stop-on-exit]\n" +
        "       -p APP_PACKAGE\n" +
        "       -C COUNT\n" +
        "\n" +
        "arguments:\n" +
        "  -p APP_PACKAGE  package name of app under test\n" +
        "  -C COUNT        number of events to be fired\n" +
        "\n" +
        "options:\n" +
        "  -P POLICY                  one of {random, dfs}, by default, random\n" +
        "  -s SEED                    seed, by default, system time\n" +
        "  -a MAIN_ACTIVITY           main activity to be launched, by default, \n" +
        "                             mnky will find one with ACTION_MAIN+CATEGORY_LAUNCHER\n" +
        "  --throttle THROTTLE        interval (ms) between events, by default, 0\n" +
        "  --attr-path LENGTH         show attribute path of length LENGTH of each widget\n" +
        "                             instead of widget information (classname, index, text, ...),\n" +
        "                             by default, LENGTH is 0, i.e., show widget information\n" +
        "  --pct-last-page PCT        percent navigating back to last page, i.e., press BACK,\n" +
        "                             requiring an integer (<100), by default, 10\n" +
        "  --pct-enter-after-text PCT percent pressing enter after sending text event,\n" +
        "                             requiring an integer (<100), by default, 75\n" +
        "  --only-alnum               use only alphabet and number when fuzz string,\n" +
        "                             by default, yes\n" +
        "  --save-state               save state after each event, saved states\n" +
        "                             are saved in directory " + stateDir + ",\n" +
        "                             by default, state will not be saved\n" +
        "  --save-screenshot          save screenshot, only enabled with --save-state\n" +
        "  --show-timestamp           show timestamp (ms) before events and states,\n" +
        "                             by default, don't show\n" +
        "  --show-activity            show activity name of each fired events,\n" +
        "                             by default, don't show\n" +
        "  --stop-on-exit             stop mnky when app exit, either exited normally, or\n" +
        "                             exited by send key BACK, by default, don't stop"
    }

    companion object {
        val SUCCEEDED_NOTHING_HAPPENED = Command.Status(0, "succeeded")
        val SUCCEEDED_APP_CRASH = Command.Status(1, "succeeded: app crashed")
        val SUCCEEDED_APP_ANR = Command.Status(2, "succeeded: app anr")
        val SUCCEEDED_APP_EXIT = Command.Status(3, "succeeded: app exited")
        val FAILED_INSUFFICIENT_ARGS = Command.Status(-1, "insufficient args")
        val FAILED_INVALID_ARGUMENT = Command.Status(-2, "invalid argument")
        val FAILED_CANNOT_INIT_DROID = Command.Status(-3, "failed to init droid")
        val FAILED_APP_CANNOT_START = Command.Status(-4, "failed to start app")
        val FAILED_EXCEPTION = Command.Status(-5, "exception happened")
        val FAILED_INITIALIZE_MNKY = Command.Status(-6, "failed initialize mnky")
    }

    // arguments (required)
    private lateinit var args: Array<String>
    private lateinit var appPackage: String
    private var count: Long = -1

    // options (required)
    private var seed = System.currentTimeMillis()
    private var policy = "random"
    private var activity: String? = null
    private var saveState = false
    private var saveScreenshot = false
    private var showTimestamp = false
    private var onlyAlnum = false
    private var stopOnExit = false
    private var showActivity = false
    private var throttle = 0L
    private var pctLastPage = 10
    private var pctEnterAfterText = 75
    private var attrPathLength = 0

    // artifacts depending on arguments and/or options
    private lateinit var random: MnkyRandom
    private lateinit var source: MnkyEventSource
    private lateinit var camera: MnkyStateCamera

    // counters
    private var counter = 0
    private var failedCounter = 0
    private var droppedCounter = 0

    // flags
    private var appCrashed = false
    private var appAnred = false
    private var appCannotStart = false

    override fun exec(args: Array<String>): Command.Status {
        this.args = args

        if (!processOptions()) {
            Logger.e("Failed to process options")
            showUsage(System.err)
            return FAILED_INVALID_ARGUMENT
        }

        return try {
            Droid.init()
            runMnky()
        } catch (e: Droid.UnableToInitException) {
            Logger.e("Failed to initialize droid")
            FAILED_CANNOT_INIT_DROID
        } finally {
            Droid.deinit()
        }
    }

    private fun runMnky(): Command.Status {
        if (!initMnky()) {
            Logger.e("Failed to initialize mnky")
            return FAILED_INITIALIZE_MNKY
        }

        val startTime = System.currentTimeMillis()
        try {
            runMnkyCycle()
        } catch (t: Throwable) {
            t.message?.let(Logger::e)
            t.stackTrace?.let(Logger::e)
            return FAILED_EXCEPTION
        } finally {
            val endTime = System.currentTimeMillis()
            Logger.i(String.format("Mnky sent %d/%d events, %d failed, %d dropped, using %.4fs",
                    counter - failedCounter - droppedCounter,
                    counter, failedCounter, droppedCounter,
                    (endTime - startTime).toDouble() / 1000))
        }

        return when {
            appCannotStart -> FAILED_APP_CANNOT_START
            appCrashed -> SUCCEEDED_APP_CRASH
            appAnred -> SUCCEEDED_APP_ANR
            stopOnExit && counter < count -> SUCCEEDED_APP_EXIT
            else -> SUCCEEDED_NOTHING_HAPPENED
        }
    }

    private fun runMnkyCycle() {
        counter = 0
        failedCounter = 0
        droppedCounter = 0
        appCrashed = false
        appAnred = false
        appCannotStart = false

        var startAppCounter = 0
        var isFirstAppStartup = true
        var lastEvent: YotaEvent? = null

        while (counter < count) {
            var ev: YotaEvent?
            var rt: Int? = null
            var timestamp: Long = System.currentTimeMillis()

            val ua = Droid.ua
            val am = Droid.am
            val fapp = am.foregroundAppName
            when (fapp) {
                null -> { // maybe something is running in foreground
                    Logger.w("App is doing some tasks, e.g., networking")
                    ev = source.getNextEvent()
                }
                appPackage -> { // ok
                    ev = source.getNextEvent()
                }
                "android" -> { // system dialog is shown
                    rt = ua.checkSystemDialog()
                    when (rt) {
                        UiAutoDelegate.SYSTEM_DIALOG_CRASH -> {
                            Logger.i("*crash* App seemed to crash", System.err)
                            appCrashed = true
                            return
                        }
                        UiAutoDelegate.SYSTEM_DIALOG_ANR -> {
                            Logger.i("*anr* App seemed not responding", System.err)
                            appAnred = true
                            return
                        }
                        else -> { // other system dialog, e.g., the "open with" dialog, dismiss it
                            ev = YotaDismissDialogEvent()
                        }
                    }
                }
                else -> {
                    // some events put app hide into background, put it into foreground
                    // check how many times start-activity was continuously fired,
                    // if it was continuously fired more than 5 times, but the app
                    // is still in background, some error may happen
                    if (!isFirstAppStartup && stopOnExit) {
                        return
                    } else {
                        isFirstAppStartup = false
                    }

                    val lastIsStartApp = lastEvent is YotaStartActivityEvent

                    if (!lastIsStartApp) {
                        startAppCounter = 0 // reset counter
                    } else if (startAppCounter > 5) {
                        Logger.i("*app-not-start* app cannot start after 5 trials", System.err)
                        appCannotStart = true
                        return
                    }

                    Logger.w("App was put into background by $fapp")
                    ev = YotaStartActivityEvent(ComponentName(appPackage, activity))
                    startAppCounter += 1
                }
            }

            if (ev != null) {
                val activityName = if (showActivity) { 
                    am.topActivityName
                } else { 
                    null 
                }
                rt = ev.inject()
                if (rt == YotaEvent.INJECT_FAILED_REMOTE_EXCEPTION) {
                    Logger.e("RemoteException happened when injecting events")
                    failedCounter ++
                    return
                } else if (rt == YotaEvent.INJECT_FAILED_SECURITY_EXCEPTION) {
                    Logger.w("SecurityException happened when injecting events")
                    failedCounter ++
                } else if (rt == YotaEvent.INJECT_FAILED) {
                    Logger.w("Injection failed for unknown reasons")
                    failedCounter ++
                } else {
                    if (showActivity) {
                        if (showTimestamp) {
                            Logger.i("|activity| $timestamp $activityName")
                        } else {
                            Logger.i("|activity| $activityName")
                        }
                    }
                    if (showTimestamp) {
                        Logger.i(":event: " + timestamp + " " + getEventDesc(ev))
                    } else {
                        Logger.i(":event: " + getEventDesc(ev))
                    }
                }
            } else {
                droppedCounter ++
            }

            // set throttle
            if (ev != null && rt == YotaEvent.INJECT_SUCCEEDED && throttle != 0L) {
                // don't care whether it is succeeded or not
                YotaThrottleEvent(throttle).inject()
            }

            // save state w/o screenshot
            if (ev != null && rt == YotaEvent.INJECT_SUCCEEDED && saveState) {
                val path = "/data/local/tmp/mnky_state_${appPackage}_$counter"
                val xmlPath = "$path.xml"
                timestamp = System.currentTimeMillis()
                rt = camera.capture(ua, xmlPath)
                if (rt == MnkyStateCamera.CAPTURE_SUCCEEDED) {
                    if (showTimestamp) {
                        Logger.i(".state. $timestamp $xmlPath")
                    } else {
                        Logger.i(".state. $xmlPath")
                    }
                } else if (rt == MnkyStateCamera.CAPTURE_FAILED_NULL_ROOT_EXCEPTION) {
                    Logger.w("Root is null when capturing current state")
                    if (showTimestamp) {
                        Logger.i(".state. $timestamp __stub__")
                    } else {
                        Logger.i(".state. __stub__")
                    }
                } else if (rt == MnkyStateCamera.CAPTURE_FAILED_IO_EXCEPTION) {
                    Logger.w("IOException happened when capturing current state")
                    if (showTimestamp) {
                        Logger.i(".state. $timestamp __stub__")
                    } else {
                        Logger.i(".state. __stub__")
                    }
                } else {
                    Logger.w("Failed to capture current state")
                    if (showTimestamp) {
                        Logger.i(".state. $timestamp __stub__")
                    } else {
                        Logger.i(".state. __stub__")
                    }
                }

                if (rt == MnkyStateCamera.CAPTURE_SUCCEEDED && saveScreenshot) {
                    val pngPath = "$path.png"
                    timestamp = System.currentTimeMillis()
                    rt = camera.takeScreenshot(ua, pngPath)
                    if (rt == MnkyStateCamera.SCREENSHOT_SUCCEEDED) {
                        if (showTimestamp) {
                            Logger.i(".screenshot. $timestamp $pngPath")
                        } else {
                            Logger.i(".screenshot. $pngPath")
                        }
                    } else if (rt == MnkyStateCamera.SCREENSHOT_FAILED_IO_EXCEPTION) {
                        Logger.w("IOException happened when capturing current state")
                    } else {
                        Logger.w("Failed to capture current state")
                    }
                }
            }

            counter ++
            lastEvent = ev
        }
    }

    private fun getEventDesc(event: YotaEvent): String {
        val builder = StringBuilder()

        // FORMAT: :event_type: event_args -- node_info
        when (event) {
            is YotaViewCompoundEvent -> {
                val internalEvent = event.internalEvent
                val view = event.view
                if (attrPathLength != 0) { // show attribute path
                    builder.append(getEventDesc(internalEvent))
                            .append(" -- ")
                            .append(view.attrPath(attrPathLength).toString())
                } else { // show widget information
                    builder.append(getEventDesc(internalEvent))
                            .append(" -- ")
                            .append("index=\"${view.idx}\",classname=\"${view.cls}\",text=\"${view.text}\",desc=\"${view.desc}\"")
                }
            }
            is YotaTapEvent -> builder.append(":tap: (${event.x}, ${event.y})")
            is YotaSwipeEvent -> builder.append(":swipe: (${event.fromX}, ${event.fromY}), ${event.steps}, (${event.toX}, ${event.toY})")
            is YotaTextEvent -> builder.append(":text: ${event.text}")
            is YotaKeyEvent -> builder.append(":key: ${KeyEvent.keyCodeToString(event.key)}")
            is YotaStartActivityEvent -> builder.append(":start-activity: ${event.cn.packageName}/${event.cn.className}")
            is YotaSleepEvent -> builder.append(":sleep: ${event.ms}ms")
            is YotaThrottleEvent -> builder.append(":throttle: ${event.ms}ms")
            is YotaNoopEvent -> builder.append(":noop:")
        }

        return builder.toString()
    }

    private fun processOptions(): Boolean {
        val parser = OptParser(args)

        for (opt in parser) {
            // arguments (required)
            if (opt == "-p") {
                val p = parser.get(opt)
                if (p == null || p.isEmpty()) {
                    Logger.e("No apps are provided, use -p to provide an application")
                    return false
                }
                appPackage = p
            } else if (opt == "-C") {
                val c = parser.get(opt)
                if (c == null || c.isEmpty()) {
                    Logger.e("No count is provided, use -C to provide a count")
                    return false
                }
                try {
                    count = java.lang.Long.parseLong(c)
                } catch (e: NumberFormatException) {
                    Logger.e("Invalid count $c, should be an integer")
                    return false
                }
            }
            // options (optional)
            else if (opt == "-a") {
                val act = parser.get(opt)
                if (act == null || act.isEmpty()) {
                    Logger.w("No activities are provided, will automatically find the main activity")
                } else {
                    activity = act
                }
            } else if (opt == "-s") {
                val s = parser.get(opt)
                if (s == null || s.isEmpty()) {
                    Logger.w("Invalid seed, use system time by default")
                } else {
                    try {
                        seed = java.lang.Long.parseLong(s)
                    } catch (e: NumberFormatException) {
                        Logger.w("Invalid seed $s, should be an integer, use system time by default")
                    }

                }
            } else if (opt == "-P") {
                val p = parser.get(opt)
                when (p) {
                    "random", "dfs" -> policy = p
                    else -> Logger.w("Invalid policy (should be one of random), use random by default")
                }
            } else if (opt == "--throttle") {
                val t = parser.get(opt)
                if (t == null || t.isEmpty()) {
                    Logger.w("Invalid throttle, discard it")
                } else {
                    try {
                        throttle = java.lang.Long.parseLong(t)
                    } catch (e: NumberFormatException) {
                        Logger.w("Invalid throttle $t, should be an integer, discard it")
                    }
                }
            } else if (opt == "--pct-last-page") {
                val p = parser.get(opt)
                if (p == null || p.isEmpty()) {
                    Logger.w("Invalid percent, discard it")
                } else {
                    var percent: Int?
                    try {
                        percent = Integer.parseInt(p)
                        if (percent >= 100) {
                            Logger.w("Invalid percent $p, should be less than 100, discard it")
                        } else {
                            pctLastPage = percent
                        }
                    } catch (e: NumberFormatException) {
                        Logger.w("Invalid percent $p, should be an integer, discard it")
                    }
                }
            } else if (opt == "--pct-enter-after-text") {
                val p = parser.get(opt)
                if (p == null || p.isEmpty()) {
                    Logger.w("Invalid percent, discard it")
                } else {
                    var percent: Int?
                    try {
                        percent = Integer.parseInt(p)
                        if (percent >= 100) {
                            Logger.w("Invalid percent $p, should be less than 100, discard it")
                        } else {
                            pctEnterAfterText = percent
                        }
                    } catch (e: NumberFormatException) {
                        Logger.w("Invalid percent $p, should be an integer, discard it")
                    }

                }
            } else if (opt == "--attr-path") {
                val length = parser.get(opt)
                if (length == null || length.isEmpty()) {
                    Logger.w("Invalid attribute path length, discard it")
                } else {
                    try {
                        attrPathLength = Integer.parseInt(length)
                    } catch (e: NumberFormatException) {
                        Logger.w("Invalid attribute path length $length, should be an integer, discard it")
                    }

                }
            } else if (opt == "--save-state") {
                saveState = true
            } else if (opt == "--save-screenshot") {
                saveScreenshot = true
            } else if (opt == "--show-timestamp") {
                showTimestamp = true
            } else if (opt == "--show-activity") {
                showActivity = true
            } else if (opt == "--only-alnum") {
                onlyAlnum = true
            } else if (opt == "--stop-on-exit") {
                stopOnExit = true
            } else {
                Logger.w("Invalid option $opt found, skip it")
            }
        }

        if (!this::appPackage.isInitialized) {
            Logger.e("No apps are provided, use -p to provide an application")
            return false
        }

        if (count < 0) {
            Logger.e("No counts are provided, use -C to provide a count")
            return false
        }

        return true
    }

    private fun initMnky(): Boolean {
        if (activity == null) {
            activity = this.findActivity()
            if (activity == null) {
                Logger.e("No main activity found")
                return false
            }
        } else if (activity!!.startsWith(".")) {
            activity = appPackage + activity
        }

        random = MnkyRandom(seed)

        source = when (policy) {
            "random" -> {
                val proba = MnkyEventSourceRandom.Proba(pctLastPage.toDouble()/100,
                        pctEnterAfterText.toDouble()/100)
                MnkyEventSourceRandom(random, Droid.ua, proba)
            }
            "dfs" -> {
                MnkyEventSourceDfs(Droid.ua, Droid.am)
            }
            else -> {
                Logger.e("Invalid policy: $policy")
                return false
            }
        }

        if (saveState) {
            camera = MnkyStateCamera()
        }

        return true
    }

    private fun findActivity(): String? {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val mainActList: List<ResolveInfo>
        try {
            val actList = Droid.pm.queryIntentActivities(intent, null, 0,
                    UserHandle.myUserId()).list
            if (actList == null || actList.isEmpty()) {
                Logger.e("No ACTION_MAIN/CATEGORY_LAUNCHER-ed activities found")
                return null
            }

            mainActList = actList
                    .filter { r ->
                        r is ResolveInfo && appPackage == r.activityInfo?.applicationInfo?.packageName
                    }
                    .map { r ->
                        r as ResolveInfo
                    }

            if (mainActList.isEmpty()) {
                Logger.e("No ACTION_MAIN/CATEGORY_LAUNCHER-ed activities found in $appPackage")
                return null
            }
        } catch (e: RemoteException) {
            e.message?.let(Logger::e)
            e.stackTrace?.let(Logger::e)
            return null
        }

        val info: ResolveInfo
        info = if (mainActList.size != 1) {
            Logger.w("Multiple main activities found, use random one")
            mainActList[random.nextInt(mainActList.size)]
        } else {
            mainActList[0]
        }

        return info.activityInfo.name
    }

    private fun showUsage(out: PrintStream) {
        out.println(usage)
    }
}