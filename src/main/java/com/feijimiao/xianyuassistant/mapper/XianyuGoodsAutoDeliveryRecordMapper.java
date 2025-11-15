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
    @Insert("INSERT INTO xianyu_goods_auto_delivery_record (xianyu_account_id, xianyu_goods_id, xy_goods_id, buyer_user_id, buyer_user_name, content, state) " +
            "VALUES (#{xianyuAccountId}, #{xianyuGoodsId}, #{xyGoodsId}, #{buyerUserId}, #{buyerUserName}, #{content}, #{state})")
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
    
    /**
     * 根据账号ID和商品ID查询记录（分页）
     */
    @Select("<script>" +
            "SELECT r.*, g.title as goods_title " +
            "FROM xianyu_goods_auto_delivery_record r " +
            "LEFT JOIN xianyu_goods g ON r.xy_goods_id = g.xy_good_id " +
            "WHERE r.xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND r.xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "ORDER BY r.create_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "xianyuAccountId", column = "xianyu_account_id"),
        @Result(property = "xianyuGoodsId", column = "xianyu_goods_id"),
        @Result(property = "xyGoodsId", column = "xy_goods_id"),
        @Result(property = "buyerUserId", column = "buyer_user_id"),
        @Result(property = "buyerUserName", column = "buyer_user_name"),
        @Result(property = "content", column = "content"),
        @Result(property = "state", column = "state"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "goodsTitle", column = "goods_title")
    })
    List<XianyuGoodsAutoDeliveryRecord> selectByAccountIdWithPage(
            @Param("accountId") Long accountId,
            @Param("xyGoodsId") String xyGoodsId,
            @Param("limit") int limit,
            @Param("offset") int offset);
    
    /**
     * 统计记录总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM xianyu_goods_auto_delivery_record " +
            "WHERE xianyu_account_id = #{accountId} " +
            "<if test='xyGoodsId != null and xyGoodsId != \"\"'>" +
            "AND xy_goods_id = #{xyGoodsId} " +
            "</if>" +
            "</script>")
    long countByAccountId(@Param("accountId") Long accountId, @Param("xyGoodsId") String xyGoodsId);
    
    /**
     * 更新发货记录状态
     */
    @Update("UPDATE xianyu_goods_auto_delivery_record SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") Long id, @Param("state") Integer state);
}