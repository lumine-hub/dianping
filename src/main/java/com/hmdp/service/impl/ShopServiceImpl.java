package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {
        String key = "cache:shop:" + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if (StringUtils.isNotBlank(shopJson)) {
            Shop shop = JSON.parseObject(shopJson, Shop.class);
            return Result.ok(shop);
        }

        // 判断redis查询的是否是空值
        if ("".equals(shopJson)) {
            return Result.fail("店铺不存在!");
        }
        Shop shop = getById(id);
        if (shop == null) {
            // 将空值写入redis：缓存穿透
            stringRedisTemplate.opsForValue().set(key, "",10L,TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(shop),30L, TimeUnit.MINUTES);

        return Result.ok(shop);
    }
    public Shop query(Long id) {
        String key = "cache:shop:" + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(shopJson)) {
            Shop shop = JSON.parseObject(shopJson, Shop.class);
            return shop;
        }
        if ("".equals(shopJson)) {
            return null;
        }
        Shop shop = getById(id);
        if (shop == null) {
            stringRedisTemplate.opsForValue().set(key,"",10,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(shop),30,TimeUnit.MINUTES);
        return shop;

    }
    public Shop queryWithMutex(Long id) {
        String key = "cache:shop:" + id;
        Shop shop = null;

        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(shopJson)) {
            shop = JSON.parseObject(shopJson, Shop.class);
            return shop;
        }
        // 判断redis是否存的空值，缓存穿透
        if ("".equals(shopJson)) {
            return null;
        }

        String lockKey = "lock:shop:" + id;
        try {
            boolean trylock = trylock(lockKey);
            while (!trylock) {
                TimeUnit.MINUTES.sleep(3);
                return queryWithMutex(id);
            }
            shop = getById(id);
            if (shop == null) {
                stringRedisTemplate.opsForValue().set(key,"",10,TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(shop),30,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        unlock(lockKey);
        return shop;


    }

    private boolean trylock(String key) {
        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(aBoolean);
    }
    private  void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id为空");
        }
        updateById(shop);
        stringRedisTemplate.delete("cache:shop:" + id);
        return Result.ok();
    }
}
