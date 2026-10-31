package com.niuqu.chatbubble.chat;
import com.niuqu.chatbubble.store.ChatMessageStore.ChatMessage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 群组频道状态
public final class GroupChannelState {
    public static final String TAB_ALL = "#all";
    public static final String TAB_WORLD = "#world";
    public static final String TAB_SYSTEM = "#system";

    public static volatile boolean enabled;

    public static final Set<String> knownGroups = new LinkedHashSet<>();
    public static final Set<String> myGroups = new LinkedHashSet<>();

    private static String active = TAB_ALL;

    // 私有构造
    private GroupChannelState() {}

    // 是否支持群组
    public static boolean supported() {
        return enabled;
    }

    // 当前频道
    public static String active() {
        return enabled ? active : null;
    }

    // 设置当前频道
    public static boolean setActive(String tab) {
        if (tab == null) tab = TAB_ALL;
        if (tab.equals(active)) return false;
        active = tab;
        return true;
    }

    // 应用频道目录
    public static void applyDirectory(List<String> names, List<Integer> counts, List<String> mine) {
        knownGroups.clear();
        if (names != null) knownGroups.addAll(names);
        myGroups.clear();
        if (mine != null) myGroups.addAll(mine);
        if (active != null && !isPseudoTab(active) && !knownGroups.contains(active)) {
            active = TAB_ALL;
        }
    }

    // 重置状态
    public static void reset() {
        enabled = false;
        knownGroups.clear();
        myGroups.clear();
        active = TAB_ALL;
    }

    // 是否伪频道
    public static boolean isPseudoTab(String tab) {
        return TAB_WORLD.equals(tab) || TAB_SYSTEM.equals(tab);
    }

    // 取消息所属频道
    public static String channelOf(ChatMessage msg) {
        if (msg == null) return TAB_WORLD;
        if (msg.group() != null && !msg.group().isEmpty()) return msg.group();
        if (msg.isSystem()) return TAB_SYSTEM;
        return TAB_WORLD;
    }

    // 按频道过滤消息
    public static List<ChatMessage> filterMessages(List<ChatMessage> publicMessages, String activeTab) {
        if (activeTab == null) return publicMessages;
        if (TAB_ALL.equals(activeTab)) {
            List<ChatMessage> out = new ArrayList<>();
            for (ChatMessage m : publicMessages) {
                if (m.group() == null || m.group().isEmpty()) out.add(m);
            }
            return out;
        }
        List<ChatMessage> out = new ArrayList<>();
        for (ChatMessage m : publicMessages) {
            if (activeTab.equals(channelOf(m))) out.add(m);
        }
        return out;
    }
}
