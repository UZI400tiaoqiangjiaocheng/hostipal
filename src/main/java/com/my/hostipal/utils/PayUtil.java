package com.my.hostipal.utils;

import com.alibaba.fastjson2.JSONObject;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PayUtil {
    public static String pay(Config config, String subject, String orderNo, String amount) throws Exception {
        Factory.setOptions(config);
        AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace().preCreate(subject, orderNo, amount);
        //解析结果
        System.out.println("-----------------");
        String httpBody = response.getHttpBody();
        //转换为json对象
        JSONObject jsonObject = JSONObject.parseObject(httpBody);
        //获取二维码
        String qrCode = jsonObject.getJSONObject("alipay_trade_precreate_response").getString("qr_code");
        return qrCode;
    }

    //主动查询支付是否成功
    public static String query(Config config, String orderNo) throws Exception {
        Factory.setOptions(config);
        AlipayTradeQueryResponse query = Factory.Payment.Common().query(orderNo);
        //解析结果
        String httpBody = query.getHttpBody();
        //转换为json对象
        JSONObject jsonObject = JSONObject.parseObject(httpBody);
        //获取支付状态
        String status = jsonObject.getJSONObject("alipay_trade_query_response").getString("code");
        //判断支付状态是否成功
        if (status.equals("10000")) {
            //获取支付订单号
            String outTradeNo = jsonObject.getJSONObject("alipay_trade_query_response").getString("out_trade_no");
            return outTradeNo;
        }
        //休息0.3秒后重试
        Thread.sleep(300);
        //递归调用查询方法
        return query(config, orderNo);
    }
}
