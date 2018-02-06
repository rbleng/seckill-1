package org.seckill.dao;

import org.seckill.entity.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({"classpath:spring/applicationContext-dao.xml"})
public class SuccessKilledDaoTest {

    @Resource
    private SuccessKilledDao SuccessKilledDao;

    @Test
    public void testInsertSuccessKilled() throws Exception {
        /**
         * 第一次:insertCount=1
         * 第二次:insertCount=0
         */
        long seckillId=1000;
        long userPhone=13476191877L;
        int insertCount=SuccessKilledDao.insertSuccessKilled(seckillId,userPhone);
        System.out.println("insertCount="+insertCount);
    }

    @Test
    public void testQueryByIdWithSeckill() throws Exception {
        long seckillId=1000L;
        long userPhone=13476191877L;
        SuccessKilled successKilled=SuccessKilledDao.queryByIdWithSeckill(seckillId,userPhone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }

}