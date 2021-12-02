package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.mapper.SetMealMapper;
import com.itheima.reggie.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetMealMapper setMealMapper;

    @Override
    public void remove(Long id) {
        // 查询dish表
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = Wrappers.lambdaQuery(Dish.class);
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        Integer dish = dishMapper.selectCount(dishLambdaQueryWrapper);
        //判断是否查到
        if (dish > 0) {
            //没查到返回错误信息
            throw new CustomException("该分类下有菜品信息，不能删除此数据");
        }
        // 查询dish表
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(Setmeal.class);
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        Integer setmeal = setMealMapper.selectCount(setmealLambdaQueryWrapper);
        //判断是否查到
        if (setmeal > 0) {
            //没查到返回错误信息
            throw new CustomException("该分类下有套餐信息，不能删除此数据");
        }
        //查到之后
        this.removeById(id);
    }
}
