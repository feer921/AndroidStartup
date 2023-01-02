package com.fee.start;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import androidx.annotation.NonNull;

/**
 * *****************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/23<br>
 * Time: 14:47<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
class HookIActivityManager implements InvocationHandler, Handler.Callback {

    /**
     * ActivityManager # private static final Singleton<IActivityManager>
     *     IActivityManagerSingleton#get()时所提供的 IActivityManager 对象
     */
    private Object mSystemIActivityManagerObj = null;

    public void hookAms() {
        try {
            int sdkInt = Build.VERSION.SDK_INT;
            Field iActivityManagerSingletonField = null;
            if (sdkInt >= Build.VERSION_CODES.O) {// >=Android 8.0
                // hook 【ActivityManager】
                // 中的
                // public static IActivityManager getService() {
                //        return IActivityManagerSingleton.get();
                //    }

                //private static final Singleton<IActivityManager> IActivityManagerSingleton =
                //            new Singleton<IActivityManager>() {
                //                @Override
                //                protected IActivityManager create() {
                //                    final IBinder b = ServiceManager.getService(Context.ACTIVITY_SERVICE);
                //                    final IActivityManager am = IActivityManager.Stub.asInterface(b);
                //                    return am;
                //                }
                //            };
                Class<?> activityManagerClass = Class.forName("android.app.ActivityManager");
                activityManagerClass.getDeclaredField("IActivityManagerSingleton");
            } else {
                Class<?> classOfActivityManagerNative = Class.forName("android.app" +
                        ".ActivityManagerNative");
                iActivityManagerSingletonField = classOfActivityManagerNative.getDeclaredField(
                        "gDefault");
            }
            iActivityManagerSingletonField.setAccessible(true);
            //  private static final Singleton<IActivityManager> IActivityManagerSingleton
            Object sActivityManagerSingleton = iActivityManagerSingletonField.get(null);
            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = singletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);
            final Object mInstance = mInstanceField.get(sActivityManagerSingleton);
            mSystemIActivityManagerObj = mInstance;
            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Object iActivityManagerProxy =
                    Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                            new Class[]{iActivityManagerClass}, this);
            //替换系统(ActivityManager)的 Singleton<IActivityManager> IActivityManagerSingleton 的 mInstance
            //SingleTon.mInstance = iActivityManagerProxy
            mInstanceField.set(mInstance,iActivityManagerProxy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String curMethodName = method.getName();
        if ("startActivity".equals(curMethodName)) {
            //拿到 启动 Activity时的 Intent,这里的 Intent 是启动 插件 APK内的Activity的 Intent,需要修改成 已经注册的
            // 的宿主 代理 Activity
            Intent srcStartIntent = null;
            int intentArgIndex = -1;
            if (args != null) {
                int argsLen = args.length;
                for (int i = 0; i < argsLen; i++) {
                    Object arg = args[i];
                    if (arg instanceof Intent) {
                        intentArgIndex = i;
                        srcStartIntent = (Intent) arg;
                        break;
                    }
                }
            }

            Intent startProxyIntent = new Intent();
            startProxyIntent.setClassName("com.fee.start", "com.fee.start.HostProxyActivity");
            startProxyIntent.putExtra("srcIntent", srcStartIntent);
            if (intentArgIndex != -1) {
                args[intentArgIndex] = startProxyIntent;
            }
        }
        // 这样，相当于仍然是调用 系统的 IActivityManager 对象的相关功能、方法
        return method.invoke(mSystemIActivityManagerObj, args);
    }


    public void hookActivityThreadHandler(){
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
//            private static volatile ActivityThread sCurrentActivityThread;
            Field sCurActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            sCurActivityThreadField.setAccessible(true);
            Object activityThread = sCurActivityThreadField.get(null);
            //final H mH = new H();
            //mh 对象
            Field mHField = activityThreadClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            Handler mH = (Handler) mHField.get(activityThread);
            //Handler.java final Callback mCallback;

            Field mCallbackField = mH.getClass().getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);

            mCallbackField.set(mH, this);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param msg A {@link Message Message} object
     * @return True if no further handling is desired
     */
    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 100: //Android 8.0,Android 9.0非 100
                //拿到 当前携带的 intent
                try {
                    Object activityClientRecordObj = msg.obj;
                    Field intentField = activityClientRecordObj.getClass().getDeclaredField(
                            "intent");
                    intentField.setAccessible(true);
                    //携带的 原 Intent
                    Intent srcIntent = (Intent) intentField.get(activityClientRecordObj);
                    if (srcIntent != null) {
                        //取出目标 插件中的 Intent
                        Intent startPluginIntent = srcIntent.getParcelableExtra("srcIntent");
                        if (startPluginIntent != null) {
                            //从而 把 ActivityThread 中将要处理的 启动 Activity的 message的 Intent 改为了 启动插件中的
                            // Activity 的 Intent
                            intentField.set(activityClientRecordObj, startPluginIntent);
                        }
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
                break;

            case 159:// Android 9.0, API 28

                break;
            default:
                break;
        }

        return false;
    }
}
