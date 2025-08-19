package com.heypixel.heypixelmod.obsoverlay.utils;

import oshi.SystemInfo;
import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

public class HWIDUtils {

    private static String cachedHWID = null;

    private HWIDUtils() {
    }

    /**
     * 获取一个由多个硬件信息组合而成的唯一硬件ID。
     * 该方法会缓存结果，以避免重复执行昂贵的操作。
     * 生成的ID通过SHA-256哈希算法处理，保证了其固定长度、安全性和不可逆性。
     *
     * @return 唯一的硬件ID哈希字符串。
     */
    public static String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }

        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        StringBuilder sb = new StringBuilder();

        try {
            // 1. 获取主板序列号
            ComputerSystem computerSystem = hal.getComputerSystem();
            Baseboard baseboard = computerSystem.getBaseboard();
            String boardSerial = baseboard.getSerialNumber();
            if (boardSerial != null && !boardSerial.isEmpty() && !"Unknown".equalsIgnoreCase(boardSerial)) {
                sb.append("Motherboard:").append(boardSerial).append(":");
            }

            // 2. 获取 CPU 序列号或处理器 ID
            CentralProcessor processor = hal.getProcessor();
            if (processor != null && processor.getProcessorIdentifier() != null) {
                String processorID = processor.getProcessorIdentifier().getProcessorID();
                if (processorID != null && !processorID.isEmpty()) {
                    sb.append("CPU:").append(processorID).append(":");
                }
            }

            // 3. 获取所有网络接口的 MAC 地址并排序
            List<String> macAddresses = hal.getNetworkIFs().stream()
                    .map(NetworkIF::getMacaddr)
                    .filter(mac -> mac != null && !mac.isEmpty())
                    .sorted()
                    .collect(Collectors.toList());

            if (!macAddresses.isEmpty()) {
                sb.append("MAC:");
                for (String mac : macAddresses) {
                    sb.append(mac).append(":");
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching hardware information: " + e.getMessage());
        }

        if (sb.length() == 0) {
            sb.append("Fallback:").append(si.getOperatingSystem().getNetworkParams().getHostName());
        }

        cachedHWID = generateHash(sb.toString());
        return cachedHWID;
    }

    /**
     * 生成给定字符串的 SHA-256 哈希值。
     *
     * @param input 要哈希的字符串。
     * @return 哈希后的字符串，如果哈希失败则返回空字符串。
     */
    private static String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not found.");
            return "";
        }
    }
}