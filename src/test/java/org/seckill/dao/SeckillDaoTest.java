package org.seckill.dao;

import org.seckill.entity.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({"classpath:spring/applicationContext-dao.xml"})
public class SeckillDaoTest {

    //注入Dao实现类依赖
    @Resource
    private SeckillDao SeckillDao;

//    @Test
    public void reduceNumber() throws Exception {
        long seckillId=1000;
        Date Date=new Date();
        int i = SeckillDao.reduceNumber(seckillId, Date);
        System.out.println(i);
    }

//    @Test
    public void queryById() throws Exception {
        //Parameter 'offset' not found. Available parameters are [0, 1, param1, param2]
        // java没有保存形参的记录:queryAll(int offset, int limit) -> queryAll(arg1,arg2)
        long seckillId = 1000;
        Seckill seckill = SeckillDao.queryById(seckillId);
        System.out.println(seckill.toString());
    }

//    @Test
    public void queryAll() throws Exception {
        List<Seckill> seckills = SeckillDao.queryAll(0, 100);
        for (Seckill seckill : seckills) {
            System.out.println(seckill.toString());
        }
    }

}