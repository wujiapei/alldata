package com.platform.mapper;

import com.platform.model.OmsOrder;
import com.platform.model.OmsOrderExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface OmsOrderMapper {
    long countByExample(OmsOrderExample example);

    int deleteByExample(OmsOrderExample example);

    int deleteByPrimaryKey(Long id);

    int insert(OmsOrder record);

    int insertSelective(OmsOrder record);

    List<OmsOrder> selectByExample(OmsOrderExample example);

    OmsOrder selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") OmsOrder record, @Param("example") OmsOrderExample example);

    int updateByExample(@Param("record") OmsOrder record, @Param("example") OmsOrderExample example);

    int updateByPrimaryKeySelective(OmsOrder record);

    int updateByPrimaryKey(OmsOrder record);

    Double getTotalSalesOfToday();

    Double getTotalSalesOfYesToday();

    Double getTotalSalesOfNearly7Days();
    
    Double getTotalSalesOfWeek();

    Double getTotalSalesOfMonth();

    Integer getNumOfWaitForPay();

    Integer getNumOfFinished();

    Integer getNumOfWaitForConfirmRecvice();

    Integer getNumOfWaitForDeliverGoods();

    Integer getNumOfNewShortageRegistration();

    Integer getNumOfWaitForRefundApplication();

    Integer getNumOfOutgoingOrders();

    Integer getReturnOrdersToBeProcessed();

    Integer getAdvertisingSpaceNealyExpire();

    Integer getTodayTotalNumOfOrder();

    Integer getMonthTotalNumOfOrder();

    Integer getWeekTotalNumOfOrder();
}