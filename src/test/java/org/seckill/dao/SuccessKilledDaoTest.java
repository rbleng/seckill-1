package org.seckill.dao;

import org.seckill.entity.SuccessKilled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SuccessKilledDaoTest {

    @Resource
    @Autowired
    private SuccessKilledDao successKilledDao;

    @Test
    public void testInsertSuccessKilled() throws Exception {
        /**
         * 第一次:insertCount=1
         * 第二次:insertCount=0
         */
        long seckillId=1000;
        long userPhone=13476191877L;
        int insertCount=successKilledDao.insertSuccessKilled(seckillId,userPhone);
        System.out.println("insertCount="+insertCount);
    }

    @Test
    public void testQueryByIdWithSeckill() throws Exception {
        long seckillId=1000L;
        long userPhone=13476191877L;
        SuccessKilled successKilled=successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
        System.out.println(successKilled);
        System.out.println(successKilled.getSeckill());
    }

}