package io.github.clixyz.yota.events

import android.content.ComponentName
import android.content.Intent
import io.github.clixyz.yota.droid.Droid
import io.github.clixyz.yota.utils.Logger
import org.robolectric.server.am.MetroidActivityManager
import java.rmi.RemoteException

open class YotaStartActivityEvent(val cn: ComponentName) : YotaEvent {

    override fun inject(): Int {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            component = cn
        }
        MetroidActivityManager.getService().startActivity(intent, null)
        Thread.sleep(1000)
        return YotaEvent.INJECT_SUCCEEDED
//        return try {
//            Droid.exec { it.am.startActivity(null, null, intent, null, null, null, 0, 0, null, null) }
//            Thread.sleep(1000)
//            YotaEvent.INJECT_SUCCEEDED
//        } catch (e: RemoteException) {
//            Logger.e("Failed to talk with activity manager")
//            YotaEvent.INJECT_FAILED_REMOTE_EXCEPTION
//        } catch (e: SecurityException) {
//            Logger.e("Permissions error starting activity ${intent.toUri(0)}")
//            YotaEvent.INJECT_FAILED_SECURITY_EXCEPTION
//        } catch (e: InterruptedException) {
//            Logger.e("Thread interrupted when starting activity ${intent.toUri(0)}")
//            YotaEvent.INJECT_FAILED
//        }
    }
}