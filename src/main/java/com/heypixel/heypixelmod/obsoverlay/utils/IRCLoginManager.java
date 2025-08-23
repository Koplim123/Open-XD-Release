package com.heypixel.heypixelmod.obsoverlay.utils;

import by.radioegor146.nativeobfuscator.Native;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.Util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
@Native
public class IRCLoginManager {
    public static String username = "";
    public static String rank = "";
    public static int userId = -1;

    private static final String LOGIN_URL = "http://nxdirc.koplim.sbs/LoginRequestAPI.php";
    private static final String REGISTER_URL = "http://nxdirc.koplim.sbs/RegAccount.html";

    public static boolean login(String user, String pass) {
        try {
            URL url = new URL(LOGIN_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000); // 设置连接超时为5秒
            connection.setReadTimeout(5000);    // 设置读取超时为5秒
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            JsonObject jsonPayload = new JsonObject();
            jsonPayload.addProperty("username", user);
            jsonPayload.addProperty("password", pass);

            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                outputStream.write(jsonPayload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject responseObject = JsonParser.parseString(response.toString()).getAsJsonObject();
                int code = responseObject.get("code").getAsInt();

                if (code == 1) {
                    JsonObject data = responseObject.getAsJsonObject("data");
                    userId = data.has("user_id") ? data.get("user_id").getAsInt() : -1;
                    username = data.has("username") ? data.get("username").getAsString() : "";
                    rank = data.has("Rank") ? data.get("Rank").getAsString() : "";
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getUsername() {
        return username;
    }

    public static void openRegisterPage() {
        Screen currentScreen = Minecraft.getInstance().screen;
        Minecraft.getInstance().setScreen(new ConfirmLinkScreen((accepted) -> {
            if (accepted) {
                Util.getPlatform().openUri(REGISTER_URL);
            }
            Minecraft.getInstance().setScreen(currentScreen);
        }, REGISTER_URL, true));
    }
}