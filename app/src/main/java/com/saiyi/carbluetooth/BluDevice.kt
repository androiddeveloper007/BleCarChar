package com.saiyi.carbluetooth

import java.io.Serializable

class BluDevice : Serializable {
    /*
	 * 设备名
	 */
    var name: String? = null
    /*
	 * MAC地址
	 */
    var address: String? = null
    /*
	 * 是否已添加到本地
	 */
    private var isBendi = "0"

    fun getIsBendi(): String {
        return isBendi
    }

    fun setIsBendi(isBendi: String) {
        this.isBendi = isBendi
    }

    fun isBendi(): String {
        return isBendi
    }

    fun setBendi(isBendi: String) {
        this.isBendi = isBendi
    }

}
