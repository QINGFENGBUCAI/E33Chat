//#if MC < 11903
package com.niuqu.chatbubble.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class E33Button {
    private E33Button() {}

    public static Builder builder(Text message, ButtonWidget.PressAction onPress) {
        return new Builder(message, onPress);
    }

    public static final class Builder {
        private final Text message;
        private final ButtonWidget.PressAction onPress;
        private int x;
        private int y;
        private int width;
        private int height;

        Builder(Text message, ButtonWidget.PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
            this.x = 0;
            this.y = 0;
            this.width = 200;
            this.height = 20;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ButtonWidget build() {
            return new ButtonWidget(x, y, width, height, message, onPress);
        }
    }
}
//#endif
