package com.feijimiao.xianyuassistant.mapper;

import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import org.apache.ibatis.annotations.*;

/**
 * 商品自动发货配置Mapper
 */
@Mapper
public interface XianyuGoodsAutoDeliveryConfigMapper {
    
    /**
     * 根据账号ID和商品ID查询配置
     */
    @Select("SELECT * FROM xianyu_goods_auto_delivery_config WHERE xianyu_account_id = #{accountId} AND xy_goods_id = #{xyGoodsId}")
    XianyuGoodsAutoDeliveryConfig selectByAccountAndGoodsId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);
    
    /**
     * 插入配置
     */
    @Insert("INSERT INTO xianyu_goods_auto_delivery_config (xianyu_account_id, xianyu_goods_id, xy_goods_id, type, auto_delivery_content) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{type}, #{autoDeliveryContent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuGoodsAutoDeliveryConfig config);
    
    /**
     * 更新配置
     */
    @Update("UPDATE xianyu_goods_auto_delivery_config SET type = #{type}, auto_delivery_content = #{autoDeliveryContent} WHERE id = #{id}")
    int update(XianyuGoodsAutoDeliveryConfig config);
}
