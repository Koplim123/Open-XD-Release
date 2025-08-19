package com.heypixel.heypixelmod.obsoverlay.commands.impl;

import com.heypixel.heypixelmod.obsoverlay.commands.Command;
import com.heypixel.heypixelmod.obsoverlay.commands.CommandInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;


@CommandInfo(
        name = "help",
        description = "Get help",
        aliases = {"hp"}
)
public class CommandHelp extends Command {
    @Override
    public void onCommand(String[] args) {
        ChatUtils.addChatMessage("========Naven-XD========");
        ChatUtils.addChatMessage(".help .hp 当前帮助列表");
        ChatUtils.addChatMessage(".binds .bds 绑定按键列表");
        ChatUtils.addChatMessage(".hide .h 隐藏或显示指定功能");
        ChatUtils.addChatMessage(".bind .b 绑定按键");
        ChatUtils.addChatMessage(".language .lang 语言");
        ChatUtils.addChatMessage(".proxy .prox 代理设置");
        ChatUtils.addChatMessage(".toggle .t 功能切换");

    }

    @Override
    public String[] onTab(String[] args) {
        return new String[0];
    }
}
