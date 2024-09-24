package com.heima.model.article.vos;

import lombok.Data;

@Data
public class ArticleBehaviorVO {

    // 返回对象，但是也可以使用resultmap返回

    private Boolean islike;
    private Boolean isunlike;
    private Boolean iscollection;
    private Boolean isfollow;

    public ArticleBehaviorVO() {
        this.islike = true;
        this.isunlike = true;
        this.iscollection = true;
        this.isfollow = true;
    }


}
