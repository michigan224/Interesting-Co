package edu.umich.interestingco.rememri

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Pin(var pin_id: Int? = null,
            var owner_id: String? = null,
            var is_owned_by_user: Boolean? = null,
            image_url: String? = null,
            var timestamp: String? = null,
            var comments: ArrayList<Comment>? = null) {
    var imageUrl: String? by PinPropDelegate(imageUrl)
}

class PinPropDelegate private constructor ():
    ReadWriteProperty<Any?, String?> {
    private var _value: String? = null
        set(newValue) {
            newValue ?: run {
                field = null
                return
            }
            field = if (newValue == "null" || newValue.isEmpty()) null else newValue
        }

    constructor(initialValue: String?): this() { _value = initialValue }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = _value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        _value = value
    }
}