package com.itheima.reggie.common;

import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Employee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<Employee> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        String msg = e.getMessage();
        log.error("出异常{}", msg);
        if (msg.contains("Duplicate entry")) {
            String[] s = msg.split(" ");
            return R.error(s[2] + "已存在");
        }
        return R.error("未知错误");
    }

    @ExceptionHandler(CustomException.class)
    public R<Category> handleException(CustomException e){
        log.error(e.getMessage());
        return R.error(e.getMessage());
    }
}
