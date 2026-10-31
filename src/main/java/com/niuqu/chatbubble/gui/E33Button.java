//#if MC < 11903
package com.niuqu.chatbubble.gui;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

// 旧版按钮构建器
public final class E33Button {
    // 禁止外部实例化
    private E33Button() {}

    // 创建按钮构建器
    public static Builder builder(Text message, ButtonWidget.PressAction onPress) {
        return new Builder(message, onPress);
    }

    // 按钮构建器
    public static final class Builder {
        private final Text message;
        private final ButtonWidget.PressAction onPress;
        private int x;
        private int y;
        private int width;
        private int height;
        // 构建器初始化
        Builder(Text message, ButtonWidget.PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
            this.x = 0;
            this.y = 0;
            this.width = 200;
            this.height = 20;
        }
        // 设置位置与尺寸
        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }
        // 设置按钮位置
        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
        // 设置按钮大小
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }
        // 构建按钮控件
        public ButtonWidget build() {
            return new ButtonWidget(x, y, width, height, message, onPress);
        }
    }
}
//#endif
