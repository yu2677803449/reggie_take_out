package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetMealMapper;
import com.itheima.reggie.service.SetMealDishService;
import com.itheima.reggie.service.SetMealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetMealServiceImpl extends ServiceImpl<SetMealMapper, Setmeal> implements SetMealService {

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private SetMealService setMealService;

    //多表添加操作
    @Transactional
    @Override
    public void saveWithDish(SetmealDto dto) {
        //将数据插入SteaMeal表中
        this.save(dto);

        //再 将setmeal dish信息加到套餐表中
        List<SetmealDish> setmealDishes = dto.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(dto.getId());
        });
        setMealDishService.saveBatch(setmealDishes);
    }

    @Transactional
    @Override
    public void removeWithDish(List<Long> ids) {
        //判断是否是在售状态
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(Setmeal.class);
        setmealLambdaQueryWrapper.in(ids.size() > 0, Setmeal::getId, ids);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        int count = this.count(setmealLambdaQueryWrapper);
        if (count > 0) {
            throw new CustomException("选择的套餐中有在售套餐，不能删除");
        }
        //执行删除setmeal表操作
        this.removeByIds(ids);
        //执行删除setmealdish操作
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = Wrappers.lambdaQuery(SetmealDish.class);
        setmealDishLambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setMealDishService.remove(setmealDishLambdaQueryWrapper);
    }

    @Override
    public void update(SetmealDto setmealDto) {
        //判断套餐是否是在售状态
        //根据数据库查询套餐是否是在售状态
        //在售状态给出错误提示
        Setmeal setmeal = setMealService.getById(setmealDto.getId());
        if (setmeal.getStatus() == 1) {
            throw new CustomException("改套餐还是在售状态，不能修改，请停售后重试");
        }
        setmealDto.setStatus(setmeal.getStatus());
        //修改套餐基本信息
        setMealService.updateById(setmealDto);
        //修改套餐关联的菜品信息列表信息,根据id删除
        //先删除所有，在加入页面提交数据
        LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .eq(setmealDto.getId() != null, SetmealDish::getSetmealId, setmealDto.getId());
        setMealDishService.remove(setmealDishLambdaQueryWrapper);
        //封装保存的setmealid
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDto.getId());
        });
        setMealDishService.saveBatch(setmealDishes);
    }
}
