package com.niuqu.chatbubble.config;

import java.util.List;

public class ServerConfig {
    public boolean use_tpa;
    public boolean history_enabled;
    public List<String> chat_templates;
    public List<String> whisper_templates;
    public boolean template_debug;
    public boolean media_enabled;

    public Boolean media_auto_clean;

    public Boolean easy_bot_compat;

    public Boolean groups_enabled;

    public Integer group_max_count;
    public Integer group_max_members;
    public Boolean group_create_op_only;

    public static ServerConfig defaults() {
        ServerConfig c = new ServerConfig();
        c.use_tpa = false;
        c.history_enabled = false;
        c.chat_templates = List.of();
        c.whisper_templates = List.of();
        c.template_debug = false;
        c.media_enabled = true;
        c.media_auto_clean = true;
        c.easy_bot_compat = true;
        c.groups_enabled = true;
        return c;
    }
}
