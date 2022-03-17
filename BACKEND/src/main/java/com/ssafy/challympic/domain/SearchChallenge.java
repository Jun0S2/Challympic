package com.ssafy.challympic.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import java.util.Date;

import static javax.persistence.FetchType.LAZY;

@Entity
@Getter @Setter
public class SearchChallenge {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_challenge_no")
    private int search_challenge_no;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "challenge_no")
    private Challenge challenge;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_no")
    private User user;

    @Column(columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    @Temporal(TemporalType.TIMESTAMP)
    private Date search_regdate;
}
