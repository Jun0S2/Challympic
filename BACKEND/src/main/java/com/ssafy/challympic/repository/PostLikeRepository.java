package com.ssafy.challympic.repository;

import com.ssafy.challympic.domain.PostLike;
import com.ssafy.challympic.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostLikeRepository {

    private final EntityManager em;

    @Transactional
    public List<PostLike> findByPostNo(Integer postNo){
        return em.createQuery("select pl from PostLike pl where pl.post_no = :postNo", PostLike.class)
                .setParameter("postNo", postNo)
                .getResultList();
    }

    @Transactional
    public List<PostLike> findPostLikeByUserNoPostNo(Integer postNo, Integer userNo){

        List<PostLike> postLike = em.createQuery("select pl from PostLike pl where pl.post_no = :postNo and pl.user_no = :userNo", PostLike.class)
                .setParameter("postNo", postNo)
                .setParameter("userNo", userNo)
                .getResultList();
        return postLike;
    }

    @Transactional
    public void deleteByPostUser(Integer postNo, Integer userNo){
        List<PostLike> postLikeList = findPostLikeByUserNoPostNo(postNo, userNo);
        for(PostLike postLike : postLikeList){
            em.remove(postLike);
        }
    }

    @Transactional
    public void save(PostLike postLike){
        em.persist(postLike);
    }

    public int postLikeCnt(int post_no) {
        List<PostLike> postLikeByPost = em.createQuery("select pl from PostLike pl where pl.post_no = :post_no", PostLike.class)
                .setParameter("post_no", post_no)
                .getResultList();
        if(!postLikeByPost.isEmpty()){
            return postLikeByPost.size();
        }else{
            return 0;
        }
    }
}
