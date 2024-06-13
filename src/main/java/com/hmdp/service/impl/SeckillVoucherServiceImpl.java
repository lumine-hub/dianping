package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Wrapper;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
@Transactional
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;
    @Override
    public boolean updateStock(Long id) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(id);
        if (seckillVoucher.getStock() - 1 < 0) {
            return false;
        }
//        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//        seckillVoucherMapper.updateById(seckillVoucher);
        int row = seckillVoucherMapper.updateStock(id, seckillVoucher.getStock() - 1);
        if (row > 0) {
            return true;
        }
        return false;

    }
}
