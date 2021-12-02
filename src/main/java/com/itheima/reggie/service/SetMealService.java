package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

public interface SetMealService extends IService<Setmeal> {
    //多表保存
    void saveWithDish(SetmealDto dto);
    //多表删除
    void removeWithDish(List<Long> ids);

    void update(SetmealDto setmealDto);
}
