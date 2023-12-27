package com.bummon.controller;

import com.bummon.service.PushService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Bummon
 * @description
 * @date 2023-12-27 16:38
 */
@RestController
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    @GetMapping("/pushMp4")
    public void pushMp4(String sourceFilePath, String pushAddress) throws Exception {
        pushService.pushMp4(sourceFilePath, pushAddress);
    }

}
