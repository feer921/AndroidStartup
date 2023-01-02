package com.fee.start;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

/**
 * *****************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2022/11/23<br>
 * Time: 13:18<br>
 * <P>DESC:
 *
 * </p>
 * ******************(^_^)***********************
 */
class LoadClassUtil {
    public static void loadClass(Context appContext,String apkDexPath) {
        // 找到 宿主的  dexElements -> dexElementsField -> DexPathList对象 -> pathList的 Field ->
        // BaseDexClassLoader 对象 ->
        try {

            Class<?> dexPathListClass = Class.forName("dalvik.system.DexPathList");
            // 查询到 【DexPathList】对象中的 Element[] dexElements 属性
            Field dexElementsField = dexPathListClass.getDeclaredField("dexElements");
            dexElementsField.setAccessible(true);
            //BaseDexClassLoader.java
            // # private final DexPathList pathList;
            Class<?> classLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
            Field pathListField = classLoaderClass.getDeclaredField("pathList");
            pathListField.setAccessible(true);

            //1. 获取宿主的类加载器 AppClassLoader extends BaseDexClassLoader
            ClassLoader pathClassLoader = appContext.getClassLoader();
            //获取到 该 对象private final DexPathList pathList;
            Object hostPathList = pathListField.get(pathClassLoader);
            //再获取到 【DexPathList】对象中 的 private Element[] dexElements; 数组
            Object[] hostDexElements = (Object[]) dexElementsField.get(hostPathList);

            //2. 插件(APK)的类加载器
            ClassLoader pluginClassLoader = new DexClassLoader(apkDexPath,
                    appContext.getCacheDir().getAbsolutePath(), null,
                    pathClassLoader);
            //获取 加载 插件 APK下的 DexPathList pathList
            Object pluginPathList = pathListField.get(pluginClassLoader);
            //获取插件APK中 DexPathList 对象内的 Element[] dexElements 数组
            Object[] pluginDexElements = (Object[]) dexElementsField.get(pluginPathList);

            //将插件APK中的 Element[] dexElements 合并到 宿主的 Element[] dexElements 中 (重新赋值新数组)
            Object[] newDexElements =
                    (Object[]) Array.newInstance(hostDexElements.getClass().getComponentType(),
                            hostDexElements.length + pluginDexElements.length);

            System.arraycopy(hostDexElements, 0, newDexElements, 0, hostDexElements.length);
            System.arraycopy(pluginDexElements, 0, newDexElements, hostDexElements.length, pluginDexElements.length);

            //将宿主的 dexElements 重新赋值为新数组
            // 相当于 把宿主中的 DexPathList对象内的  private Element[] dexElements  =  newDexElements
            dexElementsField.set(hostPathList,newDexElements);

        }catch (Exception exception){
            exception.printStackTrace();
        }
    }

    /**
     * 根据 插件 APK的 路径，创建出 适应插件的 Resources
     * @param context
     * @param apkPath
     * @return
     */
    public static Resources loadResource(Context context,String apkPath) {
        try {
            //1. 创建一个 AssetManager
            AssetManager assetManager = AssetManager.class.newInstance();
            //2.添加 插件的资源
            Method addAssetPathMethod = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPathMethod.invoke(assetManager, apkPath);
            //3.创建 Resources ,传入创建的 AssetManager
            Resources resources = context.getResources();
            return new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
