package com.heypixel.heypixelmod.obsoverlay.MusicPlayer;

/**
 * 音乐播放器基础抽象类
 * 定义所有音乐播放器的基本接口和功能
 */
public abstract class MusicPlayer {
    
    protected MusicInfo currentMusic;
    protected boolean isPlaying = false;
    protected boolean isPaused = false;
    protected float volume = 1.0f;
    protected long currentPosition = 0;
    protected long duration = 0;
    
    /**
     * 播放音乐
     * @param musicInfo 音乐信息
     */
    public abstract void play(MusicInfo musicInfo);
    
    /**
     * 暂停播放
     */
    public abstract void pause();
    
    /**
     * 继续播放
     */
    public abstract void resume();
    
    /**
     * 停止播放
     */
    public abstract void stop();
    
    /**
     * 设置音量
     * @param volume 音量值 (0.0 - 1.0)
     */
    public abstract void setVolume(float volume);
    
    /**
     * 跳转到指定位置
     * @param position 位置（毫秒）
     */
    public abstract void seekTo(long position);
    
    /**
     * 获取当前播放状态
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * 获取当前暂停状态
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * 获取当前音乐信息
     */
    public MusicInfo getCurrentMusic() {
        return currentMusic;
    }
    
    /**
     * 获取当前播放位置
     */
    public long getCurrentPosition() {
        return currentPosition;
    }
    
    /**
     * 获取音乐总时长
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * 获取当前音量
     */
    public float getVolume() {
        return volume;
    }
}
