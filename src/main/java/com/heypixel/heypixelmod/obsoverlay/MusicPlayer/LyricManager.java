package com.heypixel.heypixelmod.obsoverlay.MusicPlayer;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歌词管理器
 * 支持LRC格式歌词解析、时间同步和显示
 */
public class LyricManager {
    
    private List<LyricLine> lyrics;
    private int currentLineIndex = -1;
    private boolean isEnabled = true;
    private String currentLyricFile;
    
    // LRC时间格式正则表达式 [mm:ss.xx]
    private static final Pattern LRC_TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");
    // 扩展LRC格式 [mm:ss:xx]
    private static final Pattern LRC_TIME_PATTERN_ALT = Pattern.compile("\\[(\\d{2}):(\\d{2}):(\\d{2})\\](.*)");
    
    public LyricManager() {
        this.lyrics = new ArrayList<>();
    }
    
    /**
     * 加载LRC歌词文件
     */
    public boolean loadLyricFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("[LyricManager] 歌词文件不存在: " + filePath);
                return false;
            }
            
            lyrics.clear();
            currentLineIndex = -1;
            currentLyricFile = filePath;
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String line;
            
            while ((line = reader.readLine()) != null) {
                parseLyricLine(line.trim());
            }
            reader.close();
            
            // 按时间排序
            lyrics.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
            
            System.out.println("[LyricManager] 成功加载歌词，共" + lyrics.size() + "行");
            return true;
            
        } catch (IOException e) {
            System.err.println("[LyricManager] 读取歌词文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 解析单行歌词
     */
    private void parseLyricLine(String line) {
        if (line.isEmpty()) return;
        
        // 尝试匹配标准LRC格式 [mm:ss.xx]
        Matcher matcher = LRC_TIME_PATTERN.matcher(line);
        if (matcher.matches()) {
            long timestamp = parseTimeToMillis(
                Integer.parseInt(matcher.group(1)), // 分钟
                Integer.parseInt(matcher.group(2)), // 秒
                Integer.parseInt(matcher.group(3))  // 厘秒
            );
            String text = matcher.group(4).trim();
            lyrics.add(new LyricLine(timestamp, text));
            return;
        }
        
        // 尝试匹配替代格式 [mm:ss:xx]
        matcher = LRC_TIME_PATTERN_ALT.matcher(line);
        if (matcher.matches()) {
            long timestamp = parseTimeToMillis(
                Integer.parseInt(matcher.group(1)), // 分钟
                Integer.parseInt(matcher.group(2)), // 秒
                Integer.parseInt(matcher.group(3))  // 厘秒
            );
            String text = matcher.group(4).trim();
            lyrics.add(new LyricLine(timestamp, text));
            return;
        }
        
        // 处理标签信息（如：[ar:艺术家][ti:标题]等）
        if (line.startsWith("[") && line.contains(":") && line.endsWith("]")) {
            // 这是元数据标签，暂时忽略
            return;
        }
    }
    
    /**
     * 将时间转换为毫秒
     */
    private long parseTimeToMillis(int minutes, int seconds, int centiseconds) {
        return (minutes * 60 + seconds) * 1000 + centiseconds * 10;
    }
    
    /**
     * 根据当前播放位置获取当前歌词
     */
    public String getCurrentLyric(long currentPosition) {
        if (!isEnabled || lyrics.isEmpty()) {
            return "";
        }
        
        // 找到当前时间对应的歌词行
        int newIndex = findCurrentLyricIndex(currentPosition);
        
        if (newIndex != currentLineIndex) {
            currentLineIndex = newIndex;
        }
        
        if (currentLineIndex >= 0 && currentLineIndex < lyrics.size()) {
            return lyrics.get(currentLineIndex).text;
        }
        
        return "";
    }
    
    /**
     * 获取下一行歌词
     */
    public String getNextLyric(long currentPosition) {
        if (!isEnabled || lyrics.isEmpty()) {
            return "";
        }
        
        int currentIndex = findCurrentLyricIndex(currentPosition);
        int nextIndex = currentIndex + 1;
        
        if (nextIndex >= 0 && nextIndex < lyrics.size()) {
            return lyrics.get(nextIndex).text;
        }
        
        return "";
    }
    
    /**
     * 获取上一行歌词
     */
    public String getPreviousLyric(long currentPosition) {
        if (!isEnabled || lyrics.isEmpty()) {
            return "";
        }
        
        int currentIndex = findCurrentLyricIndex(currentPosition);
        int prevIndex = currentIndex - 1;
        
        if (prevIndex >= 0 && prevIndex < lyrics.size()) {
            return lyrics.get(prevIndex).text;
        }
        
        return "";
    }
    
    /**
     * 查找当前时间对应的歌词索引
     */
    private int findCurrentLyricIndex(long currentPosition) {
        if (lyrics.isEmpty()) return -1;
        
        for (int i = lyrics.size() - 1; i >= 0; i--) {
            if (currentPosition >= lyrics.get(i).timestamp) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * 获取所有歌词行
     */
    public List<LyricLine> getAllLyrics() {
        return new ArrayList<>(lyrics);
    }
    
    /**
     * 获取歌词总行数
     */
    public int getLyricCount() {
        return lyrics.size();
    }
    
    /**
     * 清空歌词
     */
    public void clear() {
        lyrics.clear();
        currentLineIndex = -1;
        currentLyricFile = null;
    }
    
    /**
     * 设置是否启用歌词显示
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }
    
    /**
     * 检查歌词是否启用
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * 获取当前歌词文件路径
     */
    public String getCurrentLyricFile() {
        return currentLyricFile;
    }
    
    /**
     * 从音乐信息自动加载歌词
     */
    public boolean autoLoadLyric(MusicInfo musicInfo) {
        if (musicInfo == null) return false;
        
        // 尝试多种歌词文件路径
        String[] possiblePaths = generateLyricPaths(musicInfo);
        
        for (String path : possiblePaths) {
            if (loadLyricFile(path)) {
                return true;
            }
        }
        
        System.out.println("[LyricManager] 未找到歌词文件");
        return false;
    }
    
    /**
     * 生成可能的歌词文件路径
     */
    private String[] generateLyricPaths(MusicInfo musicInfo) {
        List<String> paths = new ArrayList<>();
        
        if (musicInfo.isLocal() && musicInfo.getFilePath() != null) {
            String audioPath = musicInfo.getFilePath();
            String basePath = audioPath.substring(0, audioPath.lastIndexOf('.'));
            
            // 同名LRC文件
            paths.add(basePath + ".lrc");
            paths.add(basePath + ".LRC");
            
            // 在lyrics子目录中查找
            File audioFile = new File(audioPath);
            String parentDir = audioFile.getParent();
            if (parentDir != null) {
                String fileName = audioFile.getName();
                String baseFileName = fileName.substring(0, fileName.lastIndexOf('.'));
                paths.add(parentDir + File.separator + "lyrics" + File.separator + baseFileName + ".lrc");
            }
        }
        
        // 如果有指定的歌词路径
        if (musicInfo.getLyricPath() != null) {
            paths.add(0, musicInfo.getLyricPath()); // 优先使用指定路径
        }
        
        return paths.toArray(new String[0]);
    }
    
    /**
     * 歌词行数据类
     */
    public static class LyricLine {
        public final long timestamp;    // 时间戳（毫秒）
        public final String text;       // 歌词文本
        
        public LyricLine(long timestamp, String text) {
            this.timestamp = timestamp;
            this.text = text;
        }
        
        @Override
        public String toString() {
            return String.format("[%02d:%02d.%02d] %s",
                timestamp / 60000,
                (timestamp % 60000) / 1000,
                (timestamp % 1000) / 10,
                text);
        }
    }
}
