package com.ssafy.challympic.api;

import com.ssafy.challympic.api.Dto.ChallengeDto;
import com.ssafy.challympic.api.Dto.SubscriptionDto;
import com.ssafy.challympic.api.Dto.UserDto;
import com.ssafy.challympic.domain.*;
import com.ssafy.challympic.domain.defaults.ChallengeAccess;
import com.ssafy.challympic.domain.defaults.ChallengeType;
import com.ssafy.challympic.service.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ChallengeApiController {

    private final ChallengeService challengeService;
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final TitleService titleService;
    private final TagService tagService;

    /**
     * 챌린지 목록
     */
    @GetMapping("/challenge")
    public Result challenges() {
        List<Challenge> findChallenges = challengeService.findChallenges();
        List<ChallengeDto> collect = findChallenges.stream()
                .map(c -> {
                    return new ChallengeDto(c);
                })
                .collect(Collectors.toList());
        return new Result(true, HttpStatus.OK.value(), collect);
    }

    private final AlertService alertService;

    @Data
    static class CreateChallengeRequest {
        private int user_no;
        private List<String> challengers;
        private Date challenge_end;
        private ChallengeType challenge_type;
        private String challenge_title;
        private String challenge_content;
        private String title_name;
    }


    /**
     * 챌린지 등록
     */
    @PostMapping("/challenge")
    public Result createChallenge(@RequestBody CreateChallengeRequest request) {

        ChallengeAccess challenge_access; // 권한
        List<Integer> challengers = new ArrayList<>(); // 초대된 인원
        // 챌린지 초대한 사람이 없으면 PUBLIC
        if(request.getChallengers().size() == 0){
            challenge_access = ChallengeAccess.PUBLIC;
        }
        // 챌린지 초대한 사람이 있으면 PRIVATE
        else{
            challenge_access = ChallengeAccess.PRIVATE;
            for(String str : request.getChallengers().subList(1, request.getChallengers().size())) {
                String user_nickname = str;
                User challengerUser = userService.findByNickname(user_nickname);
                if(challengerUser == null) return new Result(false, HttpStatus.BAD_REQUEST.value());
                challengers.add(challengerUser.getUser_no());
            }
        }

        // Title 저장
        Title title = new Title();
        title.setTitle_name(request.getTitle_name());
        if(title == null) return new Result(false, HttpStatus.FORBIDDEN.value());

        User user = userService.findUser(request.user_no);

        Challenge challenge = Challenge.createChallenge(
                user,
                request.getChallenge_end(),
                challenge_access,
                request.getChallenge_type(),
                request.getChallenge_title(),
                request.getChallenge_content()
        );

        // 챌린지 엔티티 등록
        challengeService.saveChallenge(challenge);


        // 챌린저 저장
        for(int cr : challengers) {
            Challenger challenger = new Challenger();
            challenger.setChallenge(challenge);
            User _challenger = userService.findUser(cr);
            challenger.setUser(_challenger);
            challengeService.saveChallengers(challenger);

            // 태그된사람 알림
            Alert alert = new Alert();
            alert.setUser(_challenger);
            alert.setAlert_content(user.getUser_nickname() + "님이 챌린지에 초대했습니다.");
            alertService.saveAlert(alert);

        }


        // 내용 파싱해서 태그 저장
        String content = request.challenge_content;
        List<String> tagContentList = new ArrayList<>();
        StringBuilder sb = null;

        // 챌린지 제목 저장
        tagContentList.add(request.challenge_title);
        tagService.saveTag("#" + request.challenge_title, true);
        for(char c : content.toCharArray()) {
            if(c == '#') {
                if(sb != null) {
                    tagContentList.add(sb.toString());
                    tagService.saveTag(sb.toString());
                }
                sb = new StringBuilder();
            }
            if(c == ' ' && sb != null) {
                tagService.saveTag(sb.toString());
                tagContentList.add(sb.toString());
                sb = null;
            }
            if(sb == null) continue;
            sb.append(c);
        }

        if(sb != null) {
            tagService.saveTag(sb.toString());
            tagContentList.add(sb.toString());
        }

        // 챌린지 태그 저장
        for(String s : tagContentList) {
            ChallengeTag challengeTag = new ChallengeTag();
            challengeTag.setTag(tagService.findTagByTagContent(s));
            challengeTag.setChallenge(challenge);
            challengeService.saveChallengeTag(challengeTag);
        }

        // title 등록
        title.setChallenge(challenge);
        titleService.saveTitles(title);

        List<Challenge> challengeList = challengeService.findChallengeByTitle(request.challenge_title);
        int _challenge_no = 0;
        for(Challenge chall :challengeList) {
            if(chall.getChallenge_end().after(new Date())) {
                _challenge_no = chall.getChallenge_no();
            }
        }
        return new Result(true, HttpStatus.OK.value(), new ChallengeResponse(_challenge_no));
    }

    @Data
    @AllArgsConstructor
    static class ChallengeResponse {
        private int challenge_no;
    }

    @PostMapping("/challenge/confirm")
    public Result ChallengeTitleCheck(@RequestBody ChallengeTitleCheckRequest request) {
        List<Challenge> challenges = challengeService.findChallengeByTitle(request.getChallenge_title());

        if(challenges.size() != 0){
            return new Result(false, HttpStatus.FORBIDDEN.value());
        }

        for(Challenge c : challenges) {
            if(c.getChallenge_end().after(new Date())){
                return new Result(false, HttpStatus.FORBIDDEN.value());
            }
        }
        return new Result(true, HttpStatus.OK.value());
    }

    @Data
    static class ChallengeTitleCheckRequest {
        String challenge_title;
    }

    /**
     * 구독추가
     */
    @PostMapping("/challenge/{challengeNo}/subscribe/{userNo}")
    public Result addSubscription(@PathVariable int challengeNo, @PathVariable int userNo) {
        Challenge challenge = challengeService.findChallengeByChallengeNo(challengeNo);
        if(challenge == null) {
            return new Result(false, HttpStatus.BAD_REQUEST.value());
        }
        User user = userService.findUser(userNo);

        Subscription findSubscription = subscriptionService.findSubscriptionByChallengeAndUser(challengeNo, userNo);
        if(findSubscription == null){
            subscriptionService.saveSubscription(Subscription.setSubscription(challenge, user));
            List<Subscription> subscriptionList = subscriptionService.findSubscriptionByUser(userNo);
            List<SubscriptionDto> subscriptions = new ArrayList<>();
            if(!subscriptionList.isEmpty()){
                subscriptions = subscriptionList.stream()
                        .map(s -> new SubscriptionDto(s))
                        .collect(Collectors.toList());
            }
            return new Result(true, HttpStatus.OK.value(), subscriptions);
        }else{
            subscriptionService.deleteSubscription(findSubscription);
            List<Subscription> subscriptionList = subscriptionService.findSubscriptionByUser(userNo);
            List<SubscriptionDto> subscriptions = new ArrayList<>();
            if(!subscriptionList.isEmpty()){
                subscriptions = subscriptionList.stream()
                        .map(s -> new SubscriptionDto(s))
                        .collect(Collectors.toList());
            }
            return new Result(true, HttpStatus.OK.value(), subscriptions);
        }
    }

    @GetMapping("/challenge/{challengeNo}")
    public Result getChallenge(@PathVariable int challengeNo) {
        Challenge challenge = challengeService.findChallengeByChallengeNo(challengeNo);
        if(challenge == null) return new Result(false, HttpStatus.BAD_REQUEST.value());
        else {
            List<Challenger> challenger = challengeService.getChallengerByChallengeNo(challengeNo);
            List<UserDto> challengers = new ArrayList<>();
            if(!challenger.isEmpty()){
                challengers = challenger.stream()
                        .map(cs -> {
                            User user = userService.findUser(cs.getUser().getUser_no());
                            return new UserDto(user);
                        }).collect(Collectors.toList());
            }
            ChallengeDto challengeResponse = new ChallengeDto(challenge, challengers);
            return new Result(true, HttpStatus.OK.value(), challengeResponse);
        }
    }

    /**
     *  구독 여부 확인
     * */
    @GetMapping("/challenge/{challengeNo}/subscribe/{userNo}")
    public Result isSubscription(@PathVariable int challengeNo, @PathVariable int userNo){
        Subscription subscription = null;

        subscription = subscriptionService.findSubscriptionByChallengeAndUser(challengeNo, userNo);

        if(subscription != null){
            return new Result(true, HttpStatus.OK.value());
        }
        return new Result(false, HttpStatus.OK.value());
    }


    /**
     * 구독 취소
     */
    @DeleteMapping("/challenge/{challengeNo}/subscribe/{userNo}")
    public Result removeSubscription(@PathVariable int challengeNo, @PathVariable int userNo) {
        Challenge challenge = challengeService.findChallengeByChallengeNo(challengeNo);
        if(challenge == null) {
            return new Result(false, HttpStatus.BAD_REQUEST.value());
        }
        User user = userService.findUser(userNo);

        subscriptionService.deleteSubscription(Subscription.setSubscription(challenge, user));
        return new Result(true, HttpStatus.OK.value());
    }

    /**
     * 구독 취소
     */
    @GetMapping("/subscribe/{userNo}")
    public Result getSubscription(@PathVariable int userNo) {
        List<Subscription> subscriptionByUser = subscriptionService.findSubscriptionByUser(userNo);
        List<SubscriptionDto> subscriptions = new ArrayList<>();
        if(!subscriptionByUser.isEmpty()){
            subscriptions = subscriptionByUser.stream()
                    .map(s -> new SubscriptionDto(s))
                    .collect(Collectors.toList());
        }
        return new Result(true, HttpStatus.OK.value(), subscriptions);
    }
}