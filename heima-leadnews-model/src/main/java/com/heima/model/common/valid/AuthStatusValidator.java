package com.heima.model.common.valid;

import com.heima.model.common.annotation.AuthStatusValue;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class AuthStatusValidator implements ConstraintValidator<AuthStatusValue,Short> {

    private Set<Integer> set=new HashSet<>();

    @Override
    public boolean isValid(Short status, ConstraintValidatorContext constraintValidatorContext) {
        return  set.contains(status);
    }

    @Override
    public void initialize(AuthStatusValue constraintAnnotation) {
        int[] value = constraintAnnotation.status();
        for (int i : value) {
            set.add(i);
        }
    }
}
