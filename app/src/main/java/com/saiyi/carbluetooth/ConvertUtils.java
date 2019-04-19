package com.saiyi.carbluetooth;

/**
 * Created by YGP on 2018/1/7.
 */

public class ConvertUtils {
    public static boolean checkXORData(String data) {
        try {
            byte[] dataBytes = hexStringToBytes(data);
            byte result=dataBytes[0];
            for (int i = 1; i < dataBytes.length-1;i++) {
                result ^= dataBytes[i];
            }
            if (result == dataBytes[dataBytes.length-1]) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getXORData(byte[] datas) {
        try {
            byte[] dataXOR = new byte[datas.length+1];
            byte result=datas[0];
            dataXOR[0] = datas[0];
            for (int i = 1; i < datas.length;i++) {
                dataXOR[i] = datas[i];
                result ^= datas[i];
            }
            dataXOR[dataXOR.length-1] = result;
            return dataXOR;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
