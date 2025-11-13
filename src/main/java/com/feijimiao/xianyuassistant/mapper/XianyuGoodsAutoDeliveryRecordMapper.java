package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryRecord;
import org.apache.ibatis.annotations.*;

/**
 * 商品自动发货记录Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryRecordMapper {
    
    /**
     * 插入记录
     */
    @Insert("INSERT INTO xianyu_goods_auto_delivery_record (xianyu_account_id, xianyu_goods_id, xy_goods_id, content, state) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{content}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoDeliveryRecord record);
}
