package com.heima.common.constants;


// 用户行为
public class BehaviorConstants {

    /**
     * 用户点赞
     */
    public static final Short LIKE_BEHAVIOR = 0;
    public static final Short UNLIKE_BEHAVIOR = 1;

    /**
     * 用户不喜欢
     */

    public static final Short IS_UNLIKE_BEHAVIOR = 0;
    public static final Short NOT_UNLIKE_BEHAVIOR = 1;

    /**
     * 存储用户行为的key前缀
     */

    public static final String LIKE_BEHAVIOR_KEY="LIKE-BEHAVIOR-";
    public static final String UN_LIKE_BEHAVIOR_KEY="UNLIKE-BEHAVIOR-";
    public static final String COLLECTION_BEHAVIOR_KEY="COLLECTION-BEHAVIOR-";
    public static final String READ_BEHAVIOR_KEY="READ-BEHAVIOR-";
    public static final String APUSER_FOLLOW_RELATION_KEY="APUSER-FOLLOW-";
    public static final String APUSER_FANS_RELATION_KEY="APUSER-FANS-";
}
