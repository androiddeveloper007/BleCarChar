package com.saiyi.carbluetooth

class Device {
    var name: String? = null
    var address: String? = null//mac地址
    var isSelected: Boolean = false

    constructor(name: String, selected: Boolean) {
        this.name = name
        this.isSelected = selected
    }

    constructor() {

    }
}
