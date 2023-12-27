package com.bummon.service;

/**
 * @author Bummon
 * @description
 * @date 2023-12-27 17:04
 */
public interface PushService {
    /**
     * @param sourceFilePath mp4本地路径
     * @param pushAddress    推送地址
     * @return
     * @date 2023-12-27 17:06
     * @author Bummon
     * @description 推送MP4
     */
    void pushMp4(String sourceFilePath, String pushAddress) throws Exception;

}
