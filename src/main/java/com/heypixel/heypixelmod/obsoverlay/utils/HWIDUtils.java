package com.heypixel.heypixelmod.obsoverlay.utils;


import oshi.SystemInfo;
import oshi.hardware.*;

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
            // 1. 获取CPU处理器ID
            CentralProcessor processor = hal.getProcessor();
            if (processor != null && processor.getProcessorIdentifier() != null) {
                String processorID = processor.getProcessorIdentifier().getProcessorID();
                if (processorID != null && !processorID.isEmpty()) {
                    sb.append("CPU:").append(processorID).append(":");
                }
            }

            // 2. 获取硬盘序列号
            List<HWDiskStore> diskStores = hal.getDiskStores();
            if (diskStores != null && !diskStores.isEmpty()) {
                for (HWDiskStore disk : diskStores) {
                    String diskSerial = disk.getSerial();
                    if (diskSerial != null && !diskSerial.isEmpty() && !"Unknown".equalsIgnoreCase(diskSerial)) {
                        sb.append("DISK:").append(diskSerial).append(":");
                        break; // 只取第一个有效硬盘序列号
                    }
                }
            }

            // 3. 获取主板序列号和主板型号
            ComputerSystem computerSystem = hal.getComputerSystem();
            Baseboard baseboard = computerSystem.getBaseboard();
            
            // 主板序列号
            String boardSerial = baseboard.getSerialNumber();
            if (boardSerial != null && !boardSerial.isEmpty() && !"Unknown".equalsIgnoreCase(boardSerial)) {
                sb.append("MB_SERIAL:").append(boardSerial).append(":");
            }
            
            // 主板型号
            String boardModel = baseboard.getModel();
            if (boardModel != null && !boardModel.isEmpty() && !"Unknown".equalsIgnoreCase(boardModel)) {
                sb.append("MB_MODEL:").append(boardModel).append(":");
            }

        } catch (Exception e) {
            System.err.println("Error fetching hardware information: " + e.getMessage());
        }

        if (sb.length() == 0) {
            sb.append("Fallback:").append(si.getOperatingSystem().getNetworkParams().getHostName());
        }

        String fullHash = generateHash(sb.toString());
        // 截取前25个字符作为HWID
        cachedHWID = fullHash.length() > 25 ? fullHash.substring(0, 25) : fullHash;
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