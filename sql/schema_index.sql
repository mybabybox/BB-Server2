-- Show Index
SHOW INDEX FROM [Table];
SELECT DISTINCT TABLE_NAME, INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = 'babybox';


-- Drop Index
ALTER TABLE [Table] DROP INDEX [Index];


--
--



-- User [DONE]
CREATE INDEX user_idx_created ON user (deleted);
CREATE INDEX user_idx_displayname ON user (displayname);


-- Post [DONE]
CREATE INDEX post_idx_cat_sold ON post (category_id, soldMarked);


-- LikeSocialRelation [DONE]
CREATE INDEX likesocialrelation_idx_actor_target ON likesocialrelation (actor, actorType, target, targetType);


-- FollowSocialRelation [DONE]
CREATE INDEX followsocialrelation_idx_actor_target ON followsocialrelation (actor, actorType, target, targetType);


-- PostSocialRelation [DONE]
CREATE INDEX postsocialrelation_idx_actor_target ON postsocialrelation (actor, actorType, target, targetType);


-- ViewSocialRelation [DONE]
CREATE INDEX viewsocialrelation_idx_actor_target ON viewsocialrelation (actor, actorType, target, targetType);


-- Activity [DONE]
CREATE INDEX activity_idx_actor_target_user ON activity (actor, actorType, target, targetType, userId);


-- GCMToken [DONE]
CREATE INDEX gcm_idx_user ON gcmtoken (userId);
