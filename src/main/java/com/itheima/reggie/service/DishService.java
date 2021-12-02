package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    //添加菜品和菜品的口味
    void saveWithFlavor(DishDto dishDto);

    //数据回显查询
    DishDto getByIdWithFlavor(Long id);

    //修改操作
    void updateWithFlavor(DishDto dishDto);

    //批量删除
    void remove(List<Long> ids);

    void updateStatus(Integer status, List<Long> ids);
}
