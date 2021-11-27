package my.noveldokusha.uiUtils

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPreference_Enum<T : Enum<T>>(val sharedPreferences: SharedPreferences, val defaultValue: T, val deserializer: (String) -> T)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        sharedPreferences.getString(property.name, null)?.let { kotlin.runCatching { deserializer(it) }.getOrNull() } ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        sharedPreferences.edit().putString(property.name, value.name).apply()
}

class SharedPreference_Int(val sharedPreferences: SharedPreferences, val defaultValue: Int)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getInt(property.name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
        sharedPreferences.edit().putInt(property.name, value).apply()
}

class SharedPreference_Float(val sharedPreferences: SharedPreferences, val defaultValue: Float)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getFloat(property.name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) =
        sharedPreferences.edit().putFloat(property.name, value).apply()
}

class SharedPreference_String(val sharedPreferences: SharedPreferences, val defaultValue: String)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, null) ?: defaultValue
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) =
        sharedPreferences.edit().putString(property.name, value).apply()
}

class SharedPreference_StringSet(val sharedPreferences: SharedPreferences, val defaultValue: Set<String>)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getStringSet(property.name, null)?.toSet() ?: defaultValue
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>?) =
        sharedPreferences.edit().putStringSet(property.name, value).apply()
}

class SharedPreference_Boolean(val sharedPreferences: SharedPreferences, val defaultValue: Boolean)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getBoolean(property.name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        sharedPreferences.edit().putBoolean(property.name, value).apply()
}