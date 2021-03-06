package ru.andr7e;

import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andrey on 01.03.16.
 */
public class MtkUtil
{
    final public static String PROJECT_CONFIG_PATH = "/system/data/misc/ProjectConfig.mk";

    final public static String CAMERA_INFO_PROC = "/proc/driver/camera_info";;

    public static ArrayList<String> getCameraList()
    {
        String fileName = "/system/lib/libcameracustom.so";

        String searchPattern = "SENSOR_DRVNAME_";

        ArrayList<String> cameraList  = new ArrayList<String>();

        ArrayList<String> mtkCameraList = BinaryDataHelper.getStringCapturedList(fileName, searchPattern, 100);

        for (String cameraModel : mtkCameraList)
        {
            String cameraName = cameraModel.toLowerCase();

            cameraList.add(cameraName);
        }

        return cameraList;
    }

    public static String getProcCameraInfo()
    {
        String path = CAMERA_INFO_PROC;

        return IOUtil.getFileText(path);
    }

    public static ArrayList<String> getProcCameraList()
    {
        // tets "CAM[1]: imx164mipiraw; CAM[2]: ov9760mipiraw;";  CAM[%d]:%s;
        String info = getProcCameraInfo();

        ArrayList<String> list = new ArrayList<String>();

        if ( ! info.isEmpty()) {

            String[] strList = info.split(";");

            for (String item : strList)
            {
                Pattern pattern = Pattern.compile("\\s*CAM\\[(\\d+)\\]:\\s*([0-9a-z]{1,})\\s*\\w*");

                Matcher m = pattern.matcher(item);

                if (m.matches()) {
                    String name = m.group(2);

                    list.add(name);
                }
            }
        }

        return list;
    }

    public static String[] getFields ()
    {
        String[]  fields = {
                "MODEL",
                "MTK_PLATFORM",
                "LCM_HEIGHT",
                "LCM_WIDTH",
                "MTK_BUILD_VERNO",
                "CUSTOM_KERNEL_LCM",
                "CUSTOM_KERNEL_TOUCHPANEL",
                "CUSTOM_HAL_IMGSENSOR",
                "CUSTOM_HAL_MAIN_IMGSENSOR",
                "CUSTOM_HAL_SUB_IMGSENSOR",
                "CUSTOM_KERNEL_MAIN_LENS",
                "CUSTOM_KERNEL_SOUND",
                "CUSTOM_KERNEL_ACCELEROMETER",
                "CUSTOM_KERNEL_ALSPS",
                "CUSTOM_KERNEL_MAGNETOMETER",
                "CUSTOM_MODEM",
                "COMMENTS"
        };

        return fields;
    }

    public static String convertFields (String mtkField)
    {
        if (mtkField.equals("CUSTOM_KERNEL_LCM"))
        {
            return InfoUtils.LCM;
        }
        if (mtkField.equals("CUSTOM_KERNEL_TOUCHPANEL"))
        {
            return InfoUtils.TOUCHPANEL;
        }
        if (mtkField.equals("CUSTOM_HAL_IMGSENSOR"))
        {
            return InfoUtils.CAMERA;
        }
        if (mtkField.equals("CUSTOM_HAL_MAIN_IMGSENSOR"))
        {
            return InfoUtils.CAMERA_BACK;
        }
        if (mtkField.equals("CUSTOM_HAL_SUB_IMGSENSOR"))
        {
            return InfoUtils.CAMERA_FRONT;
        }
        if (mtkField.equals("CUSTOM_KERNEL_MAIN_LENS"))
        {
            return InfoUtils.LENS;
        }
        if (mtkField.equals("CUSTOM_KERNEL_ACCELEROMETER"))
        {
            return InfoUtils.ACCELEROMETER;
        }
        if (mtkField.equals("CUSTOM_KERNEL_ALSPS"))
        {
            return InfoUtils.ALSPS;
        }
        if (mtkField.equals("CUSTOM_KERNEL_MAGNETOMETER"))
        {
            return InfoUtils.MAGNETOMETER;
        }
        if (mtkField.equals("CUSTOM_KERNEL_GYROSCOPE"))
        {
            return InfoUtils.GYROSCOPE;
        }
        if (mtkField.equals("CUSTOM_KERNEL_SOUND"))
        {
            return InfoUtils.SOUND;
        }
        if (mtkField.equals("CUSTOM_MODEM"))
        {
            return InfoUtils.MODEM;
        }
        if (mtkField.equals("MTK_PLATFORM"))
        {
            return InfoUtils.PLATFORM;
        }
        if (mtkField.equals("MTK_BUILD_VERNO"))
        {
            return InfoUtils.VERSION;
        }

        return mtkField;
    }

    public static String getProjectResolution (HashMap<String,String> hash)
    {
        String h = hash.get("LCM_HEIGHT");
        String w = hash.get("LCM_WIDTH");

        return h + "x" + w;
    }

    public static String getProjectConfigText()
    {
        String fileName = PROJECT_CONFIG_PATH;

        String[] allowKeys = getFields();

        return IOUtil.getFileText(fileName);
    }

    public static HashMap<String,String> getProjectDriversHash()
    {
        HashMap<String,String> hash = new HashMap<String,String>();

        String[] allowKeys = getFields();

        String text = getProjectConfigText();

        String[] lines = text.split("\n");

        for (String line : lines)
        {
            if (line.isEmpty()) continue;

            if (line.charAt(0) != '#')
            {
                if (line.startsWith("CUSTOM")||
                        line.startsWith("LCM") ||
                    line.startsWith("MTK_"))
                {
                    String[] strList = line.split("=");

                    if (strList.length >= 2)
                    {
                        String key   = strList[0].trim();
                        String value = strList[1];

                        // ignore comment
                        int pos = value.indexOf("#");

                        if (pos != -1)
                        {
                            value = value.substring(0, pos);
                        }

                        //System.out.println(key);
                        //System.out.println(value);

                        // contains key
                        for (String allowKey : allowKeys)
                        {
                            if (key.contains(allowKey))
                            {
                                String convKey = convertFields (key);

                                String[] valueList = value.trim().split(" ");

                                String res = TextUtils.join("\n", valueList);

                                hash.put(convKey, res);
                                break;
                            }
                        }
                    }
                }
            }
        }

        // fix
        if ( ! hash.isEmpty())
        {
            String resolution = MtkUtil.getProjectResolution(hash);

            hash.put(InfoUtils.RESOLUTION, resolution);
        }

        return hash;
    }

    public static ArrayList< Pair<String, String> > makePartitionsList(String text, boolean swapAddress)
    {
        ArrayList< Pair<String, String> > objList = new ArrayList< Pair<String, String> >();

        if (text.isEmpty()) return objList;

        ArrayList<String> list = new ArrayList<String>();

        String[] lines = text.split("\n");

        int i = 0;
        for (String line : lines)
        {
            if (i == 0)
            {
                list.add(line);

                InfoList.addItem(objList, "Name", "Start | Size");
            }
            else
            {
                System.out.println(line);

                if ( ! line.contains("0x")) continue;

                line = line.replace("\t", " ");
                line = line.replaceAll("\\s+", " ");

                System.out.println(line);

                String[] items = line.split(" ");

                if (items.length >= 3)
                {
                    String key = items[0];

                    int startIndex = 1;
                    int sizeIndex  = 2;

                    if (swapAddress)
                    {
                        startIndex = 2;
                        sizeIndex  = 1;
                    }

                    String start = items[startIndex];
                    String size  = items[sizeIndex];
                    String value = start + "\n" + size;

                    value = value.replace("0x0000", "0x");

                    InfoList.addItem(objList, key, value);
                }
            }

            i++;
        }

        return objList;
    }
}
