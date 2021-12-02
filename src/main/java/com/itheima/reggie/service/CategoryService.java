package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;

public interface CategoryService extends IService<Category> {

    //多表删除
    void remove(Long id);


}
