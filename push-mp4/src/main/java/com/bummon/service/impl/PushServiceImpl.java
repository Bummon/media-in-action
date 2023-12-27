package com.bummon.service.impl;

import com.bummon.service.PushService;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

/**
 * @author Bummon
 * @description
 * @date 2023-12-27 17:07
 */
@Slf4j
@Service
public class PushServiceImpl implements PushService {
    @Override
    public void pushMp4(String sourceFilePath, String pushAddress) throws Exception {
        //设置ffmpeg日志级别
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();

        // 实例化帧抓取器对象，将文件路径传入
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFilePath);
        long startTime = System.currentTimeMillis();

        log.info("开始初始化帧抓取器");

        // 初始化帧抓取器，例如数据结构（时间戳、编码器上下文、帧对象等），
        // 如果入参等于true，还会调用avformat_find_stream_info方法获取流的信息，放入AVFormatContext类型的成员变量oc中
        grabber.start(true);

        log.info("帧获取其初始化完毕，耗时[{}]毫秒", System.currentTimeMillis() - startTime);

        // grabber.start方法中，初始化的解码器信息存在放在grabber的成员变量oc中
        AVFormatContext avFormatContext = grabber.getFormatContext();

        //文件中有几个媒体流（视频流+音频流）
        int streamNum = avFormatContext.nb_streams();

        if (streamNum < 1) {
            log.error("文件中不存在媒体流");
            return;
        }

        //获取视频帧率
        int videoFrameRate = (int) grabber.getVideoFrameRate();
        log.info("视频帧率[{}]，视频时长[{}]，媒体流数量[{}]", videoFrameRate, avFormatContext.duration() / 1000000, streamNum);

        //遍历媒体流并检查其类型
        for (int i = 0; i < streamNum; i++) {
            AVStream avStream = avFormatContext.streams(i);
            AVCodecParameters avCodecParameters = avStream.codecpar();
            log.info("流索引[{}]，编码器类型[{}]，编码器ID[{}]", i, avCodecParameters.codec_type(), avCodecParameters.codec_id());
        }

        //视频宽度
        int width = grabber.getImageWidth();
        //视频高度
        int height = grabber.getImageHeight();
        //音频通道数量
        int audioChannels = grabber.getAudioChannels();

        log.info("视频宽度[{}]，视频高度[{}]，音频通道数[{}]", width, height, audioChannels);

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(pushAddress, width, height, audioChannels);

        //设置编码格式
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);

        //设置封装格式
        recorder.setFormat("flv");

        //设置两个关键帧中间的帧数
        recorder.setFrameRate(videoFrameRate);

        //设置音频通道数 与视频的通道数相等
        recorder.setAudioChannels(2);

        startTime = System.currentTimeMillis();

        // 初始化帧录制器，例如数据结构（音频流、视频流指针，编码器），
        // 调用av_guess_format方法，确定视频输出时的封装方式，
        // 媒体上下文对象的内存分配，
        // 编码器的各项参数设置
        recorder.start();

        log.info("帧录制器初始化完毕，耗时[{}]毫秒", System.currentTimeMillis() - startTime);

        Frame frame;

        startTime = System.currentTimeMillis();

        log.info("开始推流");

        long videoTS = 0;

        int videoFrameNum = 0;
        int audioFrameNum = 0;
        int dataFrameNum = 0;

        // 假设一秒钟15帧，那么两帧间隔就是(1000/15)毫秒
        int interVal = 1000 / videoFrameRate;

        // 发送完一帧后sleep的时间，不能完全等于(1000/frameRate)，不然会卡顿，
        // 要更小一些，这里取八分之一
        interVal /= 8;

        // 持续从视频源取帧
        while (null != (frame = grabber.grab())) {
            videoTS = 1000 * (System.currentTimeMillis() - startTime);

            //时间戳
            recorder.setTimestamp(videoTS);

            //有图像则视频帧+1
            if (null != frame.image) {
                videoFrameNum++;
            }

            //有声音则音频帧+1
            if (null != frame.samples) {
                audioFrameNum++;
            }

            //有数据则数据帧+1
            if (null != frame.data) {
                dataFrameNum++;
            }

            //将每帧取出并推送至SRS
            recorder.record(frame);

            // 停顿一下再推送
            Thread.sleep(interVal);
        }
        log.info("推送完成，视频帧[{}]，音频帧[{}]，数据帧[{}]，耗时[{}]毫秒", videoFrameNum, audioFrameNum, dataFrameNum, System.currentTimeMillis() - startTime);

        //关闭帧录制器
        recorder.close();

        //关闭帧抓取器
        grabber.close();
    }
}
