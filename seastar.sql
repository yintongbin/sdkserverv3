-- ----------------------------
-- Table structure for t_app
-- ----------------------------
DROP TABLE IF EXISTS t_app;
CREATE TABLE t_app (
  appId int(12) COMMENT 'id',
  appName varchar(80) DEFAULT NULL COMMENT '应用名称',
  status int(1) DEFAULT 0 COMMENT '0等待审核,1审核通过,2上架,3下架',
  createTime datetime DEFAULT NULL COMMENT '创建日期',
  appKey varchar(80) DEFAULT NULL COMMENT '服务器验证key',
  appSecret varchar(80) DEFAULT NULL COMMENT '服务器间加密key',
  sandbox int(1) DEFAULT 1 COMMENT '0正式模式，1沙箱模式',
  `version` varchar(16) NOT NULL COMMENT '当前版本',
  notifyUrl varchar(200) DEFAULT NULL COMMENT '支付回调url',
  payType int(8) DEFAULT 0 COMMENT '支付方式，仅安卓版有效，0无支付方式 1google 2mycard 3apple 4google&mycard',
  PRIMARY KEY (appId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_app_google
-- ----------------------------
DROP TABLE IF EXISTS t_app_google;
CREATE TABLE t_app_google (
  appId int(12) DEFAULT 0 COMMENT '应用id',
  createTime datetime DEFAULT NULL,
  googleKey varchar(1024) NOT NULL COMMENT '存放签名用key',
  PRIMARY KEY (appId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_app_mycard
-- ----------------------------
DROP TABLE IF EXISTS t_app_mycard;
CREATE TABLE t_app_mycard (
  facServiceId varchar(30) DEFAULT '1' COMMENT '厂商服务代码, 1android sdk, 2web',
  tradeType varchar(1) NOT NULL COMMENT '交易模式 1:android 2:web',
  hashKey varchar(32) NOT NULL COMMENT '厂商key',
  sandBoxMode varchar(5) NOT NULL COMMENT '是否为测试环境, true, false'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_mycard_product
-- ----------------------------
DROP TABLE IF EXISTS t_mycard_product;
CREATE TABLE t_mycard_product (
  appId int(12) DEFAULT 0 COMMENT '应用id',
  createTime datetime DEFAULT NULL,
  paymentType varchar(15) NOT NULL COMMENT 'mycard付费方式, INGAME实体卡，COSTPOINT点卡 FA018上海webatm, FA029中华电信HiNet连扣, FA200000002测试用',
  itemCode varchar(15) NOT NULL COMMENT 'mycard品项代码',
  productName varchar(50) NOT NULL COMMENT '产品名称',
  amount varchar(60) DEFAULT '0' COMMENT '交易金额，可以为整数，若有小鼠最多2位',
  currency varchar(10) DEFAULT 'TWD' COMMENT '货币单位', 
  UNIQUE KEY idx_app_item (appId, itemCode) USING BTREE,
  KEY idx_app_id (appId) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for t_user_base
-- ----------------------------
DROP TABLE IF EXISTS t_user_base;
CREATE TABLE t_user_base (
  userId bigint(20) DEFAULT 0 COMMENT '用户唯一标识 在开头添加ST组成username',
  userName varchar(40) NOT NULL COMMENT '用户名 登录框中用  与ppid一一对应',
  password varchar(32) NOT NULL COMMENT '登录密码' ,
  email varchar(40) DEFAULT NULL COMMENT '密保邮箱',
  status int(1) DEFAULT 0 COMMENT '0禁止登录 1正常使用',
  createTime datetime NOT NULL,
  deviceId varchar(50) NOT NULL COMMENT '设备号(用于查询小号)',
  locale varchar(16)  DEFAULT NULL COMMENT '语言设置',
  PRIMARY KEY (userId),
  UNIQUE KEY idx_username (userName) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for t_user_login_channel
-- ----------------------------
DROP TABLE IF EXISTS t_user_login_channel;
CREATE TABLE t_user_login_channel (
  id bigint(20) AUTO_INCREMENT,
  userId bigint(20) DEFAULT 0 COMMENT '用户唯一标识u_user_base',
  thirdUserId varchar(60) NOT NULL,
  loginType int(1) DEFAULT 1 COMMENT '0帐号系统, 1guest 2google 3gamecenter 4facebook',
  createTime timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '帐号创建时间',
  appId int(12) DEFAULT 0 COMMENT '在哪个应用绑定的',
  PRIMARY KEY (id),
  KEY idx_login (loginType, thirdUserId) USING BTREE,
  KEY idx_login_userId (loginType, userId) USING BTREE,
  KEY idx_login_id (userId) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1075361 DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for t_pay_info
-- ----------------------------
DROP TABLE IF EXISTS t_pay_info;
CREATE TABLE t_pay_info (
  `order` varchar(60) NOT NULL COMMENT '订单id',
  appId int(12) DEFAULT 0 COMMENT '本交易信息来自哪个app',
  userId bigint(20) DEFAULT 0 COMMENT '用户唯一标识u_user_base',
  gameRoleId varchar(50) DEFAULT NULL COMMENT '游戏用户id(回调时返回该信息)',
  serverId varchar(10) DEFAULT NULL COMMENT '游戏服务器id',
  payStatus int(1) DEFAULT 0 COMMENT '充值状态0创建订单,1等待推送,2推送成功,3推送失败',
  productId varchar(50) DEFAULT NULL COMMENT '产品id',
  amount varchar(20) DEFAULT NULL COMMENT '充值金额',
  currency varchar(20) DEFAULT NULL COMMENT '币种',
  channelType int(8) DEFAULT 0 COMMENT '充值渠道类型：0谷歌 1apple 2mycard',
  channelOrder varchar(60) DEFAULT NULL COMMENT '充值渠道订单',
  createTime datetime DEFAULT NULL COMMENT '创建订单日期',
  notifyTime datetime DEFAULT NULL COMMENT '订单通知到CP日期',
  sandbox int(1) DEFAULT 0 COMMENT '0正式模式，1沙箱模式',
  cparam varchar(200) DEFAULT NULL COMMENT '回调gs传递的参数',
  PRIMARY KEY (`order`),
  KEY idx_channel_order (channelOrder, channelType) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS t_operative;
CREATE TABLE t_operative (
  appId int(12) DEFAULT 0 COMMENT '应用id',
  productId varchar(32) NOT NULL COMMENT '商品id',
  virtualCoin varchar(10) DEFAULT 'TWD' COMMENT '实际附送金额', 
  giveVirtualCoin varchar(10) DEFAULT 'TWD' COMMENT '赠送金额', 
  money varchar(30) DEFAULT '0' '默认币种下的金额',
  UNIQUE KEY idx_op_item (appId, productId) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS t_mycard_trade;
CREATE TABLE t_mycard_trade (
  id int(10) AUTO_INCREMENT,
  paymentType varchar(15) NOT NULL COMMENT 'mycard付费方式, INGAME实体卡，COSTPOINT点卡 FA018上海webatm, FA029中华电信HiNet连扣, FA200000002测试用',
  tradeSeq varchar(60) DEFAULT NULL COMMENT '充值渠道订单',
  myCardTradeNo varchar(60) DEFAULT NULL ,
  facTradeSeq varchar(60) DEFAULT NULL ,
  customerId VARCHAR (60) DEFAULT NULL,
  amount varchar(60) DEFAULT '0' COMMENT '交易金额，可以为整数，若有小鼠最多2位',
  currency varchar(10) DEFAULT 'TWD' COMMENT '货币单位',
  tradeDateTime datetime DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_mycard_tradeno (mycardTradeNo) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into t_app (appId, appName, status, createTime, appKey, appSecret, sandbox, version, notifyUrl, payType) values (22, 'Test', 2, now(), 'test123456', 'test123456', 0, '1.0', '', 4);
insert into t_app_mycard values (22, now(), 'seastar', '1', '1adc3f0bdc96b0d3212ccc16053fdf2f', 'true');
insert into t_mycard_product values (22, now(), 'INGAME', 'item1', '测试商品1', '150', 'TWD');

insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc001', '50点卡', '50', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc002', '150点卡', '150', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc003', '300点卡', '300', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc004', '450点卡', '450', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc005', '1000点卡', '1000', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc006', '2000点卡', '2000', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc007', '3000点卡', '3000', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc008', '月卡', '150', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc009', '高级月卡', '300', 'TWD');
insert into t_mycard_product values (24, now(), 'INGAME', 'jjhmc010', '终身月卡', '450', 'TWD');


insert into t_operative values(23, 'ss.xyjhtw.app.001', '45', '0', '30');
insert into t_operative values(23, 'ss.xyjhtw.app.002', '225', '25', '150');
insert into t_operative values(23, 'ss.xyjhtw.app.003', '450', '50', '300');
insert into t_operative values(23, 'ss.xyjhtw.app.004', '675', '100', '450');
insert into t_operative values(23, 'ss.xyjhtw.app.005', '1500', '250', '1000');
insert into t_operative values(23, 'ss.xyjhtw.app.006', '3000', '500', '2000');
insert into t_operative values(23, 'ss.xyjhtw.app.007', '4500', '900', '3000');
insert into t_operative values(23, 'ss.xyjhtw.app.008', '225', '0', '150');
insert into t_operative values(23, 'ss.xyjhtw.app.009', '450', '0', '300');
insert into t_operative values(23, 'ss.xyjhtw.app.010', '675', '0', '450');

insert into t_operative values(23, 'ss.xyjhtw.gp.001', '75', '0', '50');
insert into t_operative values(23, 'ss.xyjhtw.gp.002', '225', '25', '150');
insert into t_operative values(23, 'ss.xyjhtw.gp.003', '450', '50', '300');
insert into t_operative values(23, 'ss.xyjhtw.gp.004', '675', '100', '450');
insert into t_operative values(23, 'ss.xyjhtw.gp.005', '1500', '250', '1000');
insert into t_operative values(23, 'ss.xyjhtw.gp.006', '3000', '500', '2000');
insert into t_operative values(23, 'ss.xyjhtw.gp.007', '4500', '900', '3000');
insert into t_operative values(23, 'ss.xyjhtw.gp.008', '225', '0', '150');
insert into t_operative values(23, 'ss.xyjhtw.gp.009', '450', '0', '300');
insert into t_operative values(23, 'ss.xyjhtw.gp.010', '675', '0', '450');


insert into t_operative values(24, 'jjhmc001', '75', '0', '50');
insert into t_operative values(24, 'jjhmc002', '225', '25', '150');
insert into t_operative values(24, 'jjhmc003', '450', '50', '300');
insert into t_operative values(24, 'jjhmc004', '675', '100', '450');
insert into t_operative values(24, 'jjhmc005', '1500', '250', '1000');
insert into t_operative values(24, 'jjhmc006', '3000', '500', '2000');
insert into t_operative values(24, 'jjhmc007', '4500', '900', '3000');
insert into t_operative values(24, 'jjhmc008', '225', '0', '150');
insert into t_operative values(24, 'jjhmc009', '450', '0', '300');
insert into t_operative values(24, 'jjhmc020', '675', '0', '450');



insert into t_app (appId, appName, status, createTime, appKey, appSecret, sandbox, `version`, notifyUrl, payType) values (24, '侠隐江湖Mycard', 2, now(), '2bf4fd4f947e19bcd816b299bda2811c', '9f6d146450850ea70e9bf4', 1, '1.0', 'http://xyjh.testpay.vrseastar.com:8000/PayService', 2);
insert into t_app_mycard(facServiceId, tradeType, hashKey, sandBoxMode) values ('seastar', '1', '1adc3f0bdc96b0d3212ccc16053fdf2f', 'true');