package com.heima.model.common.annotation;

import com.heima.model.common.valid.AuthStatusValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AuthStatusValidator.class) //绑定校验器
public @interface AuthStatusValue {
    String message() default "默认消息";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    int[] status() default {};

}
