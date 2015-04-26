package fr.geobert.radis.data

public open class MType<T>(v: T) {
    private var internal: T = v

    public fun set(i: T) {
        internal = i
    }

    public fun get(): T {
        return internal
    }
}

public class MInt(i: Int) : MType<Int>(i)
public class MBoolean(b: Boolean) : MType<Boolean>(b)
public class MLong(l: Long) : MType<Long>(l)
public class MString(s: String) : MType<String>(s)

