package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetMealDishService;
import com.itheima.reggie.service.SetMealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private SetMealDishService setMealDishService;

    @Autowired
    private SetMealService setMealService;

    @Transactional
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品信息
        this.save(dishDto);//包含了dish 而且继承自dish，可以直接插入
        //保存菜品口味信息
        List<DishFlavor> flavors = dishDto.getFlavors();
        //给每个口味都关联该菜品的id
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dishDto.getId());
        });
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //根据id查询实现数据回显
        Dish dish = this.getById(id);
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        //再查询flavor数据信息封装到dishDto中
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = Wrappers.lambdaQuery(DishFlavor.class);
        dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> dishFlavors = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
        dishDto.setFlavors(dishFlavors);
        return dishDto;
    }

    /**
     * 更新操作，口味先删除完，再插入页面提交数据
     *
     * @param dishDto
     */
    @Transactional
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表的信息
        this.updateById(dishDto);
        //删除口味数据
        LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = Wrappers.lambdaQuery(DishFlavor.class);
        dishFlavorLambdaQueryWrapper.eq(dishDto.getFlavors() != null, DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(dishFlavorLambdaQueryWrapper);
        //删除完成再添加新的口味信息
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dishDto.getId());
        });
        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    @Override
    public void remove(List<Long> ids) {
        //B. 判断 : 如果菜品关联了套餐, 不能直接删除, 提示信息 ;
        //根据dishid查询setmeal_dish表中有没有套餐信息
        LambdaQueryWrapper<SetmealDish> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(SetmealDish.class)
                .in(ids != null, SetmealDish::getDishId, ids);
        int setMealDishCount = setMealDishService.count(setmealLambdaQueryWrapper);
        if (setMealDishCount > 0) {
            throw new CustomException("该菜品下关联有套餐信息，不能删除，请取消关联后重试");
        }

        //A. 判断 : 只能够删除已经 停售 的菜品 ;
        //根据id查询菜品是否停售,构建查询调价
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = Wrappers.lambdaQuery(Dish.class)
                .in(ids != null, Dish::getId, ids)
                .eq(Dish::getStatus, 1);
        int dishCount = this.count(dishLambdaQueryWrapper);
        if (dishCount > 0) {
            throw new CustomException("菜品中有在售菜品，不能直接删除，请停售后重试");
        }

        //C. 删除时, 需要删除菜品 dish 及 菜品的口味数据 dish_falvor ;
        //删除口味信息
        LambdaUpdateWrapper<DishFlavor> dishFlavorLambdaUpdateWrapper = Wrappers.lambdaUpdate(DishFlavor.class)
                .in(ids != null, DishFlavor::getDishId, ids);
        dishFlavorService.remove(dishFlavorLambdaUpdateWrapper);
        //删除菜品信息
        this.removeByIds(ids);
    }

    //批量修改status
    @Transactional
    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        //1). 判断: 如果停售的菜品关联了在售的套餐是不允许的, 需要提示错误信息
        //根据id查询关联套餐，如果有套餐是在售状态就不允许删除
        //根据id查询关联套餐的口味信息
        if (status == 0) {
            LambdaQueryWrapper<SetmealDish> setmealDishLambdaQueryWrapper = Wrappers.lambdaQuery(SetmealDish.class);
            setmealDishLambdaQueryWrapper.in(ids != null, SetmealDish::getDishId, ids);
            List<SetmealDish> setmealDishes = setMealDishService.list(setmealDishLambdaQueryWrapper);
            List<Long> setMealIds = setmealDishes.stream().map(SetmealDish::getSetmealId).collect(Collectors.toList());
            //根据id查询setmeal表是否有在售套餐
            int count = 0;
            if (setMealIds.size() > 0) {
                LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = Wrappers.lambdaQuery(Setmeal.class)
                        .in(Setmeal::getId, setMealIds)
                        .eq(Setmeal::getStatus, 1);
                count = setMealService.count(setmealLambdaQueryWrapper);
            }
            if (count > 0) {
                throw new CustomException("操作菜品中关联有在售套餐，请停售套餐后重试");
            }
        }
        //2). 批量修改菜品的状态
        LambdaUpdateWrapper<Dish> dishLambdaUpdateWrapper = Wrappers.lambdaUpdate(Dish.class);
        dishLambdaUpdateWrapper.set(Dish::getStatus, status)
                .in(Dish::getId, ids);
        this.update(dishLambdaUpdateWrapper);
    }
}
