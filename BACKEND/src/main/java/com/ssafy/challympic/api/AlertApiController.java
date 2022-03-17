package com.ssafy.challympic.api;

import com.ssafy.challympic.api.Dto.AlertDto;
import com.ssafy.challympic.domain.Alert;
import com.ssafy.challympic.domain.Result;
import com.ssafy.challympic.domain.User;
import com.ssafy.challympic.service.AlertService;
import com.ssafy.challympic.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AlertApiController {

    private final AlertService alertService;
    private final UserService userService;

    @PostMapping("/alert")
    public Result saveAlert(@RequestBody AlertRequest request){
        Alert alert = new Alert();
        alert.setUser(userService.findUser(request.getUser_no()));
        alert.setAlert_content(request.alert_content);
        alertService.saveAlert(alert);
        return new Result(true, HttpStatus.OK.value());
    }

    @GetMapping("/alert/{userNo}")
    public Result getAlert(@PathVariable int userNo) {
        List<Alert> alerts = alertService.findAlertByUserNo(userNo);
        List<AlertDto> alertList = alerts.stream()
                .map(a -> new AlertDto(a.getUser().getUser_no(), a.getAlert_content(), a.isAlert_confirm(), a.getAlert_regDate()))
                .collect(Collectors.toList());
        return new Result(true, HttpStatus.OK.value(), alertList);
    }

    @Data
    static class AlertRequest {
        private int user_no;
        private String alert_content;
    }

}
