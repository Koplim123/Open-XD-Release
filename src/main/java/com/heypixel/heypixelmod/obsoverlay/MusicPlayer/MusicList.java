package com.heypixel.heypixelmod.obsoverlay.MusicPlayer;

import java.util.*;

/**
 * 音乐列表管理器
 * 管理播放列表、播放模式、历史记录等功能
 */
public class MusicList {
    
    private List<MusicInfo> playlist;
    private List<MusicInfo> history;
    private int currentIndex = -1;
    private PlayMode playMode = PlayMode.SEQUENTIAL;
    private Random random = new Random();
    private int maxHistorySize = 100;
    
    /**
     * 播放模式枚举
     */
    public enum PlayMode {
        SEQUENTIAL,    // 顺序播放
        LOOP_ALL,      // 循环播放全部
        LOOP_SINGLE,   // 单曲循环
        RANDOM,        // 随机播放
        RANDOM_NO_REPEAT // 随机播放（不重复）
    }
    
    public MusicList() {
        this.playlist = new ArrayList<>();
        this.history = new ArrayList<>();
    }
    
    /**
     * 添加音乐到播放列表
     */
    public void addMusic(MusicInfo musicInfo) {
        if (musicInfo != null && !playlist.contains(musicInfo)) {
            playlist.add(musicInfo);
            System.out.println("[MusicList] 添加音乐: " + musicInfo.getDisplayName());
        }
    }
    
    /**
     * 批量添加音乐
     */
    public void addMusicList(List<MusicInfo> musicList) {
        if (musicList != null) {
            for (MusicInfo music : musicList) {
                addMusic(music);
            }
        }
    }
    
    /**
     * 移除音乐
     */
    public boolean removeMusic(MusicInfo musicInfo) {
        if (musicInfo != null && playlist.contains(musicInfo)) {
            int index = playlist.indexOf(musicInfo);
            playlist.remove(musicInfo);
            
            // 调整当前索引
            if (currentIndex > index) {
                currentIndex--;
            } else if (currentIndex == index) {
                currentIndex = -1; // 当前播放的歌曲被删除
            }
            
            System.out.println("[MusicList] 移除音乐: " + musicInfo.getDisplayName());
            return true;
        }
        return false;
    }
    
    /**
     * 移除指定索引的音乐
     */
    public boolean removeMusic(int index) {
        if (index >= 0 && index < playlist.size()) {
            MusicInfo music = playlist.get(index);
            return removeMusic(music);
        }
        return false;
    }
    
    /**
     * 清空播放列表
     */
    public void clear() {
        playlist.clear();
        currentIndex = -1;
        System.out.println("[MusicList] 清空播放列表");
    }
    
    /**
     * 获取当前音乐
     */
    public MusicInfo getCurrentMusic() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }
    
    /**
     * 获取下一首音乐
     */
    public MusicInfo getNextMusic() {
        if (playlist.isEmpty()) return null;
        
        int nextIndex = calculateNextIndex();
        if (nextIndex >= 0) {
            currentIndex = nextIndex;
            MusicInfo nextMusic = playlist.get(currentIndex);
            addToHistory(nextMusic);
            return nextMusic;
        }
        
        return null;
    }
    
    /**
     * 获取上一首音乐
     */
    public MusicInfo getPreviousMusic() {
        if (playlist.isEmpty()) return null;
        
        int prevIndex = calculatePreviousIndex();
        if (prevIndex >= 0) {
            currentIndex = prevIndex;
            return playlist.get(currentIndex);
        }
        
        return null;
    }
    
    /**
     * 跳转到指定音乐
     */
    public MusicInfo jumpToMusic(int index) {
        if (index >= 0 && index < playlist.size()) {
            currentIndex = index;
            MusicInfo music = playlist.get(currentIndex);
            addToHistory(music);
            return music;
        }
        return null;
    }
    
    /**
     * 跳转到指定音乐
     */
    public MusicInfo jumpToMusic(MusicInfo musicInfo) {
        if (musicInfo != null && playlist.contains(musicInfo)) {
            int index = playlist.indexOf(musicInfo);
            return jumpToMusic(index);
        }
        return null;
    }
    
    /**
     * 计算下一首音乐的索引
     */
    private int calculateNextIndex() {
        switch (playMode) {
            case SEQUENTIAL:
                return (currentIndex + 1 < playlist.size()) ? currentIndex + 1 : -1;
                
            case LOOP_ALL:
                return (currentIndex + 1) % playlist.size();
                
            case LOOP_SINGLE:
                return currentIndex;
                
            case RANDOM:
                return random.nextInt(playlist.size());
                
            case RANDOM_NO_REPEAT:
                return getRandomNoRepeatIndex();
                
            default:
                return -1;
        }
    }
    
    /**
     * 计算上一首音乐的索引
     */
    private int calculatePreviousIndex() {
        switch (playMode) {
            case SEQUENTIAL:
                return (currentIndex - 1 >= 0) ? currentIndex - 1 : -1;
                
            case LOOP_ALL:
                return (currentIndex - 1 + playlist.size()) % playlist.size();
                
            case LOOP_SINGLE:
                return currentIndex;
                
            case RANDOM:
            case RANDOM_NO_REPEAT:
                // 从历史记录中获取上一首
                return getFromHistory();
                
            default:
                return -1;
        }
    }
    
    /**
     * 获取随机不重复的索引
     */
    private int getRandomNoRepeatIndex() {
        if (playlist.size() <= 1) {
            return 0;
        }
        
        int nextIndex;
        do {
            nextIndex = random.nextInt(playlist.size());
        } while (nextIndex == currentIndex);
        
        return nextIndex;
    }
    
    /**
     * 从历史记录获取上一首
     */
    private int getFromHistory() {
        if (history.size() >= 2) {
            MusicInfo prevMusic = history.get(history.size() - 2);
            return playlist.indexOf(prevMusic);
        }
        return -1;
    }
    
    /**
     * 添加到播放历史
     */
    private void addToHistory(MusicInfo musicInfo) {
        if (musicInfo == null) return;
        
        // 避免重复添加相同的音乐
        if (!history.isEmpty() && history.get(history.size() - 1).equals(musicInfo)) {
            return;
        }
        
        history.add(musicInfo);
        
        // 限制历史记录大小
        while (history.size() > maxHistorySize) {
            history.remove(0);
        }
    }
    
    /**
     * 打乱播放列表
     */
    public void shuffle() {
        if (playlist.size() > 1) {
            Collections.shuffle(playlist, random);
            currentIndex = -1; // 重置当前索引
            System.out.println("[MusicList] 播放列表已打乱");
        }
    }
    
    /**
     * 排序播放列表（按标题）
     */
    public void sortByTitle() {
        playlist.sort((a, b) -> {
            String titleA = a.getTitle() != null ? a.getTitle() : "";
            String titleB = b.getTitle() != null ? b.getTitle() : "";
            return titleA.compareToIgnoreCase(titleB);
        });
        currentIndex = -1;
        System.out.println("[MusicList] 按标题排序完成");
    }
    
    /**
     * 排序播放列表（按艺术家）
     */
    public void sortByArtist() {
        playlist.sort((a, b) -> {
            String artistA = a.getArtist() != null ? a.getArtist() : "";
            String artistB = b.getArtist() != null ? b.getArtist() : "";
            return artistA.compareToIgnoreCase(artistB);
        });
        currentIndex = -1;
        System.out.println("[MusicList] 按艺术家排序完成");
    }
    
    // Getters and Setters
    public List<MusicInfo> getPlaylist() {
        return new ArrayList<>(playlist);
    }
    
    public List<MusicInfo> getHistory() {
        return new ArrayList<>(history);
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }
    
    public void setCurrentIndex(int index) {
        if (index >= -1 && index < playlist.size()) {
            this.currentIndex = index;
        }
    }
    
    public PlayMode getPlayMode() {
        return playMode;
    }
    
    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
        System.out.println("[MusicList] 播放模式设置为: " + playMode);
    }
    
    public int getSize() {
        return playlist.size();
    }
    
    public boolean isEmpty() {
        return playlist.isEmpty();
    }
    
    public MusicInfo getMusicAt(int index) {
        if (index >= 0 && index < playlist.size()) {
            return playlist.get(index);
        }
        return null;
    }
    
    public int getMaxHistorySize() {
        return maxHistorySize;
    }
    
    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = Math.max(1, maxHistorySize);
    }
    
    /**
     * 查找音乐
     */
    public List<MusicInfo> searchMusic(String keyword) {
        List<MusicInfo> results = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }
        
        String lowerKeyword = keyword.toLowerCase().trim();
        for (MusicInfo music : playlist) {
            if (matchesKeyword(music, lowerKeyword)) {
                results.add(music);
            }
        }
        
        return results;
    }
    
    /**
     * 检查音乐是否匹配关键词
     */
    private boolean matchesKeyword(MusicInfo music, String keyword) {
        return (music.getTitle() != null && music.getTitle().toLowerCase().contains(keyword)) ||
               (music.getArtist() != null && music.getArtist().toLowerCase().contains(keyword)) ||
               (music.getAlbum() != null && music.getAlbum().toLowerCase().contains(keyword));
    }
}
