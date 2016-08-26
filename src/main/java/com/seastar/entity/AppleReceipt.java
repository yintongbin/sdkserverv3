package com.seastar.entity;

/**
 * Created by wjl on 2016/5/20.
 */
/*

{'receipt': {'bid': 'com.****.shop',
             'bvrs': '1.0.1',
             'item_id': '514487074',
             'original_purchase_date': '2012-05-03 03:46:52 Etc/GMT',
             'original_purchase_date_ms': '1336016812072',
             'original_purchase_date_pst': '2012-05-02 20:46:52 America/Los_Angeles',
             'original_transaction_id': '1000000046751500',
             'product_id': 'store_1',
             'purchase_date': '2012-05-03 03:46:52 Etc/GMT',
             'purchase_date_ms': '1336016812072',
             'purchase_date_pst': '2012-05-02 20:46:52 America/Los_Angeles',
             'quantity': '1',
             'transaction_id': '1000000046751500'},
 'status': 0}

{"status":0, "environment":"Sandbox",

"receipt":{"receipt_type":"ProductionSandbox", "adam_id":0, "app_item_id":0, "bundle_id":"com.seastar.carrotdefense", "application_version":"1", "download_id":0, "version_external_identifier":0, "receipt_creation_date":"2016-05-23 03:05:43 Etc/GMT", "receipt_creation_date_ms":"1463972743000", "receipt_creation_date_pst":"2016-05-22 20:05:43 America/Los_Angeles", "request_date":"2016-05-23 03:05:45 Etc/GMT", "request_date_ms":"1463972745538", "request_date_pst":"2016-05-22 20:05:45 America/Los_Angeles", "original_purchase_date":"2013-08-01 07:00:00 Etc/GMT", "original_purchase_date_ms":"1375340400000", "original_purchase_date_pst":"2013-08-01 00:00:00 America/Los_Angeles", "original_application_version":"1.0", "in_app":[{"quantity":"1", "product_id":"en_IAP209", "transaction_id":"1000000212522479", "original_transaction_id":"1000000212522479", "purchase_date":"2016-05-20 06:49:17 Etc/GMT", "purchase_date_ms":"1463726957000", "purchase_date_pst":"2016-05-19 23:49:17 America/Los_Angeles", "original_purchase_date":"2016-05-20 06:49:17 Etc/GMT", "original_purchase_date_ms":"1463726957000", "original_purchase_date_pst":"2016-05-19 23:49:17 America/Los_Angeles", "is_trial_period":"false"}]}


}

 */
public class AppleReceipt {
    public static class Receipt {
        public String bid;
        public String bvrs;
        public String item_id;
        public String original_purchase_date;
        public String original_purchase_date_ms;
        public String original_purchase_date_pst;
        public String original_transaction_id;
        public String product_id;
        public String purchase_date;
        public String purchase_date_ms;
        public String purchase_date_pst;
        public String quantity;
        public String transaction_id;
    }

    public String receipt;
    public int status;
    public String environment;
}
