package rip.sunrise.warp;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class UnsafeUtils {
    private static Field getUnsafeField() {
        try {
            return Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Unsafe getUnsafe() {
        try {
            Field unsafeField = getUnsafeField();
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static long getAccessibleFlagOffset() {
        Field accessible = getUnsafeField();
        accessible.setAccessible(true);

        Field inaccessible = getUnsafeField();

        long offset = 0;
        Unsafe unsafe = getUnsafe();

        while (unsafe.getByte(accessible, offset) == unsafe.getByte(inaccessible, offset)) {
            offset++;
        }

        return offset;
    }

    public static void setAccessibleUnsafe(Field field, boolean state) {
        getUnsafe().putBoolean(field, getAccessibleFlagOffset(), state);
    }

    public static void setAccessibleUnsafe(Method method, boolean state) {
        getUnsafe().putBoolean(method, getAccessibleFlagOffset(), state);
    }
}
