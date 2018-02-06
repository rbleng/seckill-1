package org.seckill.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;


//@Componet @Service @Dao @Controller
@Service
public class SeckillServiceImpl implements SeckillService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    //注入Service依赖
    @Autowired
    private SeckillDao SeckillDao;
    @Autowired
    private SuccessKilledDao SuccessKilledDao;

    @Autowired
    private RedisDao redisDao;

    /* 加入一个盐值，用于混淆*/
    private final String salt = "skdfjksjdf7787%^%^%^FSKJFK*(&&%^%&^8DF8^%^^*7hFJDHFJ";

    /**
     * 查询全部的秒杀记录.
     *
     * @return 数据库中所有的秒杀记录
     */
    @Override
    public List<Seckill> getSeckillList() {
        return SeckillDao.queryAll(0, 4);
    }

    /**
     * 查询单个秒杀记录
     *
     * @param seckillId 秒杀记录的ID
     * @return 根据ID查询出来的记录信息
     */
    @Override
    public Seckill getById(long seckillId) {
        return SeckillDao.queryById(seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + salt;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;
    }

    /**
     * 在秒杀开启时输出秒杀接口的地址，否则输出系统时间跟秒杀地址
     *
     * @param seckillId 秒杀商品Id
     * @return 根据对应的状态返回对应的状态实体
     */
    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        // 根据秒杀的ID去查询是否存在这个商品
       /* Seckill seckill = SeckillDao.queryById(seckillId);
        if (seckill == null) {
            logger.warn("查询不到这个秒杀产品的记录");
            return new Exposer(false, seckillId);
        }*/
        // 优化点：缓存优化：超时的基础上维护一致性
        // 1.访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (seckill == null) {
            // 2.访问数据库读取数据
            seckill = SeckillDao.queryById(seckillId);
            if (seckill == null) {
                return new Exposer(false, seckillId);
            } else {
                // 3.放入redis
                redisDao.putSeckill(seckill);
            }
        }

        // 判断是否还没到秒杀时间或者是过了秒杀时间
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        // 系统当前时间
        Date nowTime = new Date();
        //   开始时间大于现在的时候说明没有开始秒杀活动    秒杀活动结束时间小于现在的时间说明秒杀已经结束了
       /* if (!nowTime.isAfter(startTime)) {
            logger.info("现在的时间不在开始时间后面，未开启秒杀");
            return new Exposer(false, seckillId, nowTime, startTime, endTime);
        }
        if (!nowTime.isBefore(endTime)) {
            logger.info("现在的时间不在结束的时间之前，可以进行秒杀");
            return new Exposer(false, seckillId, nowTime, startTime, endTime);
        }*/
        if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        //秒杀开启，返回秒杀商品的id,用给接口加密的md5，转化特定字符串的过程，不可逆
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);

    }



    /**
     * 执行秒杀操作，失败的，失败我们就抛出异常
     * 使用注解控制事务方法的优点： 1.开发团队达成一致约定，明确标注事务方法的编程风格
     * 2.保证事务方法的执行时间尽可能短，不要穿插其他网络操作，RPC/HTTP请求或者剥离到事务方法外部
     * 3.不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
     *
     * @param seckillId 秒杀的商品ID
     * @param userPhone 手机号码
     * @param md5       md5加密值
     * @return 根据不同的结果返回不同的实体信息
     */
    @Transactional
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            logger.error("秒杀数据被篡改");
            throw new SeckillException("seckill data rewrite");
        }
        // 执行秒杀业务逻辑：减库存+记录购买行为
        Date nowTIme = new Date();

       /* try {
            //执行减库存操作
            int reduceNumber = SeckillDao.reduceNumber(seckillId, nowTIme);
            if (reduceNumber <= 0) {
                logger.warn("没有更新数据库记录，说明秒杀结束");
                throw new SeckillCloseException("seckill is closed");
            } else {
                // 这里至少减少的数量不为0了，秒杀成功了就增加一个秒杀成功详细
                int insertCount = SuccessKilledDao.insertSuccessKilled(seckillId, userPhone);
                // 查看是否被重复插入，即用户是否重复秒杀
                if (insertCount <= 0) {
                    throw new RepeatKillException("seckill repeated");
                } else {
                    // 秒杀成功了，返回那条插入成功秒杀的信息
                    SuccessKilled successKilled = SuccessKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException | RepeatKillException e1) {
            throw e1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            // 把编译期异常转换为运行时异常
            throw new SeckillException("seckill inner error : " + e.getMessage());
        }*/

        try {
            // 记录购买行为
            int insertCount = SuccessKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) {
                // 重复秒杀
                throw new RepeatKillException("seckill repeated");
            } else {
                // 减库存 ，热点商品的竞争
                int reduceNumber = SeckillDao.reduceNumber(seckillId, nowTIme);
                if (reduceNumber <= 0) {
                    logger.warn("没有更新数据库记录，说明秒杀结束");
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    // 秒杀成功了，返回那条插入成功秒杀的信息  进行commit
                    SuccessKilled successKilled = SuccessKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException | RepeatKillException e1) {
            throw e1;
        }

    }

    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            return new SeckillExecution(seckillId, SeckillStatEnum.DATE_REWRITE);
        }
        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("seckillId", seckillId);
        map.put("phone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        // 执行储存过程，result被复制
        try {
            SeckillDao.killByProcedure(map);
            // 获取result
            int result = MapUtils.getInteger(map, "result", -2);
            if (result == 1) {
                SuccessKilled successKilled = SuccessKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS, successKilled);
            } else {
                return new SeckillExecution(seckillId, SeckillStatEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStatEnum.INNER_ERROR);
        }
    }
}







