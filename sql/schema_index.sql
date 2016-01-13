-- User
CREATE INDEX user_idx_created ON user (deleted);


-- LikeSocialRelation
CREATE INDEX likesocialrel_idx_actor_target ON likesocialrelation (actor, actorType, target, targetType);


-- FollowSocialRelation
CREATE INDEX followsocialrel_idx_actor_target ON followsocialrelation (actor, actorType, target, targetType);


-- PostSocialRelation
CREATE INDEX postsocialrel_idx_actor_target ON postsocialrelation (actor, actorType, target, targetType);


-- ViewSocialRelation
CREATE INDEX viewsocialrel_idx_actor_target ON viewsocialrelation (actor, actorType, target, targetType);


-- Post
CREATE INDEX post_idx_cat_sold ON post (category_id, soldMarked);


-- Activity
CREATE INDEX activity_idx_actor_target_user ON activity (actor, actorType, target, targetType, userId);


-- GCMToken
CREATE INDEX gcm_idx_user ON gcmtoken (userId);
