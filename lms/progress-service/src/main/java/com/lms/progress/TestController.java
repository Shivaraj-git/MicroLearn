package com.lms.progress;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** TEST ONLY. Remove before real use. */
@RestController
@RequestMapping("/test")
public class TestController {

    private final FailureSwitch failureSwitch;

    public TestController(FailureSwitch failureSwitch) { this.failureSwitch = failureSwitch; }

    @PostMapping("/fail-next")
    public Map<String, String> failNext() {
        failureSwitch.armFailNext();
        return Map.of("message", "The next grant-access will fail");
    }
}
