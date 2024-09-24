package com.heima.common.exception;


import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice  //控制器增强类
@Slf4j
public class ExceptionCatch {

    /**
     * 处理不可控异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseResult exception(Exception e){
        e.printStackTrace();
        log.error("catch exception:{}",e.getMessage());

        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }

    /**
     * 处理可控异常  自定义异常
     * @param e
     * @return
     */
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public ResponseResult exception(CustomException e){
        log.error("catch exception:{}",e);
        return ResponseResult.errorResult(e.getAppHttpCodeEnum());
    }


    /**
     * 校验规则异常
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseResult exception(MethodArgumentNotValidException e){
        log.error("校验异常：{}，异常类型：{}",e.getMessage(),e.getClass());
        BindingResult bindingResult = e.getBindingResult();
        StringBuffer stringBuffer = new StringBuffer();
        bindingResult.getFieldErrors().forEach(item ->{
            //获取错误信息
            String message = item.getDefaultMessage();
            //获取错误的属性名字
            String field = item.getField();
            stringBuffer.append(field + ":" + message + " ");
        });
        return ResponseResult.errorResult(400,stringBuffer.toString());
    }
}
