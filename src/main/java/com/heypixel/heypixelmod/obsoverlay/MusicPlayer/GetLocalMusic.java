package com.heypixel.heypixelmod.obsoverlay.MusicPlayer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地音乐文件扫描器
 * 扫描指定目录下的音乐文件并提取元数据信息
 */
public class GetLocalMusic {
    
    private static final String[] SUPPORTED_EXTENSIONS = {
        "mp3", "wav", "ogg", "flac", "aac", "m4a", "wma", "au", "aiff"
    };
    
    private ExecutorService executorService;
    private boolean isScanning = false;
    private ScanProgressListener progressListener;
    
    /**
     * 扫描进度监听器接口
     */
    public interface ScanProgressListener {
        void onProgressUpdate(int scannedFiles, int totalFiles, String currentFile);
        void onScanComplete(List<MusicInfo> musicList);
        void onError(String error);
    }
    
    public GetLocalMusic() {
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 扫描指定目录下的音乐文件
     * @param directory 要扫描的目录
     * @param recursive 是否递归扫描子目录
     * @return 音乐信息列表
     */
    public List<MusicInfo> scanMusicFiles(String directory, boolean recursive) {
        return scanMusicFiles(new File(directory), recursive);
    }
    
    /**
     * 扫描指定目录下的音乐文件
     * @param directory 要扫描的目录
     * @param recursive 是否递归扫描子目录
     * @return 音乐信息列表
     */
    public List<MusicInfo> scanMusicFiles(File directory, boolean recursive) {
        List<MusicInfo> musicList = new ArrayList<>();
        
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("[GetLocalMusic] 目录不存在或不是有效目录: " + directory.getAbsolutePath());
            return musicList;
        }
        
        System.out.println("[GetLocalMusic] 开始扫描目录: " + directory.getAbsolutePath());
        
        // 获取所有音频文件
        List<File> audioFiles = collectAudioFiles(directory, recursive);
        
        if (audioFiles.isEmpty()) {
            System.out.println("[GetLocalMusic] 未找到音频文件");
            return musicList;
        }
        
        System.out.println("[GetLocalMusic] 找到 " + audioFiles.size() + " 个音频文件，开始提取信息...");
        
        // 提取音乐信息
        for (int i = 0; i < audioFiles.size(); i++) {
            File audioFile = audioFiles.get(i);
            
            if (progressListener != null) {
                progressListener.onProgressUpdate(i + 1, audioFiles.size(), audioFile.getName());
            }
            
            MusicInfo musicInfo = extractMusicInfo(audioFile);
            if (musicInfo != null) {
                musicList.add(musicInfo);
            }
        }
        
        System.out.println("[GetLocalMusic] 扫描完成，成功处理 " + musicList.size() + " 个音频文件");
        
        if (progressListener != null) {
            progressListener.onScanComplete(musicList);
        }
        
        return musicList;
    }
    
    /**
     * 异步扫描音乐文件
     */
    public CompletableFuture<List<MusicInfo>> scanMusicFilesAsync(String directory, boolean recursive) {
        return CompletableFuture.supplyAsync(() -> {
            isScanning = true;
            try {
                return scanMusicFiles(directory, recursive);
            } catch (Exception e) {
                if (progressListener != null) {
                    progressListener.onError("扫描过程中发生错误: " + e.getMessage());
                }
                throw e;
            } finally {
                isScanning = false;
            }
        }, executorService);
    }
    
    /**
     * 收集指定目录下的所有音频文件
     */
    private List<File> collectAudioFiles(File directory, boolean recursive) {
        List<File> audioFiles = new ArrayList<>();
        
        File[] files = directory.listFiles();
        if (files == null) return audioFiles;
        
        for (File file : files) {
            if (file.isFile()) {
                if (isAudioFile(file)) {
                    audioFiles.add(file);
                }
            } else if (file.isDirectory() && recursive) {
                // 递归扫描子目录
                audioFiles.addAll(collectAudioFiles(file, recursive));
            }
        }
        
        return audioFiles;
    }
    
    /**
     * 检查文件是否为音频文件
     */
    private boolean isAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 提取音乐文件的元数据信息
     */
    private MusicInfo extractMusicInfo(File audioFile) {
        try {
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setFilePath(audioFile.getAbsolutePath());
            musicInfo.setLocal(true);
            
            // 从文件名提取基本信息
            String fileName = audioFile.getName();
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // 尝试解析文件名格式：艺术家 - 歌曲名
            if (nameWithoutExt.contains(" - ")) {
                String[] parts = nameWithoutExt.split(" - ", 2);
                musicInfo.setArtist(parts[0].trim());
                musicInfo.setTitle(parts[1].trim());
            } else {
                musicInfo.setTitle(nameWithoutExt);
                musicInfo.setArtist("Unknown Artist");
            }
            
            // 尝试获取音频文件信息
            try {
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(audioFile);
                if (fileFormat.getFrameLength() > 0 && fileFormat.getFormat().getFrameRate() > 0) {
                    long durationInSeconds = (long) (fileFormat.getFrameLength() / fileFormat.getFormat().getFrameRate());
                    musicInfo.setDuration(durationInSeconds * 1000); // 转换为毫秒
                }
                
                // 尝试从文件属性获取更多信息
                Map<String, Object> properties = fileFormat.properties();
                if (properties != null) {
                    // 提取标题
                    Object title = properties.get("title");
                    if (title != null && !title.toString().trim().isEmpty()) {
                        musicInfo.setTitle(title.toString().trim());
                    }
                    
                    // 提取艺术家
                    Object artist = properties.get("artist");
                    if (artist != null && !artist.toString().trim().isEmpty()) {
                        musicInfo.setArtist(artist.toString().trim());
                    }
                    
                    // 提取专辑
                    Object album = properties.get("album");
                    if (album != null && !album.toString().trim().isEmpty()) {
                        musicInfo.setAlbum(album.toString().trim());
                    }
                    
                    // 提取时长
                    Object duration = properties.get("duration");
                    if (duration != null) {
                        try {
                            long durationMicros = Long.parseLong(duration.toString());
                            musicInfo.setDuration(durationMicros / 1000); // 转换为毫秒
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
            } catch (Exception e) {
                // 如果无法读取音频信息，继续使用从文件名提取的信息
                System.out.println("[GetLocalMusic] 无法读取音频信息: " + audioFile.getName() + " - " + e.getMessage());
            }
            
            // 设置默认专辑（使用父目录名）
            if (musicInfo.getAlbum() == null || musicInfo.getAlbum().trim().isEmpty()) {
                String parentName = audioFile.getParentFile().getName();
                musicInfo.setAlbum(parentName);
            }
            
            return musicInfo;
            
        } catch (Exception e) {
            System.err.println("[GetLocalMusic] 处理文件失败: " + audioFile.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 扫描单个音乐文件
     */
    public MusicInfo scanSingleFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("[GetLocalMusic] 文件不存在: " + filePath);
            return null;
        }
        
        if (!isAudioFile(file)) {
            System.err.println("[GetLocalMusic] 不是支持的音频文件: " + filePath);
            return null;
        }
        
        return extractMusicInfo(file);
    }
    
    /**
     * 获取支持的音频格式
     */
    public static String[] getSupportedFormats() {
        return SUPPORTED_EXTENSIONS.clone();
    }
    
    /**
     * 获取支持的音频格式字符串
     */
    public static String getSupportedFormatsString() {
        return String.join(", ", SUPPORTED_EXTENSIONS);
    }
    
    /**
     * 检查指定路径是否为支持的音频文件
     */
    public static boolean isSupportedAudioFile(String filePath) {
        String fileName = new File(filePath).getName().toLowerCase();
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith("." + extension)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 快速扫描（只获取文件列表，不提取详细信息）
     */
    public List<String> quickScan(String directory, boolean recursive) {
        List<String> audioFilePaths = new ArrayList<>();
        File dir = new File(directory);
        
        if (!dir.exists() || !dir.isDirectory()) {
            return audioFilePaths;
        }
        
        List<File> audioFiles = collectAudioFiles(dir, recursive);
        for (File file : audioFiles) {
            audioFilePaths.add(file.getAbsolutePath());
        }
        
        return audioFilePaths;
    }
    
    /**
     * 根据关键词过滤音乐文件
     */
    public List<MusicInfo> filterMusic(List<MusicInfo> musicList, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(musicList);
        }
        
        List<MusicInfo> filteredList = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase().trim();
        
        for (MusicInfo music : musicList) {
            if (matchesKeyword(music, lowerKeyword)) {
                filteredList.add(music);
            }
        }
        
        return filteredList;
    }
    
    /**
     * 检查音乐是否匹配关键词
     */
    private boolean matchesKeyword(MusicInfo music, String keyword) {
        return (music.getTitle() != null && music.getTitle().toLowerCase().contains(keyword)) ||
               (music.getArtist() != null && music.getArtist().toLowerCase().contains(keyword)) ||
               (music.getAlbum() != null && music.getAlbum().toLowerCase().contains(keyword)) ||
               (music.getFilePath() != null && music.getFilePath().toLowerCase().contains(keyword));
    }
    
    /**
     * 设置扫描进度监听器
     */
    public void setProgressListener(ScanProgressListener listener) {
        this.progressListener = listener;
    }
    
    /**
     * 检查是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * 停止扫描
     */
    public void stopScanning() {
        isScanning = false;
    }
    
    /**
     * 关闭扫描器并清理资源
     */
    public void shutdown() {
        stopScanning();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 创建音乐库统计信息
     */
    public MusicLibraryStats createStats(List<MusicInfo> musicList) {
        return new MusicLibraryStats(musicList);
    }
    
    /**
     * 音乐库统计信息类
     */
    public static class MusicLibraryStats {
        public final int totalSongs;
        public final Set<String> artists;
        public final Set<String> albums;
        public final long totalDuration;
        public final Map<String, Integer> formatCount;
        
        public MusicLibraryStats(List<MusicInfo> musicList) {
            this.totalSongs = musicList.size();
            this.artists = new HashSet<>();
            this.albums = new HashSet<>();
            this.formatCount = new HashMap<>();
            
            long duration = 0;
            for (MusicInfo music : musicList) {
                if (music.getArtist() != null) artists.add(music.getArtist());
                if (music.getAlbum() != null) albums.add(music.getAlbum());
                duration += music.getDuration();
                
                // 统计格式
                if (music.getFilePath() != null) {
                    String extension = music.getFilePath().substring(music.getFilePath().lastIndexOf('.') + 1).toLowerCase();
                    formatCount.put(extension, formatCount.getOrDefault(extension, 0) + 1);
                }
            }
            this.totalDuration = duration;
        }
        
        @Override
        public String toString() {
            return String.format("音乐库统计:\n总歌曲数: %d\n艺术家数: %d\n专辑数: %d\n总时长: %d分钟\n格式统计: %s",
                totalSongs, artists.size(), albums.size(), totalDuration / 60000, formatCount);
        }
    }
}
