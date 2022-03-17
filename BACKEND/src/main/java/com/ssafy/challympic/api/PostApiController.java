package com.ssafy.challympic.api;

import com.ssafy.challympic.api.Dto.CommentDto;
import com.ssafy.challympic.domain.*;
import com.ssafy.challympic.service.*;
import com.ssafy.challympic.util.S3Uploader;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@CrossOrigin("*")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;
    private final MediaService mediaService;
    private final ChallengeService challengeService;
    private final PostLikeService postLikeService;
    private final UserService userService;
    private final TagService tagService;
    private final FollowService followService;
    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final S3Uploader s3Uploader;

    @Data
    @AllArgsConstructor
    static class Result<T>{
        private boolean isSuccess;
        private int code;
        private T data;

        public Result(boolean isSuccess, int code) {
            this.isSuccess = isSuccess;
            this.code = code;
        }
    }


    // 프론트 단에서 전달받은 파일과 포스트 정보
    @Data
    @Getter @Setter
    @AllArgsConstructor
    static class PostRequest{
        private Integer user_no;
        private String post_content;
        private MultipartFile file;
    }


    @Data
    @AllArgsConstructor
    static class PostLikeUserDto{
        private int user_no;
        private String user_nickname;
        private String user_title;
        private int file_no;
        private String file_path;
        private String file_savedname;
        private Boolean isFollowing;

        public PostLikeUserDto(User user, Media media, boolean isFollowing) {
            this.user_no = user.getUser_no();
            this.user_nickname = user.getUser_nickname();
            this.user_title = user.getUser_title();
            if(media != null){
                this.file_no = media.getFile_no();
                this.file_path = media.getFile_path();
                this.file_savedname = media.getFile_savedname();
            }
            this.isFollowing = isFollowing;
        }
    }

    @Data
    @Setter @Getter
    static class PostDto{
        // 포스트 정보
        private int post_no;
        private String post_content;
        private int post_report;
        private Date post_regdate;
        private Date post_update;

        // 유저 타입
        private int user_no;
        private String user_nickname;
        private String user_title;
        private String user_profile;

        // 챌린지 타입
        private String challenge_type;
        private String challenge_name;
        private int challenge_no;

        // 미디어 정보
        private int file_no;
        private String file_path;
        private String file_savedname;

        // 좋아요 수
        private Integer LikeCnt;

        // 이 유저가 좋아요를 눌렀는지
        private boolean IsLike = false;

        // 댓글 리스트
        private List<CommentDto> commentList;
    }

    @Data
    static class ChallengePostRequest {
        private int user_no;
        private int challenge_no;
    }

    @Data
    @Setter @Getter
    static class CreateResult{
        private Media media;
        private Integer post_no;
    }

    @GetMapping("/main/recent/post")
    public Result getRecentPosts(@RequestParam(required = false) Integer userNo){
        // 최대 50개 가져오기
        List<Post> postList = postService.getRecentPostList(50);
        List<PostDto> collect = new ArrayList<>();

        log.info("userNo : " + userNo);

        for(Post post : postList){
            List<PostLike> postLikeList = postLikeService.getPostLikeListByPostNo(post.getPost_no());
            Challenge challenge = challengeService.findChallengeByChallengeNo(post.getChallenge_no());
            User user = post.getUser();

            log.info(post.getPost_content());

            // 기본 포스트 정보
            PostDto postDto = new PostDto();
            postDto.setPost_no(post.getPost_no());
            postDto.setPost_content(post.getPost_content());
            postDto.setPost_report(post.getPost_report());
            postDto.setPost_regdate(post.getPost_regdate());

            // 유저 타입
            postDto.setUser_no(user.getUser_no());
            postDto.setUser_nickname(user.getUser_nickname());
            postDto.setUser_title(user.getUser_title());
            if(user.getMedia() != null)
                postDto.setUser_profile(user.getMedia().getFile_path() + File.separator + user.getMedia().getFile_savedname());
            else
                postDto.setUser_profile(null);


            // 챌린지 타입
            postDto.setChallenge_type(challenge.getChallenge_type().name().toLowerCase());
            postDto.setChallenge_no(post.getChallenge_no());
            postDto.setChallenge_name(challengeService.findChallengeByChallengeNo(post.getChallenge_no()).getChallenge_title());

            // 미디어 정보
            postDto.setFile_no(post.getMedia().getFile_no());
            postDto.setFile_path(post.getMedia().getFile_path());
            postDto.setFile_savedname(post.getMedia().getFile_savedname());

            // 좋아요 수
            if(postLikeList.size() == 0){
                postDto.setLikeCnt(0);
            } else{
                postDto.setLikeCnt(postLikeList.size());
            }

            if(userNo != null) {
                boolean isLike = postService.getPostLikeByPostNoAndUserNo(post.getPost_no(), userNo);
                postDto.setIsLike(isLike);
                log.info("istLike : " + isLike);

                List<Comment> comments = commentService.findByPost(post.getPost_no());
                List<CommentDto> commentList = comments.stream()
                        .map(c -> {
                            boolean IsLiked = commentLikeService.findIsLikeByUser(userNo, c.getComment_no());
                            return new CommentDto(c, IsLiked);
                        })
                        .collect(Collectors.toList());
                postDto.setCommentList(commentList);
            } else {
                postDto.setCommentList(null);
            }

            collect.add(postDto);
        }

        log.info("size : "+collect.size());

        return new Result(true, HttpStatus.OK.value(), collect);
    }

    /**
     *  챌린지 번호로 포스트 가져오기(챌린지로 확인 예정)
     * */
    @PostMapping("/challenge/post")
    public Result list(@RequestBody ChallengePostRequest request){
        Result result = null;

        // 챌린지 정보
        Challenge challenge = challengeService.findChallengeByChallengeNo(request.getChallenge_no());
        if(challenge == null) return new Result(false, HttpStatus.BAD_REQUEST.value());
        String type = challenge.getChallenge_type().name().toLowerCase();
        // 포스트 리스트
        List<Post> postList = postService.getPostList(request.getChallenge_no());

        List<PostDto> collect = new ArrayList<>();

        for(Post post : postList){
            List<PostLike> postLikeList = postLikeService.getPostLikeListByPostNo(post.getPost_no());
            User user = post.getUser();

            PostDto postDto = new PostDto();
            postDto.setPost_no(post.getPost_no());
            postDto.setPost_content(post.getPost_content());
            postDto.setPost_report(post.getPost_report());
            postDto.setPost_regdate(post.getPost_regdate());

            // 유저 타입
            postDto.setUser_no(user.getUser_no());
            postDto.setUser_nickname(user.getUser_nickname());
            postDto.setUser_title(user.getUser_title());
            if(user.getMedia() != null)
                postDto.setUser_profile(user.getMedia().getFile_path() + File.separator + user.getMedia().getFile_savedname());
            else
                postDto.setUser_profile(null);


            // 챌린지 타입
            postDto.setChallenge_type(type);

            // 미디어 정보
            postDto.setFile_no(post.getMedia().getFile_no());
            postDto.setFile_path(post.getMedia().getFile_path());
            postDto.setFile_savedname(post.getMedia().getFile_savedname());

            // 좋아요 수
            if(postLikeList == null){
                postDto.setLikeCnt(0);
            } else{
                postDto.setLikeCnt(postLikeList.size());
            }

            boolean isLike = postService.getPostLikeByPostNoAndUserNo(post.getPost_no(), request.getUser_no());
            postDto.setIsLike(isLike);

            List<Comment> comments = commentService.findByPost(post.getPost_no());
            List<CommentDto> commentList = comments.stream()
                            .map(c -> {
                                boolean IsLiked = commentLikeService.findIsLikeByUser(request.user_no, c.getComment_no());
                                return new CommentDto(c, IsLiked);
                            })
                    .collect(Collectors.toList());
            postDto.setCommentList(commentList);

            collect.add(postDto);
        }

        if(postList != null){
            result = new Result(true, HttpStatus.OK.value(), collect);
        } else {
            result = new Result(false, HttpStatus.OK.value());
        }

        return result;
    }

    /** 
     *  해당 게시글을 좋아요한 유저의 목록(Complete)
     *      - 유저의 정보를 모두 가져올 수도 있음
     *      - 유저 번호와 유저 닉네임만 전달받을 수도 있음
     *      - 현재 번호만 가져옴
     * */
    @GetMapping("/post/{postNo}/like/{userNo}")
    public Result likeList(@PathVariable("postNo") int postNo, @PathVariable("userNo") int userNo){

        // PostLike에서 게시글이 post인 것 추출

        // 받는 쪽에서 길이 구해서 좋아요 수 출력
        List<PostLike> postLikeList = postLikeService.getPostLikeListByPostNo(postNo);

        // 좋아요 누른 유저 정보만 가져오기
        List<PostLikeUserDto> userList = new ArrayList<>();
        for(PostLike postLike : postLikeList){
            User user = userService.findUser(postLike.getUser_no());
            boolean follow = followService.isFollow(userNo, user.getUser_no());
            userList.add(new PostLikeUserDto(user, user.getMedia(), follow));
        }

        return new Result(true, HttpStatus.OK.value(), userList);
    }
    
    /** 
     *  포스트 등록하기(Complete)
     *          alter table post
     *          convert to char set utf8;
     *      - Chaalenge : 챌린지 이름으로 검색
     *      - Challenge : 해당 챌린지 사진/영상 포맷 가져오기
     *      - File명 프론트 단에서 거르기
     *      - 확장자 프론트에서 거를지 백에서 거를지 결정
     *      - File 도메인 변경 및 테이블 명 변경 -> Media, File이라는 io의 객체와 이름이 겹침
     * */
    @PostMapping("/challenge/{challengeNo}/post")
    public Result create(@PathVariable("challengeNo") int challengeNo, PostRequest postRequest) throws IOException {

        log.info("Create Post");

        // 플로우 시작
        Media media = null;
        MultipartFile files = null;
        Challenge challenge = challengeService.findChallengeByChallengeNo(challengeNo);

        // 챌린저 목록 가져옴
        List<Challenger> challengerList = challengeService.getChallengerByChallengeNo(challengeNo);
        User _user = userService.findUser(postRequest.getUser_no());
        boolean isChallenger = false;

        // 챌린저 목록이 지정되어 있거나 포스트 작성자가 챌린지 작성자인 경우
        if(!challengerList.isEmpty()){
            for(Challenger challenger : challengerList){
                if(challenger.getUser() == _user) {
                    isChallenger = true;
                    break;
                }
            }

            if(postRequest.getUser_no() == challenge.getUser().getUser_no())
                isChallenger = true;

            if(!isChallenger)
                return new Result(false, HttpStatus.OK.value());
        }


        try {

            files = postRequest.getFile();

            log.info(postRequest.getPost_content());

            // 확장자 체크
            String fileType = getFileType(files);

            if(fileType == null)
                // 지원하지 않는 확장자
                return new Result(false, HttpStatus.OK.value());
            
            if(!fileType.equals(challenge.getChallenge_type().name())){
                // 챌린지와 확장자 명이 다름
                return new Result(false, HttpStatus.OK.value());
            }

            // png/jpg, mp4 <- 확장자
            media = s3Uploader.upload(files, fileType.toLowerCase(), "media");

            if(media == null) {
                // AWS S3 업로드 실패
                return new Result(false, HttpStatus.OK.value());
            }

            int file_no = mediaService.saveMedia(media);

            // 본문 텍스트 파싱
            String content = postRequest.getPost_content();
            String[] splitSharp = content.split(" ");

            for(String str : splitSharp){
                if(str.startsWith("#")){
                    // #을 분리하고 태그명만 추출
                    tagService.saveTag(str);
                }
            }

            // 포스트 등록
            Post post = new Post();

            for(String str : splitSharp) {
                if(str.startsWith("#")) {
                    PostTag postTag = new PostTag();
                    postTag.setPost(post);
                    Tag tag = tagService.findTagByTagContent(str);
                    postTag.setTag(tag);
                    tagService.savePostTag(postTag);
                }
            }

            // 수정 필요
            post.setChallenge_no(challengeNo);  // 포스트가 속한 챌린지 정보

            post.setUser(userService.findUser(postRequest.getUser_no()));  // 포스트 작성 유저 정보
            post.setPost_content(postRequest.getPost_content());  // 포스트 본문
            post.setPost_report(0); // 신고횟수 초기값 0
            post.setMedia(media);

            int postId = postService.save(post);

            // 태그한 사람 알림
            for(String str : splitSharp) {
                if(str.startsWith("@")) {
                    // 태그 당한 닉네임
                    String user_nickname = str.substring(1);

                    Alert alert = new Alert();
                    User user = userService.findByNickname(user_nickname);
                    // 태그 당한 유저
                    if(user == null) {
                        continue;
                    }
                    alert.setUser(user);
                    User writer = userService.findUser(postRequest.getUser_no());
                    alert.setAlert_content(writer.getUser_nickname() + "님이 태그했습니다.");
                    alertService.saveAlert(alert);
                }
            }

            if(postId != -1) {
                CreateResult cr = new CreateResult();
                cr.setPost_no(postId);
                cr.setMedia(media);
                return new Result(true, HttpStatus.OK.value(), cr);
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return new Result(false, HttpStatus.OK.value());
    }
    
    /**
     *  기존 포스트를  수정하는 함수
     *      - Post를 postNo로 가져온다.
     *      - 파일 번호를 가져와서 파일을 업데이트
     *      - 포스트 업데이트
     * */
    @PutMapping("/challenge/{challengeNo}/post/{postNo}")
    public Result update(@PathVariable("challengeNo") int challengNo, @PathVariable("postNo") int postNo, PostRequest postRequest) throws Exception {

        Post _post = new Post();
        log.info("postNo : " + postNo);
        log.info("challengNo : " + challengNo);

        log.info("getPost_content : " + postRequest.getPost_content());

        // 새로 저장
        if(postRequest.getFile() != null){

            // 기존 가지고 있던 데이터 삭제
            Post post = postService.getPost(postNo);
            s3Uploader.deleteS3(post.getMedia().getFile_path());
            mediaService.delete(post.getMedia().getFile_no());

            String type = getFileType(postRequest.getFile());
            Media media = s3Uploader.upload(postRequest.getFile(), type.toLowerCase(), "media");
            _post.setMedia(media);
        }

        if(postRequest.getPost_content() != null) {
            _post.setPost_content(postRequest.getPost_content());

            // 본문 텍스트 파싱
            String content = postRequest.getPost_content();
            String[] splitSharp = content.split(" ");

            for(String str : splitSharp){
                if(str.startsWith("#")){
                    // #을 분리하고 태그명만 추출
                    tagService.saveTag(str);
                }
            }
        }
        int postId = 0;

        // 포스트 업데이트
        postId = postService.update(postNo, _post);

        if(postId != 0)
            return new Result(true, HttpStatus.OK.value());

        return new Result(false, HttpStatus.OK.value());
    }

    
    /** 
     *  저장된 포스트를 삭제하는 함수
     * */
    @DeleteMapping("/post/{postNo}")
    public Result delete(@PathVariable("postNo") int postNo){

        log.info("postNo : "+ postNo);

        Post post = postService.getPost(postNo);

        Media media = post.getMedia();

        s3Uploader.deleteS3(media.getFile_path());

        mediaService.delete(media.getFile_no());

        List<PostTag> ptl = tagService.findPostTagList(postNo);

        for(PostTag pt : ptl){
            log.info("post_tag_no : " + pt.getPost_tag_no());
            tagService.deletePostTag(pt);
        }

        postService.delete(postNo);

        return new Result(true, HttpStatus.OK.value());
    }

    private final AlertService alertService;

    /**
     *  좋아요 클릭 처리(Complete)
     * */
    @PostMapping("/post/{postNo}/like/{userNo}")
    public Result like(@PathVariable("postNo") int postNo, @PathVariable("userNo") int userNo){

        // 포스트 라이크 테이블에서 해당 유저번호와 해당 게시글에 해당하는 엔티티가 있는지 검색
        List<PostLike> postLike = postLikeService.getPostLikeByUserNoPostNo(postNo, userNo);

        if(!postLike.isEmpty()){
            // 좋아요 누른 정보가 있으면
            // delete
            postLikeService.delete(postNo, userNo);
        } else {
            // insert
            PostLike _postLike = new PostLike(postNo, userNo);
            postLikeService.save(_postLike);

            // 좋아요를 눌렀을때 알림 설정
            Alert alert = new Alert();
            User writer = postService.getPost(postNo).getUser();
            alert.setUser(writer);
            alert.setAlert_content(userService.findUser(userNo).getUser_nickname() + "님이 포스트에 좋아요를 눌렀습니다.");
            alertService.saveAlert(alert);
        }

        return new Result(true, HttpStatus.OK.value());
    }

    @GetMapping("/post/{userNo}")
    public Result postByUser(@PathVariable("userNo") int user_no){
        List<Post> postListByUserNo = postService.getPostListByUserNo(user_no);
        List<PostResponse> collect = new ArrayList<>();
        if(!postListByUserNo.isEmpty()){
            collect = postListByUserNo.stream()
                    .map(p -> {
                        int challenge_no = p.getChallenge_no();
                        Challenge challenge = challengeService.findChallengeByChallengeNo(challenge_no);
                        int like_cnt = postLikeService.postLikeCnt(p.getPost_no());
                        int comment_cnt = commentService.postCommentCnt(p.getPost_no());
                        return new PostResponse(p, challenge, like_cnt, comment_cnt);
                    }).collect(Collectors.toList());
        }
        return new Result(true, HttpStatus.OK.value(), collect);
    }

    @Data
    static class PostResponse{
        private int challenge_no;
        private int post_no;
        private int file_no;
        private String file_path;
        private String file_savedname;
        private String challenge_title;
        private int like_cnt;
        private int comment_cnt;

        public PostResponse(Post post, Challenge challenge, int like_cnt, int comment_cnt) {
            this.challenge_no = post.getChallenge_no();
            this.post_no = post.getPost_no();
            this.file_no = post.getMedia().getFile_no();
            this.file_path = post.getMedia().getFile_path();
            this.file_savedname = post.getMedia().getFile_savedname();
            this.challenge_title = challenge.getChallenge_title();
            this.like_cnt = like_cnt;
            this.comment_cnt = comment_cnt;
        }
    }

    /**
     * @param postNo 
     *   게시글 번호 받아서 해당 게시글의 신고 횟수 추가(Complete)
     */
    @PostMapping("/report/post/{postNo}")
    public Result report(@PathVariable("postNo") int postNo){

        postService.updateReport(postNo);

        return new Result(true, HttpStatus.OK.value());
    }

    /** 
     *  프론트 단에서 파일을 받아 확장자에 따라 파일 타입을 결정
     *      - String으로 할지 Enum으로 할지 결정 필요
     * */
    private String getFileType(MultipartFile files){
        String fileName = files.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

        if(extension.equals("mp4") || extension.equals("MP4"))
            return "VIDEO";

        extension = extension.toLowerCase();
        if(extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png"))
            return "IMAGE";

        if(extension.equals("avi"))
            return "VIDEO";

        return null;
    }

    /** 
     *  챌린지 번호로 챌린지 정보를 가져와 파일 타입이 유효한 타입인지 확인
     *      - 유효하면 true
     *      - 잘못된 타입이면 false
     * */
    private boolean fileTypeValidate(int challengeNo, String fileType){
        // 챌린지 번호로 챌린지 정보 가져오기
        Challenge challenge = challengeService.findChallenges().get(challengeNo);

        //입력받은 파일의 타입과 챌린지 타입 비교
        if(fileType.equals(challenge.getChallenge_type())){
            return true;
        }
        return false;
    }

}
