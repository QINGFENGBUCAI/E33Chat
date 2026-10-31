package com.niuqu.chatbubble;
import com.niuqu.chatbubble.mixin.ScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

// 界面接口版本适配
public final class GuiCompat {
    // 禁止外部实例化
    private GuiCompat() {}

    // 创建按钮控件
    public static ButtonWidget button(Text message, ButtonWidget.PressAction action, int x, int y, int w, int h) {
        //#if MC >= 11903
        return ButtonWidget.builder(message, action).dimensions(x, y, w, h).build();
        //#else
        //$$ return new ButtonWidget(x, y, w, h, message, action);
        //#endif
    }

    // 创建资源标识
    public static Identifier id(String namespace, String path) {
        //#if MC >= 12000
        return Identifier.of(namespace, path);
        //#else
        //$$ return new Identifier(namespace, path);
        //#endif
    }

    // 添加可绘制子元素
    public static <T extends ClickableWidget> T addDrawableChild(Screen screen, T widget) {
        if (screen == null || widget == null) return widget;
        //#if MC >= 11900
        try {
            return ((ScreenAccessor) screen).e33chat$addDrawableChild(widget);
        } catch (Throwable ignored) {
        }
        //#endif
        if (invokeScreenMethod(screen, "addDrawableChild", widget)) return widget;
        if (invokeScreenMethod(screen, "addSelectableChild", widget)) return widget;
        if (invokeScreenMethod(screen, "addButton", widget)) return widget;
        invokeScreenMethod(screen, "addChild", widget);
        return widget;
    }

    // 清空屏幕子元素
    public static void clearChildren(Screen screen) {
        if (screen == null) return;
        //#if MC >= 11900
        try {
            ((ScreenAccessor) screen).e33chat$clearChildren();
            return;
        } catch (Throwable ignored) {
        }
        //#endif
        if (invokeNoArg(screen, "clearChildren")) return;
        clearListField(screen, "children");
        clearListField(screen, "buttons");
        clearListField(screen, "selectables");
        clearListField(screen, "drawables");
    }

    // 切换当前屏幕
    public static void setScreen(MinecraftClient client, Screen screen) {
        if (client == null) return;
        //#if MC >= 11700
        client.setScreen(screen);
        //#else
        //$$ client.openScreen(screen);
        //#endif
    }

    // 获取玩家名称
    public static String profileName(GameProfile profile) {
        if (profile == null) return null;
        //#if MC >= 12109
        return profile.name();
        //#else
        //$$ return profile.getName();
        //#endif
    }

    // 获取玩家标识
    public static UUID profileId(GameProfile profile) {
        if (profile == null) return null;
        //#if MC >= 12109
        return profile.id();
        //#else
        //$$ return profile.getId();
        //#endif
    }

    // 反射调用无参方法
    private static boolean invokeNoArg(Object target, String methodName) {
        try {
            Method m = findNoArgMethod(target.getClass(), methodName);
            if (m == null) return false;
            m.setAccessible(true);
            m.invoke(target);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    // 反射调用屏幕方法
    private static boolean invokeScreenMethod(Screen screen, String methodName, Object arg) {
        try {
            Method m = findOneArgMethod(screen.getClass(), methodName, arg.getClass());
            if (m == null) return false;
            m.setAccessible(true);
            m.invoke(screen, arg);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    // 查找无参方法
    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> cur = type;
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 0) {
                    return m;
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    // 查找单参方法
    private static Method findOneArgMethod(Class<?> type, String methodName, Class<?> argType) {
        Class<?> cur = type;
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (m.getName().equals(methodName) && params.length == 1 && params[0].isAssignableFrom(argType)) {
                    return m;
                }
            }
            cur = cur.getSuperclass();
        }
        return null;
    }

    // 清空列表字段
    private static void clearListField(Object target, String fieldName) {
        try {
            Field f = findField(target.getClass(), fieldName);
            if (f == null) return;
            f.setAccessible(true);
            Object value = f.get(target);
            if (value instanceof java.util.List) {
                ((java.util.List<?>) value).clear();
            }
        } catch (Exception ignored) {
        }
    }

    // 查找字段
    private static Field findField(Class<?> type, String fieldName) {
        Class<?> cur = type;
        while (cur != null) {
            try {
                return cur.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }
}
