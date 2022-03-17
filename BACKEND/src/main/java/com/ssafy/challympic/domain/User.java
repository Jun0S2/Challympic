package com.ssafy.challympic.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ssafy.challympic.domain.defaults.UserActive;
import com.ssafy.challympic.domain.defaults.UserAuthEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter @Setter
@Table(uniqueConstraints = {@UniqueConstraint(name = "email_nickname_unique", columnNames = {"user_email", "user_nickname"})})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 생성 전략. AUTO_INCREMENT0
    @Column(name = "user_no")
    private int user_no;

    @Column(nullable = false)
    private String user_email;

    @Column(nullable = false)
    private String user_pwd;

    @Column(columnDefinition = "varchar(100) default 'USER'")
    @Enumerated(EnumType.STRING)
    private UserAuthEnum user_auth = UserAuthEnum.USER;

    @Column(columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date user_regdate;

    @Column(columnDefinition = "varchar(100) default 'ACTIVE'")
    @Enumerated(EnumType.STRING)
    private UserActive user_active = UserActive.ACTIVE;

    @Temporal(TemporalType.TIMESTAMP)
    private Date user_inactivedate;

    @Column(nullable = false)
    private String user_nickname;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "file_no")
    private Media media;

    @Column(columnDefinition = "varchar(50) default '도전자'")
    private String user_title;

    @OneToMany(mappedBy = "user")
    private List<Challenge> challenge;

    @OneToMany(mappedBy = "user")
    private List<Interest> interest;

    @OneToMany(mappedBy = "user")
    private List<Subscription> subscription;

    @OneToMany(mappedBy = "follow_following_no")
    private List<Follow> following;

    @OneToMany(mappedBy = "follow_follower_no")
    private List<Follow> follower;

    @OneToMany(mappedBy = "user")
    private List<QnA> qna;
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
//    private List<Comment> comment;
//
//    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
//    private List<CommentLike> commentLike;
}
