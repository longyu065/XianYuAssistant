package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 商品自动发货记录Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryRecordMapper {
    
    /**
     * 插入记录
     */
    @Insert("INSERT INTO xianyu_goods_auto_delivery_record (xianyu_account_id, xianyu_goods_id, xy_goods_id, buyer_user_id, content, state) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{buyerUserId}, #{content}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoDeliveryRecord record);
    
    /**
     * 根据账号ID查询记录
     */
    @Select("SELECT * FROM xianyu_goods_auto_delivery_record WHERE xianyu_account_id = #{accountId} ORDER BY create_time DESC")
    List<XianyuGoodsAutoDeliveryRecord> selectByAccountId(@Param("accountId") Long accountId);
    
    /**
     * 根据账号ID删除记录
     */
    @Delete("DELETE FROM xianyu_goods_auto_delivery_record WHERE xianyu_account_id = #{accountId}")
    int deleteByAccountId(@Param("accountId") Long accountId);
}