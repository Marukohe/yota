package android.support.test.uiautomator;

public class UiDevice {

    private static UiDevice sInstance;

    public static UiDevice getInstance() {
        synchronized (UiDevice.class) {
            if (sInstance == null) {
                sInstance = new UiDevice();
            }
        }
        return sInstance;
    }

    private UiDevice() {}
}
