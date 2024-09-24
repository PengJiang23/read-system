package com.heima.model.wemedia.dtos;


import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;

@Data
public class NewsAuthDto extends PageRequestDto {

    /**
     * 文章id
     */
    private Integer id;


    /**
     * msg
     */

    private String msg;


    /**
     * 文章状态
     */
    private Integer status;

    /**
     * 文章标题
     */
    private String title;
}
